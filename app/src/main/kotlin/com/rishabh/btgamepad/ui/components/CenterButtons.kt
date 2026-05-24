package com.rishabh.btgamepad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CenterButtons(
    onSelect: (Boolean) -> Unit,
    onStart: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton("SELECT", onSelect)
            PillButton("START", onStart)
        }
        HomeIndicator()
    }
}

@Composable
private fun PillButton(label: String, onPressed: (Boolean) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(Color(0xFF2D3748), RoundedCornerShape(12.dp))
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
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(label, color = Color(0xFFB0BEC5), fontSize = 10.sp)
    }
}

@Composable
private fun HomeIndicator() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(22.dp)
            .background(Color(0xFF3D4A5C), CircleShape)
    ) {
        Text("⊙", color = Color(0xFF78909C), fontSize = 12.sp)
    }
}
