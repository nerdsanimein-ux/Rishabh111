package com.rishabh.btgamepad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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

private val BG_TOP    = Color(0xFF0D1117)
private val BG_BOTTOM = Color(0xFF0A0E1A)
private val ACCENT    = Color(0xFF3D5AFE)

@Composable
fun GamepadScreen(viewModel: GamepadViewModel) {
    val state         by viewModel.connectionState.observeAsState(BluetoothHidService.State.IDLE)
    val layout        by viewModel.layoutMode.observeAsState(LayoutMode.XBOX)
    val showSettings  by viewModel.showSettings.observeAsState(false)
    val permDenied    by viewModel.permissionDenied.observeAsState(false)
    val stickScale    by viewModel.stickScale.observeAsState(1f)
    val dpadScale     by viewModel.dpadScale.observeAsState(1f)
    val buttonScale   by viewModel.buttonScale.observeAsState(1f)
    val shoulderScale by viewModel.shoulderScale.observeAsState(1f)
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BG_TOP, BG_BOTTOM)))
    ) {
        Column(Modifier.fillMaxSize()) {

            // ── Status bar ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).background(statusColor(state), CircleShape))
                    Text(statusText(state), color = statusColor(state), fontSize = 11.sp)
                }
                GearButton { viewModel.toggleSettings() }
            }

            // ── Shoulder row ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShoulderButtons(
                    side = ShoulderSide.LEFT, scale = shoulderScale,
                    onL1 = viewModel::onL1Button, onL2 = viewModel::onL2Button
                )
                ShoulderButtons(
                    side = ShoulderSide.RIGHT, scale = shoulderScale,
                    onR1 = viewModel::onR1Button, onR2 = viewModel::onR2Button
                )
            }

            Spacer(Modifier.height(4.dp))

            // ── Main controller row ───────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left column: DPad (top) + Left Stick (bottom)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    DPad(
                        onDirectionChange = viewModel::onDpadChange,
                        scale = dpadScale
                    )
                    AnalogStick(
                        label = "L",
                        scale = stickScale,
                        onMove = viewModel::onLeftStickMove
                    )
                }

                // Center column: system buttons
                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CenterButtons(
                        onSelect = viewModel::onSelectButton,
                        onStart = viewModel::onStartButton
                    )
                }

                // Right column: Action buttons (top) + Right Stick (bottom)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    ActionButtons(
                        layout = layout, scale = buttonScale,
                        onA = viewModel::onAButton, onB = viewModel::onBButton,
                        onX = viewModel::onXButton, onY = viewModel::onYButton
                    )
                    AnalogStick(
                        label = "R",
                        scale = stickScale,
                        onMove = viewModel::onRightStickMove
                    )
                }
            }
        }

        // ── Bluetooth OFF overlay ─────────────────────────────────────────────
        if (state == BluetoothHidService.State.BLUETOOTH_OFF) {
            StatusOverlay {
                Text("Bluetooth is OFF", color = Color(0xFFFF5252), fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.openBluetoothSettings(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = ACCENT)
                ) { Text("Open Bluetooth Settings") }
                Spacer(Modifier.height(6.dp))
                Text("App reconnects automatically when Bluetooth turns ON",
                    color = Color(0xFF607D8B), fontSize = 11.sp)
            }
        }

        // ── Permission denied overlay ─────────────────────────────────────────
        if (permDenied) {
            StatusOverlay {
                Text("Bluetooth Permission Required", color = Color(0xFFFF5252), fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.openBluetoothSettings(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = ACCENT)
                ) { Text("Open Settings") }
            }
        }

        // ── Error overlay ─────────────────────────────────────────────────────
        if (state == BluetoothHidService.State.ERROR && !permDenied) {
            StatusOverlay {
                Text("HID Registration Failed", color = Color(0xFFFF5252), fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text("Make sure Bluetooth is ON and try again.",
                    color = Color(0xFF90A4AE), fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.retryConnection() },
                    colors = ButtonDefaults.buttonColors(containerColor = ACCENT)
                ) { Text("Retry") }
            }
        }

        // ── Settings overlay ──────────────────────────────────────────────────
        if (showSettings) {
            SettingsOverlay(
                layout = layout,
                stickScale = stickScale, dpadScale = dpadScale,
                buttonScale = buttonScale, shoulderScale = shoulderScale,
                onLayoutChange = viewModel::setLayoutMode,
                onStickScale = viewModel::setStickScale,
                onDpadScale = viewModel::setDpadScale,
                onButtonScale = viewModel::setButtonScale,
                onShoulderScale = viewModel::setShoulderScale,
                onClose = viewModel::toggleSettings
            )
        }
    }
}

