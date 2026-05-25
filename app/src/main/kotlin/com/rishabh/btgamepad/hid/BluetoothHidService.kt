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
import java.util.concurrent.Executor

class BluetoothHidService : Service() {

    enum class State { IDLE, BLUETOOTH_OFF, REGISTERING, WAITING_FOR_HOST, CONNECTED, ERROR, NOT_SUPPORTED }

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothHidService = this@BluetoothHidService
    }

    private val binder        = LocalBinder()
    private var hidDevice: BluetoothHidDevice? = null
    private var connectedHost: BluetoothDevice? = null
    private val reportBuilder  = HidReportBuilder()
    private val mainHandler    = Handler(Looper.getMainLooper())
    private var profileListener: BluetoothProfile.ServiceListener? = null
    private var retryStage     = 0   // 0=first try, 1=unregister+retry, 2=full reset, 3=error

    var onStateChanged: ((State) -> Unit)? = null
    var currentState: State = State.IDLE
        private set(value) {
            field = value
            mainHandler.post { onStateChanged?.invoke(value) }
        }

    // SCAN_MODE_CONNECTABLE_DISCOVERABLE = 23
    // Forces the BT adapter into discoverable mode without a user dialog.
    // Called via reflection because setScanMode() is a hidden API.
    @SuppressLint("MissingPermission")
    private fun forceDiscoverable() {
        val adapter = getSystemService(BluetoothManager::class.java).adapter ?: return
        try {
            adapter.javaClass
                .getMethod("setScanMode", Int::class.javaPrimitiveType)
                .invoke(adapter, 23)
        } catch (_: Exception) {
            try {
                adapter.javaClass
                    .getMethod("setScanMode", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                    .invoke(adapter, 23, 300)
            } catch (_: Exception) {}
        }
        // Also show the user-facing dialog as a fallback (some ROMs block the hidden API)
        try {
            startActivity(
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {}
    }

    /**
     * Multi-stage retry runnable.
     *
     * Stage 0 → registerApp() timed out or returned false. Unregister, wait 500 ms, re-register.
     * Stage 1 → still failed; full proxy teardown + rebuild from scratch.
     * Stage 2 → total failure. Show ERROR state.
     */
    private val registrationTimeoutRunnable: Runnable = Runnable {
        if (currentState != State.REGISTERING) return@Runnable
        when (retryStage) {
            0 -> {
                retryStage = 1
                val hid = hidDevice
                if (hid != null) {
                    try { hid.unregisterApp() } catch (_: Exception) {}
                    mainHandler.postDelayed({
                        val hid2 = hidDevice ?: run { fullProxyReset(); return@postDelayed }
                        val ok = hid2.registerApp(
                            GamepadHidDescriptor.buildSdpSettings(deviceName()),
                            null, null,
                            Executor { cmd -> mainHandler.post(cmd) },
                            hidCallback
                        )
                        if (!ok) {
                            mainHandler.removeCallbacks(registrationTimeoutRunnable)
                            mainHandler.postDelayed(registrationTimeoutRunnable, 500)
                        } else {
                            mainHandler.postDelayed(registrationTimeoutRunnable, 20_000)
                        }
                    }, 500)
                } else {
                    retryStage = 1
                    fullProxyReset()
                }
            }
            1 -> {
                retryStage = 2
                fullProxyReset()
            }
            else -> {
                retryStage = 0
                currentState = State.ERROR
            }
        }
    }

    private fun fullProxyReset() {
        try { hidDevice?.unregisterApp() } catch (_: Exception) {}
        profileListener = null
        getSystemService(BluetoothManager::class.java)
            .adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        hidDevice = null
        mainHandler.postDelayed({ registerHidProfile() }, 2_000)
    }

    private val btStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_ON -> {
                    if (currentState == State.BLUETOOTH_OFF || currentState == State.ERROR) {
                        retryStage = 0
                        mainHandler.postDelayed({ registerHidProfile() }, 2_000)
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
            // Executor is main-thread, so no mainHandler.post() needed here
            if (registered) {
                mainHandler.removeCallbacks(registrationTimeoutRunnable)
                retryStage = 0
                currentState = State.WAITING_FOR_HOST
                // Force discoverable now that registration succeeded
                forceDiscoverable()
            } else if (currentState != State.REGISTERING) {
                // Ignore the false from our own unregisterApp() pre-call during REGISTERING.
                // Only act if registration is lost from another state.
                mainHandler.removeCallbacks(registrationTimeoutRunnable)
                currentState = State.IDLE
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED    -> { connectedHost = device; currentState = State.CONNECTED }
                BluetoothProfile.STATE_DISCONNECTED -> { connectedHost = null;   currentState = State.WAITING_FOR_HOST }
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            if (id.toInt() == HidConstants.REPORT_ID)
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
        // Force discoverable immediately via hidden API — no user dialog needed.
        // Do NOT call forceDiscoverable() (which shows a dialog) here because firing
        // a system UI Activity before the BT service listener binds can interrupt
        // the profile proxy connection on some manufacturer skins (MIUI, OneUI, etc.).
        mainHandler.postDelayed({ forceScanModeOnly() }, 500)
        mainHandler.postDelayed({ registerHidProfile() }, 1_500)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        mainHandler.removeCallbacks(registrationTimeoutRunnable)
        unregisterReceiver(btStateReceiver)
        try { hidDevice?.unregisterApp() } catch (_: Exception) {}
        profileListener = null
        getSystemService(BluetoothManager::class.java)
            .adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        super.onDestroy()
    }

    // Sets scan mode to CONNECTABLE_DISCOVERABLE without showing any Activity/dialog.
    @SuppressLint("MissingPermission")
    private fun forceScanModeOnly() {
        val adapter = getSystemService(BluetoothManager::class.java).adapter ?: return
        try {
            adapter.javaClass
                .getMethod("setScanMode", Int::class.javaPrimitiveType)
                .invoke(adapter, 23)
        } catch (_: Exception) {
            try {
                adapter.javaClass
                    .getMethod("setScanMode", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                    .invoke(adapter, 23, 300)
            } catch (_: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerHidProfile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
        ) { currentState = State.ERROR; return }

        val adapter = getSystemService(BluetoothManager::class.java).adapter
            ?: run { currentState = State.ERROR; return }
        if (!adapter.isEnabled) { currentState = State.BLUETOOTH_OFF; return }

        currentState = State.REGISTERING
        mainHandler.removeCallbacks(registrationTimeoutRunnable)
        mainHandler.postDelayed(registrationTimeoutRunnable, 30_000)  // 30 s for slow manufacturer stacks

        profileListener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                // Reset timeout: give registerApp() a full fresh window from proxy connect
                mainHandler.removeCallbacks(registrationTimeoutRunnable)
                mainHandler.postDelayed(registrationTimeoutRunnable, 20_000)
                hidDevice = proxy as BluetoothHidDevice

                val hid = hidDevice ?: run { currentState = State.ERROR; return }
                val ok = hid.registerApp(
                    GamepadHidDescriptor.buildSdpSettings(deviceName()),
                    null,   // null QoS = stack default, maximum compatibility
                    null,
                    Executor { cmd -> mainHandler.post(cmd) },
                    hidCallback
                )
                // If registerApp() returned false the BT stack rejected the call outright.
                // Trigger the retry runnable immediately instead of waiting 20 s.
                if (!ok) {
                    mainHandler.removeCallbacks(registrationTimeoutRunnable)
                    mainHandler.postDelayed(registrationTimeoutRunnable, 500)
                }
            }
            override fun onServiceDisconnected(profile: Int) {
                try { hidDevice?.unregisterApp() } catch (_: Exception) {}
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
    private fun deviceName(): String = try {
        getSystemService(BluetoothManager::class.java)
            .adapter?.name?.takeIf { it.isNotBlank() } ?: "Android"
    } catch (_: Exception) { "Android" }.let { "$it Controller" }

    @SuppressLint("MissingPermission")
    fun sendReport(report: ByteArray) {
        hidDevice?.sendReport(connectedHost ?: return, HidConstants.REPORT_ID, report)
    }

    fun getReportBuilder(): HidReportBuilder = reportBuilder

    fun retry() {
        if (currentState == State.NOT_SUPPORTED) return
        retryStage = 0
        mainHandler.removeCallbacks(registrationTimeoutRunnable)
        try { hidDevice?.unregisterApp() } catch (_: Exception) {}
        profileListener = null
        getSystemService(BluetoothManager::class.java)
            .adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        hidDevice = null
        mainHandler.postDelayed({ forceScanModeOnly() }, 300)
        mainHandler.postDelayed({ registerHidProfile() }, 2_500)
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
