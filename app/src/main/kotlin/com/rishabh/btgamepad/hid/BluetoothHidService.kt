package com.rishabh.btgamepad.hid

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.rishabh.btgamepad.MainActivity
import com.rishabh.btgamepad.R
import java.util.concurrent.Executors

class BluetoothHidService : Service() {

    enum class State { IDLE, BLUETOOTH_OFF, REGISTERING, WAITING_FOR_HOST, CONNECTED, ERROR }

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothHidService = this@BluetoothHidService
    }

    private val binder       = LocalBinder()
    private var hidDevice: BluetoothHidDevice? = null
    private var connectedHost: BluetoothDevice? = null
    private val reportBuilder = HidReportBuilder()
    private val mainHandler  = Handler(Looper.getMainLooper())
    private val executor     = Executors.newSingleThreadExecutor()
    private var retryCount   = 0

    // Held as a field so it is never GC'd between getProfileProxy() and the callback
    private var profileListener: BluetoothProfile.ServiceListener? = null

    var onStateChanged: ((State) -> Unit)? = null
    var currentState: State = State.IDLE
        private set(value) {
            field = value
            mainHandler.post { onStateChanged?.invoke(value) }
        }

    // If onServiceConnected never fires within 15s, retry once then give up
    private val registrationTimeoutRunnable = Runnable {
        if (currentState == State.REGISTERING) {
            if (retryCount < 2) {
                retryCount++
                // Close the stale proxy and try again
                hidDevice?.unregisterApp()
                val btm = getSystemService(BluetoothManager::class.java)
                btm.adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
                hidDevice = null
                profileListener = null
                mainHandler.postDelayed({ registerHidProfile() }, 1500)
            } else {
                retryCount = 0
                currentState = State.ERROR
            }
        }
    }

    private val btStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_ON -> {
                    if (currentState == State.BLUETOOTH_OFF || currentState == State.ERROR) {
                        retryCount = 0
                        mainHandler.postDelayed({ registerHidProfile() }, 2000)
                    }
                }
                BluetoothAdapter.STATE_OFF -> {
                    mainHandler.removeCallbacks(registrationTimeoutRunnable)
                    currentState = State.BLUETOOTH_OFF
                }
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        @SuppressLint("MissingPermission")
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            mainHandler.removeCallbacks(registrationTimeoutRunnable)
            if (registered) {
                currentState = State.WAITING_FOR_HOST
                requestDiscoverable()
            } else {
                currentState = State.IDLE
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedHost = device; currentState = State.CONNECTED
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedHost = null; currentState = State.WAITING_FOR_HOST
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            if (id == HidConstants.REPORT_ID)
                hidDevice?.replyReport(device, type, id, reportBuilder.toReport())
            else
                hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID)
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            hidDevice?.replyReport(device, type, id, byteArrayOf())
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification("Starting…"))
        registerReceiver(btStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        // Delay first attempt — BT profile services may not be ready immediately on startup
        mainHandler.postDelayed({ registerHidProfile() }, 3000)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        mainHandler.removeCallbacks(registrationTimeoutRunnable)
        unregisterReceiver(btStateReceiver)
        hidDevice?.unregisterApp()
        profileListener = null
        getSystemService(BluetoothManager::class.java)
            .adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    private fun registerHidProfile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
        ) {
            currentState = State.ERROR; return
        }

        val adapter = getSystemService(BluetoothManager::class.java).adapter ?: run {
            currentState = State.ERROR; return
        }
        if (!adapter.isEnabled) { currentState = State.BLUETOOTH_OFF; return }

        currentState = State.REGISTERING
        // Schedule timeout — 15s gives the BT stack time to respond
        mainHandler.removeCallbacks(registrationTimeoutRunnable)
        mainHandler.postDelayed(registrationTimeoutRunnable, 15_000)

        // Store as field — prevents the GC from collecting the listener
        // before the BT stack calls back on slow devices
        profileListener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                mainHandler.removeCallbacks(registrationTimeoutRunnable)
                hidDevice = proxy as BluetoothHidDevice
                // Delay before registerApp — unregister any stale state first,
                // then wait for BT stack to settle (some devices are slow)
                mainHandler.postDelayed({ registerApp() }, 800)
            }
            override fun onServiceDisconnected(profile: Int) {
                hidDevice = null
                if (currentState != State.BLUETOOTH_OFF) currentState = State.IDLE
            }
        }

        val ok = adapter.getProfileProxy(this, profileListener!!, BluetoothProfile.HID_DEVICE)
        if (!ok) {
            mainHandler.removeCallbacks(registrationTimeoutRunnable)
            profileListener = null
            currentState = State.ERROR
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerApp() {
        val hid = hidDevice ?: run { currentState = State.ERROR; return }
        // Unregister any leftover registration from a previous crash/session before re-registering.
        // This clears stale HID app state that prevents new registration on some devices.
        try { hid.unregisterApp() } catch (_: Exception) {}
        mainHandler.postDelayed({
            val hid2 = hidDevice ?: run { currentState = State.ERROR; return@postDelayed }
            val qos = BluetoothHidDeviceAppQosSettings(
                BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                800, 9, 0, 11250, BluetoothHidDeviceAppQosSettings.MAX
            )
            hid2.registerApp(GamepadHidDescriptor.buildSdpSettings(), null, qos, executor, hidCallback)
        }, 400)
    }

    private fun requestDiscoverable() {
        startActivity(
            Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun sendReport(report: ByteArray) {
        hidDevice?.sendReport(connectedHost ?: return, HidConstants.REPORT_ID.toInt(), report)
    }

    fun getReportBuilder(): HidReportBuilder = reportBuilder

    fun retry() {
        retryCount = 0
        mainHandler.removeCallbacks(registrationTimeoutRunnable)
        try { hidDevice?.unregisterApp() } catch (_: Exception) {}
        profileListener = null
        val btm = getSystemService(BluetoothManager::class.java)
        btm.adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        hidDevice = null
        // Longer wait so the BT stack fully releases the previous registration
        mainHandler.postDelayed({ registerHidProfile() }, 2500)
    }

    private fun buildNotification(text: String): Notification {
        val ch = "bt_gamepad"
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(ch, "BT Gamepad", NotificationManager.IMPORTANCE_LOW))
        return NotificationCompat.Builder(this, ch)
            .setContentTitle("BT Gamepad")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_gamepad)
            .setContentIntent(
                PendingIntent.getActivity(this, 0,
                    Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            )
            .setOngoing(true)
            .build()
    }

    companion object { private const val NOTIFICATION_ID = 1001 }
}
