package com.rishabh.btgamepad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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

enum class ShoulderSide { LEFT, RIGHT }

@Composable
fun ShoulderButtons(
    side: ShoulderSide,
    scale: Float = 1f,
    onL1: ((Boolean) -> Unit)? = null,
    onL2: ((Boolean) -> Unit)? = null,
    onR1: ((Boolean) -> Unit)? = null,
    onR2: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (side == ShoulderSide.LEFT) {
            ShoulderKey("L1", scale, onL1)
            Spacer(Modifier.height(4.dp))
            ShoulderKey("L2", scale, onL2)
        } else {
            ShoulderKey("R1", scale, onR1)
            Spacer(Modifier.height(4.dp))
            ShoulderKey("R2", scale, onR2)
        }
    }
}

@Composable
private fun ShoulderKey(label: String, scale: Float, onPressed: ((Boolean) -> Unit)?) {
    val haptic = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = (72 * scale).dp, height = (36 * scale).dp)
            .background(Color(0xFF616161), RoundedCornerShape(6.dp))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.any { it.pressed }
                        if (pressed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPressed?.invoke(pressed)
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        Text(label, color = Color.White)
    }
}
