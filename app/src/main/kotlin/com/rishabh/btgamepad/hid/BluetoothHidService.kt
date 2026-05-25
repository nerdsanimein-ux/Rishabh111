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

    /**
     * Optimistic fallback: some OEM BT stacks (Motorola, Xiaomi, Samsung) successfully
     * register the HID app but silently drop the onAppStatusChanged(true) callback.
     * If 5 seconds pass after registerApp() returned true and we're still in REGISTERING,
     * assume registration succeeded and advance to WAITING_FOR_HOST.
     */
    private val optimisticRunnable: Runnable = Runnable {
        if (currentState == State.REGISTERING) {
            mainHandler.removeCallbacks(registrationTimeoutRunnable)
            retryStage = 0
            currentState = State.WAITING_FOR_HOST
            requestDiscoverable()
        }
    }

    /**
     * Multi-stage retry: fires when registration times out completely.
     * Stage 0 → unregister + 500 ms + re-register.
     * Stage 1 → full proxy teardown + rebuild.
     * Stage 2 → ERROR.
     */
    private val registrationTimeoutRunnable: Runnable = Runnable {
        if (currentState != State.REGISTERING) return@Runnable
        mainHandler.removeCallbacks(optimisticRunnable)
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
                        if (ok) {
                            requestDiscoverable()
                            mainHandler.postDelayed(optimisticRunnable, 5_000)
                            mainHandler.postDelayed(registrationTimeoutRunnable, 20_000)
                        } else {
                            mainHandler.postDelayed(registrationTimeoutRunnable, 500)
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
        mainHandler.removeCallbacks(optimisticRunnable)
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
                    mainHandler.removeCallbacks(optimisticRunnable)
                    currentState = State.BLUETOOTH_OFF
                }
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            // Executor posts to main thread, so no mainHandler.post() needed
            if (registered) {
                mainHandler.removeCallbacks(registrationTimeoutRunnable)
                mainHandler.removeCallbacks(optimisticRunnable)
                retryStage = 0
                currentState = State.WAITING_FOR_HOST
                requestDiscoverable()
            } else if (currentState != State.REGISTERING) {
                // Ignore false fired by our own unregisterApp() pre-call during REGISTERING.
                // Only act if we lose registration from a connected/waiting state.
                mainHandler.removeCallbacks(registrationTimeoutRunnable)
                mainHandler.removeCallbacks(optimisticRunnable)
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
        // Silent scan mode (no dialog) — makes phone connectable immediately
        mainHandler.postDelayed({ forceScanMode() }, 500)
        mainHandler.postDelayed({ registerHidProfile() }, 1_500)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        mainHandler.removeCallbacks(registrationTimeoutRunnable)
        mainHandler.removeCallbacks(optimisticRunnable)
        unregisterReceiver(btStateReceiver)
        try { hidDevice?.unregisterApp() } catch (_: Exception) {}
        profileListener = null
        getSystemService(BluetoothManager::class.java)
            .adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        super.onDestroy()
    }

    // Forces adapter to SCAN_MODE_CONNECTABLE_DISCOVERABLE (23) via hidden API.
    // No user dialog needed. Silently no-ops if the ROM blocks hidden API access.
    @SuppressLint("MissingPermission")
    private fun forceScanMode() {
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
        mainHandler.removeCallbacks(optimisticRunnable)
        // 30 s outer guard — covers slow manufacturer BT daemons
        mainHandler.postDelayed(registrationTimeoutRunnable, 30_000)

        profileListener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                mainHandler.removeCallbacks(registrationTimeoutRunnable)
                mainHandler.postDelayed(registrationTimeoutRunnable, 20_000)
                hidDevice = proxy as BluetoothHidDevice

                val hid = hidDevice ?: run { currentState = State.ERROR; return }
                val ok = hid.registerApp(
                    GamepadHidDescriptor.buildSdpSettings(deviceName()),
                    null,   // null QoS = BT stack default, maximum compatibility
                    null,
                    Executor { cmd -> mainHandler.post(cmd) },
                    hidCallback
                )
                if (ok) {
                    // Start making phone visible NOW — don't wait for the callback.
                    // This lets the laptop see the phone in Bluetooth scans immediately.
                    forceScanMode()
                    requestDiscoverable()
                    // Optimistic fallback: if onAppStatusChanged(true) never fires
                    // (OEM BT stack bug), advance to WAITING_FOR_HOST after 5 s.
                    mainHandler.postDelayed(optimisticRunnable, 5_000)
                } else {
                    // registerApp() rejected outright — retry immediately
                    mainHandler.removeCallbacks(registrationTimeoutRunnable)
                    mainHandler.postDelayed(registrationTimeoutRunnable, 500)
                }
            }
            override fun onServiceDisconnected(profile: Int) {
                mainHandler.removeCallbacks(optimisticRunnable)
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
    private fun requestDiscoverable() {
        try {
            startActivity(
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    fun sendReport(report: ByteArray) {
        hidDevice?.sendReport(connectedHost ?: return, HidConstants.REPORT_ID, report)
    }

    fun getReportBuilder(): HidReportBuilder = reportBuilder

    fun retry() {
        if (currentState == State.NOT_SUPPORTED) return
        retryStage = 0
        mainHandler.removeCallbacks(registrationTimeoutRunnable)
        mainHandler.removeCallbacks(optimisticRunnable)
        try { hidDevice?.unregisterApp() } catch (_: Exception) {}
        profileListener = null
        getSystemService(BluetoothManager::class.java)
            .adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        hidDevice = null
        mainHandler.postDelayed({ forceScanMode() }, 300)
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
