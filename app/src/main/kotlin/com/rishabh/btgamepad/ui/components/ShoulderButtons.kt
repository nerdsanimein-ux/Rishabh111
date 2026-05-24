package com.rishabh.btgamepad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

enum class ShoulderSide { LEFT, RIGHT }

private val SH_COLOR = Color(0xFF3A3A3A)
private val SH_LABEL = Color(0xFF999999)

// Both sides stack as a Column: bumper (LB/RB) on top, trigger (LT/RT) below

@Composable
fun ShoulderButtons(
    side: ShoulderSide,
    layout: LayoutMode = LayoutMode.XBOX,
    onL1: ((Boolean) -> Unit)? = null,
    onL2: ((Boolean) -> Unit)? = null,
    onR1: ((Boolean) -> Unit)? = null,
    onR2: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val size = 72.dp

    if (side == ShoulderSide.LEFT) {
        val bmpLabel  = if (layout == LayoutMode.XBOX) "LB" else "L1"
        val trigLabel = if (layout == LayoutMode.XBOX) "LT" else "L2"
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShoulderCircle(bmpLabel,  size, onL1)
            ShoulderCircle(trigLabel, size, onL2)
        }
    } else {
        val bmpLabel  = if (layout == LayoutMode.XBOX) "RB" else "R1"
        val trigLabel = if (layout == LayoutMode.XBOX) "RT" else "R2"
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShoulderCircle(bmpLabel,  size, onR1)
            ShoulderCircle(trigLabel, size, onR2)
        }
    }
}

@Composable
private fun ShoulderCircle(
    label: String,
    size: androidx.compose.ui.unit.Dp,
    onPressed: ((Boolean) -> Unit)?
) {
    val haptic = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .background(SH_COLOR, CircleShape)
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
        Text(label, color = SH_LABEL, fontSize = 13.sp)
    }
}
