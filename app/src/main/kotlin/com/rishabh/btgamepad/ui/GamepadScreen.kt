package com.rishabh.btgamepad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import com.rishabh.btgamepad.ui.components.CenterButtons
import com.rishabh.btgamepad.ui.components.DPad
import com.rishabh.btgamepad.ui.components.ShoulderButtons
import com.rishabh.btgamepad.ui.components.ShoulderSide

// Fixed element sizes — tuned to match the reference controller layout
private val STICK_DP    = 124.dp   // large analog stick diameter
private val DPAD_KEY_DP = 48.dp    // each DPad key; cross = 144dp × 144dp
private val BTN_DP      = 52.dp    // face button; cross = 156dp × 156dp
private val SHLDR_DP    = 52.dp    // shoulder circle; row = (52+6+52)=110dp
private val STATUS_H    = 24.dp
private val ACCENT      = Color(0xFF3D5AFE)

@Composable
fun GamepadScreen(viewModel: GamepadViewModel) {
    val state        by viewModel.connectionState.observeAsState(BluetoothHidService.State.IDLE)
    val layout       by viewModel.layoutMode.observeAsState(LayoutMode.XBOX)
    val showSettings by viewModel.showSettings.observeAsState(false)
    val permDenied   by viewModel.permissionDenied.observeAsState(false)
    val context = LocalContext.current

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D1117), Color(0xFF0A0E1A))))
    ) {
        // ── Status bar ─────────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .height(STATUS_H)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(7.dp).background(statusColor(state), CircleShape))
                Text(statusText(state), color = statusColor(state), fontSize = 10.sp)
            }
            GearButton { viewModel.toggleSettings() }
        }

        // ── Controller layout ───────────────────────────────────────────────
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(top = STATUS_H)
        ) {
            val W = maxWidth
            val H = maxHeight

            val dpadSize  = DPAD_KEY_DP                 // cross = 3×dpadSize
            val stickSize = STICK_DP                    // diameter
            val btnSize   = BTN_DP                      // face button; cross = 3×btnSize
            val shRow     = SHLDR_DP * 2 + 6.dp        // total shoulder row width = 110dp

            if (layout == LayoutMode.XBOX) {
                // ── Xbox layout ────────────────────────────────────────────
                // Shoulders at top corners; DPad left-center; face buttons right-center;
                // BOTH sticks in the CENTER-BOTTOM zone (classic Xbox thumb position)

                val shY   = H * 0.04f
                val lShX  = W * 0.01f
                val rShX  = W - shRow - W * 0.01f

                // DPad and face buttons at same vertical band, outer edges
                val ctrlY = H * 0.22f
                val dpadX = W * 0.02f
                val faceX = W - (btnSize * 3) - W * 0.02f

                // System buttons centered, slightly below DPad/face midpoint
                val sysX  = W * 0.50f - 55.dp
                val sysY  = H * 0.44f

                // Both sticks side-by-side at bottom center
                val stY   = H * 0.48f
                val lStX  = W * 0.23f
                val rStX  = W * 0.62f

                ShoulderButtons(ShoulderSide.LEFT, layout,
                    onL1 = viewModel::onL1Button, onL2 = viewModel::onL2Button,
                    modifier = Modifier.absoluteOffset(lShX, shY))
                ShoulderButtons(ShoulderSide.RIGHT, layout,
                    onR1 = viewModel::onR1Button, onR2 = viewModel::onR2Button,
                    modifier = Modifier.absoluteOffset(rShX, shY))

                DPad(viewModel::onDpadChange, modifier = Modifier.absoluteOffset(dpadX, ctrlY))

                ActionButtons(layout, onA = viewModel::onAButton, onB = viewModel::onBButton,
                    onX = viewModel::onXButton, onY = viewModel::onYButton,
                    modifier = Modifier.absoluteOffset(faceX, ctrlY))

                CenterButtons(viewModel::onSelectButton, viewModel::onStartButton,
                    modifier = Modifier.absoluteOffset(sysX, sysY))

                AnalogStick("L", onMove = viewModel::onLeftStickMove,
                    modifier = Modifier.absoluteOffset(lStX, stY))
                AnalogStick("R", onMove = viewModel::onRightStickMove,
                    modifier = Modifier.absoluteOffset(rStX, stY))

            } else {
                // ── PlayStation layout ─────────────────────────────────────
                // DPad top-left; shoulder circles right of DPad / left of face buttons;
                // Face buttons top-right; BOTH sticks at the outer BOTTOM edges

                val topY  = H * 0.04f
                val dpadX = W * 0.02f
                // Left shoulders start right after the DPad cross
                val lShX  = dpadX + dpadSize * 3 + 10.dp
                val faceX = W - (btnSize * 3) - W * 0.02f
                // Right shoulders start right before the face button cross
                val rShX  = faceX - shRow - 10.dp

                val sysX  = W * 0.50f - 55.dp
                val sysY  = H * 0.46f

                // Sticks at outer bottom edges — clear of DPad/face bottom
                val stY   = H * 0.52f
                val lStX  = W * 0.01f
                val rStX  = W - stickSize - W * 0.01f

                DPad(viewModel::onDpadChange, modifier = Modifier.absoluteOffset(dpadX, topY))

                ShoulderButtons(ShoulderSide.LEFT, layout,
                    onL1 = viewModel::onL1Button, onL2 = viewModel::onL2Button,
                    modifier = Modifier.absoluteOffset(lShX, topY))
                ShoulderButtons(ShoulderSide.RIGHT, layout,
                    onR1 = viewModel::onR1Button, onR2 = viewModel::onR2Button,
                    modifier = Modifier.absoluteOffset(rShX, topY))

                ActionButtons(layout, onA = viewModel::onAButton, onB = viewModel::onBButton,
                    onX = viewModel::onXButton, onY = viewModel::onYButton,
                    modifier = Modifier.absoluteOffset(faceX, topY))

                CenterButtons(viewModel::onSelectButton, viewModel::onStartButton,
                    modifier = Modifier.absoluteOffset(sysX, sysY))

                AnalogStick("L", onMove = viewModel::onLeftStickMove,
                    modifier = Modifier.absoluteOffset(lStX, stY))
                AnalogStick("R", onMove = viewModel::onRightStickMove,
                    modifier = Modifier.absoluteOffset(rStX, stY))
            }
        }

        // ── Bluetooth OFF overlay ───────────────────────────────────────────
        if (state == BluetoothHidService.State.BLUETOOTH_OFF) {
            StateOverlay {
                Text("Bluetooth is OFF", color = Color(0xFFFF5252), fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.openBluetoothSettings(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = ACCENT)
                ) { Text("Open Bluetooth Settings") }
                Spacer(Modifier.height(6.dp))
                Text("Reconnects automatically when Bluetooth turns ON",
                    color = Color(0xFF607D8B), fontSize = 11.sp)
            }
        }

        // ── Permission denied overlay ───────────────────────────────────────
        if (permDenied) {
            StateOverlay {
                Text("Bluetooth Permission Required", color = Color(0xFFFF5252), fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.openBluetoothSettings(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = ACCENT)
                ) { Text("Open App Settings → Permissions") }
            }
        }

        // ── HID error overlay ───────────────────────────────────────────────
        if (state == BluetoothHidService.State.ERROR && !permDenied) {
            StateOverlay {
                Text("HID Registration Failed", color = Color(0xFFFF5252), fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text("Make sure Bluetooth is ON.\nSome devices need a restart of Bluetooth.",
                    color = Color(0xFF90A4AE), fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.retryConnection() },
                    colors = ButtonDefaults.buttonColors(containerColor = ACCENT)
                ) { Text("Retry") }
            }
        }

        // ── Settings overlay ────────────────────────────────────────────────
        if (showSettings) {
            SettingsOverlay(
                layout = layout,
                onLayoutChange = viewModel::setLayoutMode,
                onClose = viewModel::toggleSettings
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun StateOverlay(content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xE0000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

@Composable
private fun GearButton(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(22.dp)
            .background(Color(0x331E2A3A), CircleShape)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (!event.changes.any { it.pressed }) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClick()
                        }
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        Text("⚙", color = Color(0xFF78909C), fontSize = 12.sp)
    }
}

@Composable
private fun SettingsOverlay(
    layout: LayoutMode,
    onLayoutChange: (LayoutMode) -> Unit,
    onClose: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xE8000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .background(Color(0xFF161B27), RoundedCornerShape(16.dp))
                .padding(20.dp)
                .width(300.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header row with X close button always visible
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Settings", color = Color.White, fontSize = 18.sp)
                CloseButton(onClose)
            }

            // Layout toggle
            Text("Controller Layout", color = Color(0xFF78909C), fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToggleChip("Xbox",        layout == LayoutMode.XBOX)        { onLayoutChange(LayoutMode.XBOX) }
                ToggleChip("PlayStation", layout == LayoutMode.PLAYSTATION) { onLayoutChange(LayoutMode.PLAYSTATION) }
            }

            Text(
                "Switch between Xbox controller layout\n(sticks in center) and PlayStation layout\n(sticks at outer edges).",
                color = Color(0xFF546E7A), fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(30.dp)
            .background(Color(0xFF2D3748), CircleShape)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (!event.changes.any { it.pressed }) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClick()
                        }
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        Text("✕", color = Color(0xFFB0BEC5), fontSize = 13.sp)
    }
}