// ── Shared status overlay ──────────────────────────────────────────────────────
@Composable
private fun StatusOverlay(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDD000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

// ── Gear / settings button ────────────────────────────────────────────────────
@Composable
private fun GearButton(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .background(Color(0xFF1E2A3A), CircleShape)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.any { it.pressed }
                        if (!pressed) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClick()
                        }
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        Text("⚙", color = Color(0xFF78909C), fontSize = 14.sp)
    }
}

// ── Settings overlay ───────────────────────────────────────────────────────────
@Composable
private fun SettingsOverlay(
    layout: LayoutMode,
    stickScale: Float, dpadScale: Float,
    buttonScale: Float, shoulderScale: Float,
    onLayoutChange: (LayoutMode) -> Unit,
    onStickScale: (Float) -> Unit,
    onDpadScale: (Float) -> Unit,
    onButtonScale: (Float) -> Unit,
    onShoulderScale: (Float) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE5000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(Color(0xFF161B27), RoundedCornerShape(16.dp))
                .padding(24.dp)
                .width(340.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Settings", color = Color.White, fontSize = 18.sp)

            // Controller layout toggle
            SectionLabel("Controller Layout")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToggleChip("Xbox",        layout == LayoutMode.XBOX)        { onLayoutChange(LayoutMode.XBOX) }
                ToggleChip("PlayStation", layout == LayoutMode.PLAYSTATION) { onLayoutChange(LayoutMode.PLAYSTATION) }
            }

            // Per-group size sliders
            SectionLabel("Analog Sticks — ${pct(stickScale)}")
            ScaleSlider(stickScale, onStickScale)

            SectionLabel("D-Pad — ${pct(dpadScale)}")
            ScaleSlider(dpadScale, onDpadScale)

            SectionLabel("Face Buttons — ${pct(buttonScale)}")
            ScaleSlider(buttonScale, onButtonScale)

            SectionLabel("Shoulder Buttons — ${pct(shoulderScale)}")
            ScaleSlider(shoulderScale, onShoulderScale)

            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ACCENT)
            ) { Text("Done") }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Color(0xFF78909C), fontSize = 11.sp)
}

@Composable
private fun ScaleSlider(value: Float, onChange: (Float) -> Unit) {
    Slider(
        value = value,
        onValueChange = onChange,
        valueRange = 0.5f..1.8f,
        colors = SliderDefaults.colors(
            thumbColor = ACCENT,
            activeTrackColor = ACCENT,
            inactiveTrackColor = Color(0xFF2D3748)
        )
    )
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

private fun pct(scale: Float) = "${(scale * 100).toInt()}%"

// ── Helpers ───────────────────────────────────────────────────────────────────
private fun statusText(state: BluetoothHidService.State) = when (state) {
    BluetoothHidService.State.IDLE             -> "Idle"
    BluetoothHidService.State.BLUETOOTH_OFF    -> "Bluetooth OFF"
    BluetoothHidService.State.REGISTERING      -> "Registering HID…"
    BluetoothHidService.State.WAITING_FOR_HOST -> "Waiting for PC…"
    BluetoothHidService.State.CONNECTED        -> "Connected ✓"
    BluetoothHidService.State.ERROR            -> "Error"
}

private fun statusColor(state: BluetoothHidService.State) = when (state) {
    BluetoothHidService.State.CONNECTED        -> Color(0xFF00E676)
    BluetoothHidService.State.WAITING_FOR_HOST -> Color(0xFFFFD600)
    BluetoothHidService.State.BLUETOOTH_OFF,
    BluetoothHidService.State.ERROR            -> Color(0xFFFF5252)
    BluetoothHidService.State.REGISTERING      -> Color(0xFF40C4FF)
    else                                       -> Color(0xFF607D8B)
}
