package com.rishabh.btgamepad

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.rishabh.btgamepad.hid.BluetoothHidService
import com.rishabh.btgamepad.permission.BluetoothPermissionHandler
import com.rishabh.btgamepad.ui.GamepadScreen
import com.rishabh.btgamepad.ui.GamepadViewModel
import com.rishabh.btgamepad.ui.theme.BtGamepadTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GamepadViewModel by viewModels()
    private var serviceBound = false

    private val permissionLauncher = BluetoothPermissionHandler.createLauncher(this) { granted ->
        if (granted) bindHidService()
        else viewModel.onPermissionDenied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableFullScreen()
        setContent { BtGamepadTheme { GamepadScreen(viewModel = viewModel) } }
    }

    override fun onStart() {
        super.onStart()
        when {
            !BluetoothPermissionHandler.allGranted(this) ->
                permissionLauncher.launch(BluetoothPermissionHandler.requiredPermissions())
            !serviceBound -> bindHidService()
        }
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            viewModel.unbindService(this)
            serviceBound = false
        }
    }

    private fun bindHidService() {
        startForegroundService(Intent(this, BluetoothHidService::class.java))
        viewModel.bindService(this)
        serviceBound = true
    }

    private fun enableFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
