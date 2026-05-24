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

    private val permissionLauncher = BluetoothPermissionHandler.createLauncher(this) { granted ->
        if (granted) startHidService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableFullScreen()

        if (BluetoothPermissionHandler.allGranted(this)) {
            startHidService()
        } else {
            permissionLauncher.launch(BluetoothPermissionHandler.requiredPermissions())
        }

        setContent {
            BtGamepadTheme {
                GamepadScreen(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.bindService(this)
    }

    override fun onStop() {
        super.onStop()
        viewModel.unbindService(this)
    }

    private fun startHidService() {
        startForegroundService(Intent(this, BluetoothHidService::class.java))
    }

    private fun enableFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
