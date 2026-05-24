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

private val DPAD_SZ   = 150.dp
private val BTN_SZ    = 68.dp
private val SH_SZ     = 72.dp
private val SH_GAP    = 6.dp
private val STICK_D   = 130.dp
private val FACE_CROSS = BTN_SZ * 3
private val ACCENT    = Color(0xFF3D5AFE)
private val BG        = Color(0xFF111111)

@Composable
fun GamepadScreen(viewModel: GamepadViewModel) {
    val state        by viewModel.connectionState.observeAsState(BluetoothHidService.State.IDLE)
    val layout       by viewModel.layoutMode.observeAsState(LayoutMode.XBOX)
    val showSettings by viewModel.showSettings.observeAsState(false)
    val permDenied   by viewModel.permissionDenied.observeAsState(false)
    val context = LocalContext.current

    Box(Modifier.fillMaxSize().background(BG)) {

        // ── Top status bar ──────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GearButton { viewModel.toggleSettings() }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                LayoutDot(selected = layout == LayoutMode.PLAYSTATION) { viewModel.setLayoutMode(LayoutMode.PLAYSTATION) }
                LayoutDot(selected = layout == LayoutMode.XBOX)        { viewModel.setLayoutMode(LayoutMode.XBOX) }
                Box(Modifier.size(8.dp).background(statusColor(state), CircleShape))
            }
        }

        // ── Controller layout ───────────────────────────────────────────────
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(top = 28.dp)
        ) {
            val W = maxWidth
            val H = maxHeight

            if (layout == LayoutMode.XBOX) {
                // Shoulders stacked at top-left / top-right corners
                // DPad middle-left, face buttons middle-right
                // Both sticks side-by-side center-bottom, system buttons between them

                val shY   = H * 0.03f
                val lShX  = W * 0.02f
                val rShX  = W - SH_SZ - W * 0.02f

                val dpadX = W * 0.08f
                val dpadY = H * 0.28f

                val faceX = W - FACE_CROSS - W * 0.06f
                val faceY = H * 0.18f

                val stY   = H * 0.46f
                val lStX  = W * 0.26f
                val rStX  = W * 0.57f

                val sysX  = W * 0.5f - 80.dp
                val sysY  = H * 0.58f

                ShoulderButtons(ShoulderSide.LEFT, layout,
                    onL1 = viewModel::onL1Button, onL2 = viewModel::onL2Button,
                    modifier = Modifier.absoluteOffset(lShX, shY))
                ShoulderButtons(ShoulderSide.RIGHT, layout,
                    onR1 = viewModel::onR1Button, onR2 = viewModel::onR2Button,
                    modifier = Modifier.absoluteOffset(rShX, shY))

                DPad(viewModel::onDpadChange,
                    modifier = Modifier.absoluteOffset(dpadX, dpadY))

                ActionButtons(layout,
                    onA = viewModel::onAButton, onB = viewModel::onBButton,
                    onX = viewModel::onXButton, onY = viewModel::onYButton,
                    modifier = Modifier.absoluteOffset(faceX, faceY))

                AnalogStick("L", onMove = viewModel::onLeftStickMove,
                    modifier = Modifier.absoluteOffset(lStX, stY))
                AnalogStick("R", onMove = viewModel::onRightStickMove,
                    modifier = Modifier.absoluteOffset(rStX, stY))

                CenterButtons(viewModel::onSelectButton, viewModel::onStartButton,
                    modifier = Modifier.absoluteOffset(sysX, sysY))

            } else {
                // DPad top-left, left shoulders stacked right of DPad
                // Face buttons top-right, right shoulders stacked left of face
                // Sticks at bottom outer edges, system buttons top-center

                val topY  = H * 0.08f
                val dpadX = W * 0.06f

                val lShX  = dpadX + DPAD_SZ + 12.dp
                val faceX = W - FACE_CROSS - W * 0.06f
                val rShX  = faceX - SH_SZ - 12.dp

                val stY   = H * 0.46f
                val lStX  = W * 0.02f
                val rStX  = W - STICK_D - W * 0.02f

                val sysX  = W * 0.5f - 80.dp
                val sysY  = H * 0.08f

                DPad(viewModel::onDpadChange,
                    modifier = Modifier.absoluteOffset(dpadX, topY))

                ShoulderButtons(ShoulderSide.LEFT, layout,
                    onL1 = viewModel::onL1Button, onL2 = viewModel::onL2Button,
                    modifier = Modifier.absoluteOffset(lShX, topY))
                ShoulderButtons(ShoulderSide.RIGHT, layout,
                    onR1 = viewModel::onR1Button, onR2 = viewModel::onR2Button,
                    modifier = Modifier.absoluteOffset(rShX, topY))

                ActionButtons(layout,
                    onA = viewModel::onAButton, onB = viewModel::onBButton,
                    onX = viewModel::onXButton, onY = viewModel::onYButton,
                    modifier = Modifier.absoluteOffset(faceX, topY))

                AnalogStick("L", onMove = viewModel::onLeftStickMove,
                    modifier = Modifier.absoluteOffset(lStX, stY))
                AnalogStick("R", onMove = viewModel::onRightStickMove,
                    modifier = Modifier.absoluteOffset(rStX, stY))

                CenterButtons(viewModel::onSelectButton, viewModel::onStartButton,
                    modifier = Modifier.absoluteOffset(sysX, sysY))
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
        Modifier.fillMaxSize().background(Color(0xE0000000)),
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
            .size(24.dp)
            .background(Color(0xFF2A2A2A), CircleShape)
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
        Text("⚙", color = Color(0xFF777777), fontSize = 12.sp)
    }
}

