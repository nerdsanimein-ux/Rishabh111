package com.rishabh.btgamepad.permission

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

object BluetoothPermissionHandler {

    fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+: BLUETOOTH_CONNECT for HID profile API, BLUETOOTH_ADVERTISE for discoverability
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            // Android 9-11: BLUETOOTH / BLUETOOTH_ADMIN are normal permissions (auto-granted at install).
            // ACCESS_FINE_LOCATION is NOT needed for the HID device role (no scanning).
            emptyArray()
        }

    fun allGranted(activity: Activity): Boolean {
        val perms = requiredPermissions()
        return perms.isEmpty() || perms.all { perm ->
            ContextCompat.checkSelfPermission(activity, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun createLauncher(
        activity: ComponentActivity,
        onResult: (allGranted: Boolean) -> Unit
    ): ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            onResult(results.isEmpty() || results.values.all { it })
        }
}
