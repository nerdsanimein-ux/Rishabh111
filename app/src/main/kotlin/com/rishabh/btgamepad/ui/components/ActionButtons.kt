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

private val BTN_COLOR = Color(0xFF3A3A3A)
private val LBL_COLOR = Color(0xFF999999)

@Composable
fun ActionButtons(
    layout: LayoutMode,
    onA: (Boolean) -> Unit,
    onB: (Boolean) -> Unit,
    onX: (Boolean) -> Unit,
    onY: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val btnSize = 68.dp

    val topLabel   = if (layout == LayoutMode.XBOX) "Y" else "△"
    val leftLabel  = if (layout == LayoutMode.XBOX) "X" else "□"
    val rightLabel = if (layout == LayoutMode.XBOX) "B" else "○"
    val botLabel   = if (layout == LayoutMode.XBOX) "A" else "×"

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        FaceButton(topLabel, btnSize, onY)
        Row(verticalAlignment = Alignment.CenterVertically) {
            FaceButton(leftLabel, btnSize, onX)
            Spacer(Modifier.size(btnSize))
            FaceButton(rightLabel, btnSize, onB)
        }
        FaceButton(botLabel, btnSize, onA)
    }
}

@Composable
private fun FaceButton(
    label: String,
    size: androidx.compose.ui.unit.Dp,
    onPressedChanged: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .background(BTN_COLOR, CircleShape)
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
        Text(label, color = LBL_COLOR, fontSize = 18.sp)
    }
}
