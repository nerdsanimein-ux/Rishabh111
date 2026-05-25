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
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishabh.btgamepad.hid.BluetoothHidService
import com.rishabh.btgamepad.ui.components.ActionButtons
import com.rishabh.btgamepad.ui.components.AnalogStick
import com.rishabh.btgamepad.ui.components.CenterButtons
import com.rishabh.btgamepad.ui.components.DPad
import com.rishabh.btgamepad.ui.components.ShoulderButtons
import com.rishabh.btgamepad.ui.components.ShoulderSide

private val ACCENT = Color(0xFF3D5AFE)
private val BG     = Color(0xFF111111)
private const val STATUS_H_DP = 28

@Composable
fun GamepadScreen(viewModel: GamepadViewModel) {
    val state        by viewModel.connectionState.observeAsState(BluetoothHidService.State.IDLE)
    val layout       by viewModel.layoutMode.observeAsState(LayoutMode.XBOX)
    val showSettings by viewModel.showSettings.observeAsState(false)
    val permDenied   by viewModel.permissionDenied.observeAsState(false)
    val context      = LocalContext.current

    Box(Modifier.fillMaxSize().background(BG)) {
      Box(Modifier.fillMaxSize().safeDrawingPadding()) {

        // ── Status bar ───────────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .height(STATUS_H_DP.dp)
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

        // ── Controller area ──────────────────────────────────────────────────
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(top = STATUS_H_DP.dp)
        ) {
            val W = maxWidth
            val H = maxHeight

            // ── Adaptive scale ──────────────────────────────────────────────
            // Base design at H=280dp, W=560dp. Scale down for smaller screens.
            val scaleH = (H.value / 280f).coerceIn(0.55f, 1.15f)
            val scaleW = (W.value / 560f).coerceIn(0.55f, 1.15f)
            val scale  = minOf(scaleH, scaleW)

            val shSz    = (68f  * scale).dp   // shoulder button diameter
            val dpadSz  = (140f * scale).dp   // DPad cross total size
            val btnSz   = (62f  * scale).dp   // face button diameter
            val stickD  = (120f * scale).dp   // analog stick base diameter
            val shRowW  = shSz * 2f + 6.dp    // 6.dp matches ShoulderButtons' internal gap
            val faceCrossH = btnSz * 3f        // face cross total height (3 buttons tall)
            val margin  = W * 0.05f

            if (layout == LayoutMode.XBOX) {
                // ── Xbox layout ─────────────────────────────────────────────
                // Shoulders: ROW (side-by-side) at top corners.
                // DPad:      left-center (no vertical overlap with shoulders — different x zone).
                // Face:      right-center, BELOW the shoulder row → faceY = shY + shSz + 8dp.
                // Sticks:    center-bottom, side by side.
                // System:    between sticks.

                val shY   = H * 0.02f
                val lShX  = margin
                val rShX  = W - shRowW - margin

                // DPad is in the left half, vertically centered — doesn't conflict with
                // left shoulder row since they're at the same x but DPad is positioned lower.
                val dpadX = margin + shSz * 0.15f
                val dpadBaseY = shY + shSz + 4.dp
                val dpadY = dpadBaseY + ((H - dpadBaseY - dpadSz - stickD) * 0.3f).coerceAtLeast(0.dp)

                // Face buttons start just below the right shoulder row.
                val faceY = shY + shSz + 8.dp
                val faceX = W - btnSz * 2.25f - margin   // right-aligned, slightly inset

                // Sticks: bottom-center. Ensure they fit within H.
                val stY  = (H - stickD - H * 0.04f).coerceAtLeast(shY + shSz + 8.dp)
                val lStX = W * 0.26f
                val rStX = W * 0.56f

                val sysX = W * 0.5f - 60.dp
                val sysY = stY + stickD * 0.3f

                ShoulderButtons(ShoulderSide.LEFT, layout, shSz,
                    onL1 = viewModel::onL1Button, onL2 = viewModel::onL2Button,
                    modifier = Modifier.absoluteOffset(lShX, shY))
                ShoulderButtons(ShoulderSide.RIGHT, layout, shSz,
                    onR1 = viewModel::onR1Button, onR2 = viewModel::onR2Button,
                    modifier = Modifier.absoluteOffset(rShX, shY))

                DPad(viewModel::onDpadChange, dpadSz,
                    modifier = Modifier.absoluteOffset(dpadX, dpadY))

                ActionButtons(layout, btnSz,
                    onA = viewModel::onAButton, onB = viewModel::onBButton,
                    onX = viewModel::onXButton, onY = viewModel::onYButton,
                    modifier = Modifier.absoluteOffset(faceX, faceY))

                AnalogStick("L", stickD, viewModel::onLeftStickMove,
                    modifier = Modifier.absoluteOffset(lStX, stY))
                AnalogStick("R", stickD, viewModel::onRightStickMove,
                    modifier = Modifier.absoluteOffset(rStX, stY))

                CenterButtons(viewModel::onSelectButton, viewModel::onStartButton,
                    modifier = Modifier.absoluteOffset(sysX, sysY))

            } else {
                // ── PlayStation layout ───────────────────────────────────────
                // DPad:      top-left.
                // L-Shoulder COLUMN: just right of DPad.
                // Face:      top-right.
                // R-Shoulder COLUMN: just left of face cross.
                // L-stick:   bottom-left.
                // R-stick:   bottom-right (clamped so it doesn't go off-screen).
                // System:    top-center.

                val topY  = H * 0.06f
                val dpadX = margin

                val lShX  = dpadX + dpadSz + 10.dp
                val faceW = btnSz * 2.25f          // width of face cross (2 btns + small gap)
                val faceX = W - faceW - margin
                val rShX  = faceX - shSz - 10.dp

                val stY   = (H - stickD - H * 0.04f).coerceAtLeast(topY + dpadSz + 8.dp)
                val lStX  = margin
                val rStX  = (W - stickD - margin).coerceAtLeast(0.dp)

                val sysX  = W * 0.5f - 60.dp
                val sysY  = topY

                DPad(viewModel::onDpadChange, dpadSz,
                    modifier = Modifier.absoluteOffset(dpadX, topY))

                ShoulderButtons(ShoulderSide.LEFT, layout, shSz,
                    onL1 = viewModel::onL1Button, onL2 = viewModel::onL2Button,
                    modifier = Modifier.absoluteOffset(lShX, topY))
                ShoulderButtons(ShoulderSide.RIGHT, layout, shSz,
                    onR1 = viewModel::onR1Button, onR2 = viewModel::onR2Button,
                    modifier = Modifier.absoluteOffset(rShX, topY))

                ActionButtons(layout, btnSz,
                    onA = viewModel::onAButton, onB = viewModel::onBButton,
                    onX = viewModel::onXButton, onY = viewModel::onYButton,
                    modifier = Modifier.absoluteOffset(faceX, topY))

                AnalogStick("L", stickD, viewModel::onLeftStickMove,
                    modifier = Modifier.absoluteOffset(lStX, stY))
                AnalogStick("R", stickD, viewModel::onRightStickMove,
                    modifier = Modifier.absoluteOffset(rStX, stY))

                CenterButtons(viewModel::onSelectButton, viewModel::onStartButton,
                    modifier = Modifier.absoluteOffset(sysX, sysY))
            }
        }

        // ── Overlays ─────────────────────────────────────────────────────────
        if (state == BluetoothHidService.State.WAITING_FOR_HOST) {
            StateOverlay {
                Text("Ready! Waiting for PC to connect", color = Color(0xFFFFD600), fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "On your laptop / PC:\n" +
                    "Settings → Bluetooth → Add Device\n" +
                    "→ Look for  \"${viewModel.controllerName}\"",
                    color = Color(0xFF90A4AE), fontSize = 13.sp
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { viewModel.connectToBonded() },
                    colors = ButtonDefaults.buttonColors(containerColor = ACCENT)
                ) { Text("Connect to PC") }
                Spacer(Modifier.height(4.dp))
                Text("Already paired but shows 'Not connected'? Tap this.",
                    color = Color(0xFF607D8B), fontSize = 10.sp)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.makeDiscoverable() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                ) { Text("Make Discoverable Again", fontSize = 12.sp) }
            }
        }

        if (state == BluetoothHidService.State.REGISTERING) {
            StateOverlay {
                Text("Setting up Bluetooth controller…", color = Color(0xFF40C4FF), fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "If a dialog appears, tap Allow.\nUsually takes 3–10 seconds.",
                    color = Color(0xFF90A4AE), fontSize = 12.sp
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { viewModel.forceWaiting() },
                    colors = ButtonDefaults.buttonColors(containerColor = ACCENT)
                ) { Text("Force Connect") }
                Spacer(Modifier.height(4.dp))
                Text("Use this if the status stays blue for over 10 s",
                    color = Color(0xFF607D8B), fontSize = 10.sp)
            }
        }

        if (state == BluetoothHidService.State.BLUETOOTH_OFF) {
            StateOverlay {
                Text("Bluetooth is OFF", color = Color(0xFFFF5252), fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { viewModel.openBluetoothSettings(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = ACCENT)
                ) { Text("Open Bluetooth Settings") }
                Spacer(Modifier.height(6.dp))
                Text("Reconnects automatically when Bluetooth turns ON",
                    color = Color(0xFF607D8B), fontSize = 11.sp)
            }
        }

        if (permDenied) {
            StateOverlay {
                Text("Bluetooth Permission Required", color = Color(0xFFFF5252), fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { viewModel.openBluetoothSettings(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = ACCENT)
                ) { Text("Open App Settings → Permissions") }
            }
        }

        if (state == BluetoothHidService.State.ERROR && !permDenied) {
            StateOverlay {
                Text("HID Registration Failed", color = Color(0xFFFF5252), fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Fix: Turn Bluetooth OFF → ON, then press Retry.\n\n" +
                    "If it keeps failing: force-stop this app in Settings, then reopen it.",
                    color = Color(0xFF90A4AE), fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { viewModel.retryConnection() },
                    colors = ButtonDefaults.buttonColors(containerColor = ACCENT)
                ) { Text("Retry") }
                Spacer(Modifier.height(6.dp))
                Button(onClick = { viewModel.openBluetoothSettings(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                ) { Text("Open Bluetooth Settings", fontSize = 11.sp) }
            }
        }

        if (state == BluetoothHidService.State.NOT_SUPPORTED) {
            StateOverlay {
                Text("Device Not Supported", color = Color(0xFFFF5252), fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Your phone's Bluetooth hardware does not support\n" +
                    "HID Device (peripheral) mode.\n\n" +
                    "This is a hardware limitation — not all Android phones\n" +
                    "support acting as a Bluetooth controller.",
                    color = Color(0xFF90A4AE), fontSize = 12.sp
                )
            }
        }

        if (showSettings) {
            SettingsOverlay(layout, viewModel::setLayoutMode, viewModel::toggleSettings)
        }
      } // end safeDrawingPadding box
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

@Composable
private fun StateOverlay(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xE0000000)), contentAlignment = Alignment.Center) {
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
    ) { Text("⚙", color = Color(0xFF777777), fontSize = 12.sp) }
}

@Composable
private fun LayoutDot(selected: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(if (selected) Color(0xFF888888) else Color(0xFF3A3A3A), CircleShape)
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
private fun SettingsOverlay(layout: LayoutMode, onLayoutChange: (LayoutMode) -> Unit, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xE8000000)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                .padding(20.dp).width(300.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Settings", color = Color.White, fontSize = 18.sp)
                CloseButton(onClose)
            }
            Text("Controller Layout", color = Color(0xFF666666), fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToggleChip("Xbox",        layout == LayoutMode.XBOX)        { onLayoutChange(LayoutMode.XBOX) }
                ToggleChip("PlayStation", layout == LayoutMode.PLAYSTATION) { onLayoutChange(LayoutMode.PLAYSTATION) }
            }
            Text("Xbox: sticks center-bottom\nPlayStation: sticks at outer edges",
                color = Color(0xFF555555), fontSize = 10.sp)
        }
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(30.dp).background(Color(0xFF333333), CircleShape)
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
    ) { Text("✕", color = Color(0xFF888888), fontSize = 13.sp) }
}

@Composable
private fun ToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = if (selected) ACCENT else Color(0xFF2D2D2D)),
        shape = RoundedCornerShape(8.dp)
    ) { Text(label, fontSize = 12.sp) }
}

private fun Modifier.absoluteOffset(x: Dp, y: Dp): Modifier = this.offset(x = x, y = y)

private fun statusColor(state: BluetoothHidService.State) = when (state) {
    BluetoothHidService.State.CONNECTED        -> Color(0xFF00E676)
    BluetoothHidService.State.WAITING_FOR_HOST -> Color(0xFFFFD600)
    BluetoothHidService.State.BLUETOOTH_OFF,
    BluetoothHidService.State.ERROR,
    BluetoothHidService.State.NOT_SUPPORTED    -> Color(0xFFFF5252)
    BluetoothHidService.State.REGISTERING      -> Color(0xFF40C4FF)
    else                                       -> Color(0xFF444444)
}
