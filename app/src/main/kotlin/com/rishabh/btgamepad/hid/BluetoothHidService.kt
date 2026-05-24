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
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.rishabh.btgamepad.MainActivity
import com.rishabh.btgamepad.R
import java.util.concurrent.Executors

class BluetoothHidService : Service() {

    enum class State { IDLE, REGISTERING, WAITING_FOR_HOST, CONNECTED, ERROR }

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothHidService = this@BluetoothHidService
    }

    private val binder = LocalBinder()
    private var hidDevice: BluetoothHidDevice? = null
    private var connectedHost: BluetoothDevice? = null
    private val reportBuilder = HidReportBuilder()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    var onStateChanged: ((State) -> Unit)? = null
    var currentState: State = State.IDLE
        private set(value) {
            field = value
            mainHandler.post { onStateChanged?.invoke(value) }
        }

    private val hidCallback = object : BluetoothHidDevice.Callback() {

        @SuppressLint("MissingPermission")
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
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
                    connectedHost = device
                    currentState = State.CONNECTED
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedHost = null
                    currentState = State.WAITING_FOR_HOST
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            if (id == HidConstants.REPORT_ID) {
                hidDevice?.replyReport(device, type, id, reportBuilder.toReport())
            } else {
                hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID)
            }
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            hidDevice?.replyReport(device, type, id, byteArrayOf())
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification("Waiting for connection…"))
        registerHidProfile()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        hidDevice?.unregisterApp()
        val btManager = getSystemService(BluetoothManager::class.java)
        btManager.adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    private fun registerHidProfile() {
        currentState = State.REGISTERING
        val adapter: BluetoothAdapter =
            getSystemService(BluetoothManager::class.java).adapter ?: run {
                currentState = State.ERROR; return
            }
        adapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                hidDevice = proxy as BluetoothHidDevice
                registerApp()
            }
            override fun onServiceDisconnected(profile: Int) {
                hidDevice = null
                currentState = State.IDLE
            }
        }, BluetoothProfile.HID_DEVICE)
    }

    @SuppressLint("MissingPermission")
    private fun registerApp() {
        val qos = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,
            9,
            0,
            11250,
            BluetoothHidDeviceAppQosSettings.MAX
        )
        hidDevice?.registerApp(
            GamepadHidDescriptor.buildSdpSettings(),
            null,
            qos,
            executor,
            hidCallback
        )
    }

    private fun requestDiscoverable() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    @SuppressLint("MissingPermission")
    fun sendReport(report: ByteArray) {
        val host = connectedHost ?: return
        hidDevice?.sendReport(host, HidConstants.REPORT_ID.toInt(), report)
    }

    fun getReportBuilder(): HidReportBuilder = reportBuilder

    private fun buildNotification(text: String): Notification {
        val channelId = "bt_gamepad"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(channelId, "BT Gamepad", NotificationManager.IMPORTANCE_LOW)
        )
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("BT Gamepad")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_gamepad)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
