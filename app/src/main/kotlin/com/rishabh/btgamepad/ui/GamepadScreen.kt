package com.rishabh.btgamepad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishabh.btgamepad.hid.BluetoothHidService
import com.rishabh.btgamepad.ui.components.ActionButtons
import com.rishabh.btgamepad.ui.components.AnalogStick
import com.rishabh.btgamepad.ui.components.DPad
import com.rishabh.btgamepad.ui.components.ShoulderButtons
import com.rishabh.btgamepad.ui.components.ShoulderSide

@Composable
fun GamepadScreen(viewModel: GamepadViewModel) {
    val state by viewModel.connectionState.observeAsState(BluetoothHidService.State.IDLE)
    val layout by viewModel.layoutMode.observeAsState(LayoutMode.XBOX)
    val scale by viewModel.controlScale.observeAsState(1.0f)
    val showSettings by viewModel.showSettings.observeAsState(false)
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
    ) {
        // ---- Status bar ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(state)
            Text(
                text = statusText(state),
                color = statusColor(state),
                fontSize = 12.sp
            )
            // Settings gear button
            SystemButton("⚙", { pressed -> if (!pressed) viewModel.toggleSettings() })
        }

        // ---- Bluetooth OFF banner ----
        if (state == BluetoothHidService.State.BLUETOOTH_OFF) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Bluetooth is OFF", color = Color(0xFFF44336), fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.openBluetoothSettings(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) { Text("Open Bluetooth Settings") }
                Spacer(Modifier.height(8.dp))
                Text("App will reconnect automatically when Bluetooth turns ON",
                    color = Color(0xFF9E9E9E), fontSize = 11.sp)
            }
        }

        // ---- Error banner ----
        if (state == BluetoothHidService.State.ERROR) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Bluetooth HID Error", color = Color(0xFFF44336), fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { viewModel.retryConnection() }) { Text("Retry") }
            }
        }

        // ---- Main gamepad UI ----
        if (state != BluetoothHidService.State.BLUETOOTH_OFF &&
            state != BluetoothHidService.State.ERROR) {

            // Shoulder buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 28.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShoulderButtons(
                    side = ShoulderSide.LEFT, scale = scale,
                    onL1 = viewModel::onL1Button, onL2 = viewModel::onL2Button
                )
                ShoulderButtons(
                    side = ShoulderSide.RIGHT, scale = scale,
                    onR1 = viewModel::onR1Button, onR2 = viewModel::onR2Button
                )
            }

            // Center row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DPad(onDirectionChange = viewModel::onDpadChange, scale = scale)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SystemButton("SELECT", viewModel::onSelectButton)
                    SystemButton("START", viewModel::onStartButton)
                }

                ActionButtons(
                    layout = layout, scale = scale,
                    onA = viewModel::onAButton, onB = viewModel::onBButton,
                    onX = viewModel::onXButton, onY = viewModel::onYButton
                )
            }

            // Analog sticks
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AnalogStick(label = "L", scale = scale, onMove = viewModel::onLeftStickMove)
                AnalogStick(label = "R", scale = scale, onMove = viewModel::onRightStickMove)
            }
        }

        // ---- Settings overlay ----
        if (showSettings) {
            SettingsOverlay(
                layout = layout,
                scale = scale,
                onLayoutChange = viewModel::setLayoutMode,
                onScaleChange = viewModel::setScale,
                onClose = viewModel::toggleSettings
            )
        }
    }
}

@Composable
private fun StatusDot(state: BluetoothHidService.State) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(statusColor(state), RoundedCornerShape(50))
    )
}

private fun statusText(state: BluetoothHidService.State) = when (state) {
    BluetoothHidService.State.IDLE            -> "Idle"
    BluetoothHidService.State.BLUETOOTH_OFF   -> "Bluetooth OFF"
    BluetoothHidService.State.REGISTERING     -> "Registering HID…"
    BluetoothHidService.State.WAITING_FOR_HOST -> "Waiting for PC…"
    BluetoothHidService.State.CONNECTED       -> "Connected ✓"
    BluetoothHidService.State.ERROR           -> "Error"
}

private fun statusColor(state: BluetoothHidService.State) = when (state) {
    BluetoothHidService.State.CONNECTED       -> Color(0xFF4CAF50)
    BluetoothHidService.State.WAITING_FOR_HOST -> Color(0xFFFFEB3B)
    BluetoothHidService.State.BLUETOOTH_OFF,
    BluetoothHidService.State.ERROR           -> Color(0xFFF44336)
    else                                      -> Color(0xFF9E9E9E)
}

@Composable
fun SystemButton(label: String, onPressed: (Boolean) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(Color(0xFF424242), RoundedCornerShape(6.dp))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.any { it.pressed }
                        if (pressed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPressed(pressed)
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
private fun SettingsOverlay(
    layout: LayoutMode,
    scale: Float,
    onLayoutChange: (LayoutMode) -> Unit,
    onScaleChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
                .padding(24.dp)
                .width(320.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Settings", color = Color.White, fontSize = 18.sp)

            // Layout toggle
            Text("Controller Layout", color = Color(0xFF9E9E9E), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LayoutToggleButton("Xbox", layout == LayoutMode.XBOX) {
                    onLayoutChange(LayoutMode.XBOX)
                }
                LayoutToggleButton("PlayStation", layout == LayoutMode.PLAYSTATION) {
                    onLayoutChange(LayoutMode.PLAYSTATION)
                }
            }

            // Size slider
            Text("Control Size: ${(scale * 100).toInt()}%",
                color = Color(0xFF9E9E9E), fontSize = 12.sp)
            Slider(
                value = scale,
                onValueChange = onScaleChange,
                valueRange = 0.6f..1.6f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF4CAF50),
                    activeTrackColor = Color(0xFF4CAF50))
            )

            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242))
            ) { Text("Close") }
        }
    }
}

@Composable
private fun LayoutToggleButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF4CAF50) else Color(0xFF424242)
        )
    ) { Text(label, fontSize = 12.sp) }
}