@Composable
private fun LayoutDot(selected: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(
                if (selected) Color(0xFF888888) else Color(0xFF3A3A3A),
                CircleShape
            )
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
    )
}

@Composable
private fun SettingsOverlay(
    layout: LayoutMode,
    onLayoutChange: (LayoutMode) -> Unit,
    onClose: () -> Unit
) {
    Box(
        Modifier.fillMaxSize().background(Color(0xE8000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                .padding(20.dp)
                .width(300.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Settings", color = Color.White, fontSize = 18.sp)
                CloseButton(onClose)
            }
            Text("Controller Layout", color = Color(0xFF666666), fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToggleChip("Xbox",        layout == LayoutMode.XBOX)        { onLayoutChange(LayoutMode.XBOX) }
                ToggleChip("PlayStation", layout == LayoutMode.PLAYSTATION) { onLayoutChange(LayoutMode.PLAYSTATION) }
            }
            Text(
                "Xbox: sticks center-bottom\nPlayStation: sticks at outer edges",
                color = Color(0xFF555555), fontSize = 10.sp
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
            .background(Color(0xFF333333), CircleShape)
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
        Text("✕", color = Color(0xFF888888), fontSize = 13.sp)
    }
}

@Composable
private fun ToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) ACCENT else Color(0xFF2D2D2D)
        ),
        shape = RoundedCornerShape(8.dp)
    ) { Text(label, fontSize = 12.sp) }
}

private fun Modifier.absoluteOffset(x: androidx.compose.ui.unit.Dp, y: androidx.compose.ui.unit.Dp): Modifier =
    this.offset(x = x, y = y)

private fun statusColor(state: BluetoothHidService.State) = when (state) {
    BluetoothHidService.State.CONNECTED        -> Color(0xFF00E676)
    BluetoothHidService.State.WAITING_FOR_HOST -> Color(0xFFFFD600)
    BluetoothHidService.State.BLUETOOTH_OFF,
    BluetoothHidService.State.ERROR            -> Color(0xFFFF5252)
    BluetoothHidService.State.REGISTERING      -> Color(0xFF40C4FF)
    else                                       -> Color(0xFF444444)
}