@Composable
private fun ToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) ACCENT else Color(0xFF2D3748)
        ),
        shape = RoundedCornerShape(8.dp)
    ) { Text(label, fontSize = 12.sp) }
}

// Dp-based offset (non-composable form of Modifier.offset)
private fun Modifier.absoluteOffset(x: androidx.compose.ui.unit.Dp, y: androidx.compose.ui.unit.Dp): Modifier =
    this.offset(x = x, y = y)

private fun statusText(state: BluetoothHidService.State) = when (state) {
    BluetoothHidService.State.IDLE             -> "Idle"
    BluetoothHidService.State.BLUETOOTH_OFF    -> "Bluetooth OFF"
    BluetoothHidService.State.REGISTERING      -> "Registering HID…"
    BluetoothHidService.State.WAITING_FOR_HOST -> "Waiting for PC…"
    BluetoothHidService.State.CONNECTED        -> "Connected ✓"
    BluetoothHidService.State.ERROR            -> "Error — tap ⚙ to retry"
}

private fun statusColor(state: BluetoothHidService.State) = when (state) {
    BluetoothHidService.State.CONNECTED        -> Color(0xFF00E676)
    BluetoothHidService.State.WAITING_FOR_HOST -> Color(0xFFFFD600)
    BluetoothHidService.State.BLUETOOTH_OFF,
    BluetoothHidService.State.ERROR            -> Color(0xFFFF5252)
    BluetoothHidService.State.REGISTERING      -> Color(0xFF40C4FF)
    else                                       -> Color(0xFF607D8B)
}
