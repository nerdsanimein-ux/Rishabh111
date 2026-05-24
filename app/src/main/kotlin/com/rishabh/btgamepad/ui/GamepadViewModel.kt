package com.rishabh.btgamepad.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.rishabh.btgamepad.hid.BluetoothHidService
import com.rishabh.btgamepad.hid.HidConstants
import com.rishabh.btgamepad.hid.HidReportBuilder
import kotlin.math.roundToInt

class GamepadViewModel(application: Application) : AndroidViewModel(application) {

    val connectionState = MutableLiveData(BluetoothHidService.State.IDLE)

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

    fun onLeftStickMove(x: Float, y: Float) {
        val rb = reportBuilder ?: return
        rb.leftX = normalizeAxis(x)
        rb.leftY = normalizeAxis(y)
        dispatchReport()
    }

    fun onRightStickMove(x: Float, y: Float) {
        val rb = reportBuilder ?: return
        rb.rightX = normalizeAxis(x)
        rb.rightY = normalizeAxis(y)
        dispatchReport()
    }

    fun onDpadChange(direction: Byte) {
        reportBuilder?.dpad = direction
        dispatchReport()
    }

    fun onAButton(pressed: Boolean) = setButton(HidConstants.BTN_A, 1, pressed)
    fun onBButton(pressed: Boolean) = setButton(HidConstants.BTN_B, 1, pressed)
    fun onXButton(pressed: Boolean) = setButton(HidConstants.BTN_X, 1, pressed)
    fun onYButton(pressed: Boolean) = setButton(HidConstants.BTN_Y, 1, pressed)
    fun onL1Button(pressed: Boolean) = setButton(HidConstants.BTN_L1, 2, pressed)
    fun onR1Button(pressed: Boolean) = setButton(HidConstants.BTN_R1, 2, pressed)
    fun onL2Button(pressed: Boolean) = setButton(HidConstants.BTN_L2, 2, pressed)
    fun onR2Button(pressed: Boolean) = setButton(HidConstants.BTN_R2, 2, pressed)
    fun onStartButton(pressed: Boolean) = setButton(HidConstants.BTN_START, 2, pressed)
    fun onSelectButton(pressed: Boolean) = setButton(HidConstants.BTN_SELECT, 2, pressed)

    private fun setButton(mask: Int, group: Int, pressed: Boolean) {
        reportBuilder?.setButton(mask, group, pressed)
        dispatchReport()
    }

    private fun dispatchReport() {
        val rb = reportBuilder ?: return
        hidService?.sendReport(rb.toReport())
    }

    /** Maps [-1.0, 1.0] float to [0, 255] unsigned byte. */
    private fun normalizeAxis(value: Float): Byte =
        ((value.coerceIn(-1f, 1f) * 127f) + 128f)
            .roundToInt().coerceIn(0, 255).toByte()
}
