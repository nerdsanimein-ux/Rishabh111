package com.rishabh.btgamepad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(8.dp)
    ) {
        // Connection status
        Text(
            text = when (state) {
                BluetoothHidService.State.IDLE -> "Bluetooth: Initializing…"
                BluetoothHidService.State.REGISTERING -> "Registering HID profile…"
                BluetoothHidService.State.WAITING_FOR_HOST -> "Waiting for PC to connect…"
                BluetoothHidService.State.CONNECTED -> "Connected"
                BluetoothHidService.State.ERROR -> "Error — check Bluetooth"
            },
            color = when (state) {
                BluetoothHidService.State.CONNECTED -> Color(0xFF4CAF50)
                BluetoothHidService.State.ERROR -> Color(0xFFF44336)
                else -> Color(0xFFBDBDBD)
            },
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Shoulder buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ShoulderButtons(side = ShoulderSide.LEFT, onL1 = viewModel::onL1Button, onL2 = viewModel::onL2Button)
            ShoulderButtons(side = ShoulderSide.RIGHT, onR1 = viewModel::onR1Button, onR2 = viewModel::onR2Button)
        }

        // Center: D-pad | Start+Select | ABXY
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DPad(onDirectionChange = viewModel::onDpadChange)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SystemButton("SELECT", viewModel::onSelectButton)
                SystemButton("START", viewModel::onStartButton)
            }

            ActionButtons(
                onA = viewModel::onAButton,
                onB = viewModel::onBButton,
                onX = viewModel::onXButton,
                onY = viewModel::onYButton
            )
        }

        // Bottom: analog sticks
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AnalogStick(label = "L", onMove = viewModel::onLeftStickMove)
            AnalogStick(label = "R", onMove = viewModel::onRightStickMove)
        }
    }
}

@Composable
private fun SystemButton(label: String, onPressed: (Boolean) -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(Color(0xFF424242), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        onPressed(event.changes.any { it.pressed })
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}
