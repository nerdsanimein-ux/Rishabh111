package com.rishabh.btgamepad.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.rishabh.btgamepad.hid.BluetoothHidService
import com.rishabh.btgamepad.hid.HidConstants
import com.rishabh.btgamepad.hid.HidReportBuilder
import kotlin.math.roundToInt

enum class LayoutMode { XBOX, PLAYSTATION }

class GamepadViewModel(application: Application) : AndroidViewModel(application) {

    val connectionState = MutableLiveData(BluetoothHidService.State.IDLE)
    val layoutMode = MutableLiveData(LayoutMode.XBOX)
    val controlScale = MutableLiveData(1.0f)
    val showSettings = MutableLiveData(false)

    private var hidService: BluetoothHidService? = null
    private var reportBuilder: HidReportBuilder? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val svc = (binder as BluetoothHidService.LocalBinder).getService()
            hidService = svc
            reportBuilder = svc.getReportBuilder()
            svc.onStateChanged = { state -> connectionState.postValue(state) }
            connectionState.postValue(svc.currentState)
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            hidService = null
            reportBuilder = null
        }
    }

    fun bindService(context: Context) {
        val intent = Intent(context, BluetoothHidService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService(context: Context) {
        context.unbindService(serviceConnection)
    }

    fun retryConnection() {
        hidService?.retry()
    }

    fun openBluetoothSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun toggleSettings() {
        showSettings.value = !(showSettings.value ?: false)
    }

    fun setLayoutMode(mode: LayoutMode) { layoutMode.value = mode }
    fun setScale(scale: Float) { controlScale.value = scale.coerceIn(0.6f, 1.6f) }

    // ---- Input events ----

    fun onLeftStickMove(x: Float, y: Float) {
        val rb = reportBuilder ?: return
        rb.leftX = normalizeAxis(x); rb.leftY = normalizeAxis(y)
        dispatchReport()
    }

    fun onRightStickMove(x: Float, y: Float) {
        val rb = reportBuilder ?: return
        rb.rightX = normalizeAxis(x); rb.rightY = normalizeAxis(y)
        dispatchReport()
    }

    fun onDpadChange(direction: Byte) { reportBuilder?.dpad = direction; dispatchReport() }

    fun onAButton(pressed: Boolean)      = setButton(HidConstants.BTN_A,      1, pressed)
    fun onBButton(pressed: Boolean)      = setButton(HidConstants.BTN_B,      1, pressed)
    fun onXButton(pressed: Boolean)      = setButton(HidConstants.BTN_X,      1, pressed)
    fun onYButton(pressed: Boolean)      = setButton(HidConstants.BTN_Y,      1, pressed)
    fun onL1Button(pressed: Boolean)     = setButton(HidConstants.BTN_L1,     2, pressed)
    fun onR1Button(pressed: Boolean)     = setButton(HidConstants.BTN_R1,     2, pressed)
    fun onL2Button(pressed: Boolean)     = setButton(HidConstants.BTN_L2,     2, pressed)
    fun onR2Button(pressed: Boolean)     = setButton(HidConstants.BTN_R2,     2, pressed)
    fun onStartButton(pressed: Boolean)  = setButton(HidConstants.BTN_START,  2, pressed)
    fun onSelectButton(pressed: Boolean) = setButton(HidConstants.BTN_SELECT, 2, pressed)

    private fun setButton(mask: Int, group: Int, pressed: Boolean) {
        reportBuilder?.setButton(mask, group, pressed)
        dispatchReport()
    }

    private fun dispatchReport() {
        hidService?.sendReport(reportBuilder?.toReport() ?: return)
    }

    private fun normalizeAxis(value: Float): Byte =
        ((value.coerceIn(-1f, 1f) * 127f) + 128f).roundToInt().coerceIn(0, 255).toByte()
}
