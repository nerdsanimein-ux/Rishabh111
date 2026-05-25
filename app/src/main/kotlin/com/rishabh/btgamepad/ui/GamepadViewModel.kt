package com.rishabh.btgamepad.ui

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothManager
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

    val connectionState  = MutableLiveData(BluetoothHidService.State.IDLE)
    val layoutMode       = MutableLiveData(LayoutMode.XBOX)
    val showSettings     = MutableLiveData(false)
    val permissionDenied = MutableLiveData(false)

    @get:SuppressLint("MissingPermission")
    val controllerName: String get() = try {
        val name = getApplication<Application>()
            .getSystemService(BluetoothManager::class.java)?.adapter?.name?.takeIf { it.isNotBlank() } ?: "Android"
        "$name Controller"
    } catch (_: Exception) { "Android Controller" }

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
            hidService = null; reportBuilder = null
        }
    }

    fun bindService(context: Context) =
        context.bindService(Intent(context, BluetoothHidService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)

    fun unbindService(context: Context) = context.unbindService(serviceConnection)

    fun onPermissionDenied() { permissionDenied.value = true }

    fun retryConnection()    { hidService?.retry() }
    fun forceWaiting()       { hidService?.forceWaiting() }
    fun makeDiscoverable()   { hidService?.makeDiscoverable() }
    fun connectToBonded()    { hidService?.connectToBonded() }
    fun toggleSettings()   { showSettings.value = !(showSettings.value ?: false) }
    fun setLayoutMode(m: LayoutMode) { layoutMode.value = m }

    fun openBluetoothSettings(context: Context) =
        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    // ── Input events ─────────────────────────────────────────────────────────

    fun onLeftStickMove(x: Float, y: Float) {
        val rb = reportBuilder ?: return
        rb.leftX = normalizeAxis(x); rb.leftY = normalizeAxis(y); dispatchReport()
    }
    fun onRightStickMove(x: Float, y: Float) {
        val rb = reportBuilder ?: return
        rb.rightX = normalizeAxis(x); rb.rightY = normalizeAxis(y); dispatchReport()
    }
    fun onDpadChange(direction: Byte) { reportBuilder?.dpad = direction; dispatchReport() }

    fun onAButton(p: Boolean)      = setButton(HidConstants.BTN_A,      p)
    fun onBButton(p: Boolean)      = setButton(HidConstants.BTN_B,      p)
    fun onXButton(p: Boolean)      = setButton(HidConstants.BTN_X,      p)
    fun onYButton(p: Boolean)      = setButton(HidConstants.BTN_Y,      p)
    fun onL1Button(p: Boolean)     = setButton(HidConstants.BTN_L1,     p)
    fun onR1Button(p: Boolean)     = setButton(HidConstants.BTN_R1,     p)
    fun onL2Button(p: Boolean)     = setButton(HidConstants.BTN_L2,     p)
    fun onR2Button(p: Boolean)     = setButton(HidConstants.BTN_R2,     p)
    fun onStartButton(p: Boolean)  = setButton(HidConstants.BTN_START,  p)
    fun onSelectButton(p: Boolean) = setButton(HidConstants.BTN_SELECT, p)

    private fun setButton(mask: Int, pressed: Boolean) {
        reportBuilder?.setButton(mask, pressed); dispatchReport()
    }
    private fun dispatchReport() { hidService?.sendReport(reportBuilder?.toReport() ?: return) }

    // Unsigned axis: -1.0f → 0, 0.0f → 128, 1.0f → 255
    private fun normalizeAxis(v: Float): Byte =
        (v.coerceIn(-1f, 1f) * 127f + 128f).roundToInt().coerceIn(0, 255).toByte()
}
