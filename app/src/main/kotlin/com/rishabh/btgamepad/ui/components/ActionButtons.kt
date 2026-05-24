package com.rishabh.btgamepad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.rishabh.btgamepad.ui.LayoutMode

@Composable
fun ActionButtons(
    layout: LayoutMode,
    scale: Float,
    onA: (Boolean) -> Unit,
    onB: (Boolean) -> Unit,
    onX: (Boolean) -> Unit,
    onY: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val btnSize = (52 * scale).dp

    // Xbox:        A=green  B=red    X=blue   Y=yellow
    // PlayStation: ×=blue   ○=red    □=pink   △=green
    val (topLabel, topColor)    = if (layout == LayoutMode.XBOX) "Y" to Color(0xFFFFEB3B) else "△" to Color(0xFF4CAF50)
    val (leftLabel, leftColor)  = if (layout == LayoutMode.XBOX) "X" to Color(0xFF2196F3) else "□" to Color(0xFFE91E8C)
    val (rightLabel, rightColor)= if (layout == LayoutMode.XBOX) "B" to Color(0xFFF44336) else "○" to Color(0xFFF44336)
    val (botLabel, botColor)    = if (layout == LayoutMode.XBOX) "A" to Color(0xFF4CAF50) else "×" to Color(0xFF2196F3)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        FaceButton(topLabel, topColor, btnSize, onY)
        Row(verticalAlignment = Alignment.CenterVertically) {
            FaceButton(leftLabel, leftColor, btnSize, onX)
            Spacer(Modifier.size(btnSize))
            FaceButton(rightLabel, rightColor, btnSize, onB)
        }
        FaceButton(botLabel, botColor, btnSize, onA)
    }
}

@Composable
private fun FaceButton(
    label: String,
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    onPressedChanged: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .background(color, CircleShape)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.any { it.pressed }
                        if (pressed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPressedChanged(pressed)
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        Text(label, color = Color.White, fontSize = (16 * scale(size)).sp)
    }
}

// Helper to derive a relative font scale from button size dp value
private fun scale(size: androidx.compose.ui.unit.Dp): Float = (size.value / 52f).coerceIn(0.5f, 2f)
