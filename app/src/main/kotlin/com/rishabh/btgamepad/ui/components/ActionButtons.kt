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

// Xbox: A=green B=red X=blue Y=yellow
// PlayStation: ×=blue ○=red □=pink △=green
private val CLR_GREEN  = Color(0xFF00E676)
private val CLR_RED    = Color(0xFFFF1744)
private val CLR_BLUE   = Color(0xFF2979FF)
private val CLR_YELLOW = Color(0xFFFFD600)
private val CLR_PINK   = Color(0xFFFF4081)

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
    val btnSize = (50 * scale).dp

    val (topLabel, topColor)   = if (layout == LayoutMode.XBOX) "Y" to CLR_YELLOW else "△" to CLR_GREEN
    val (leftLabel, leftColor) = if (layout == LayoutMode.XBOX) "X" to CLR_BLUE   else "□" to CLR_PINK
    val (rightLabel, rightColor) = if (layout == LayoutMode.XBOX) "B" to CLR_RED  else "○" to CLR_RED
    val (botLabel, botColor)   = if (layout == LayoutMode.XBOX) "A" to CLR_GREEN  else "×" to CLR_BLUE

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
        Text(label, color = Color.White, fontSize = (15 * (size.value / 50f)).sp)
    }
}
