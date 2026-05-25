package com.rishabh.btgamepad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishabh.btgamepad.ui.LayoutMode

enum class ShoulderSide { LEFT, RIGHT }

private val SH_COLOR = Color(0xFF3A3A3A)
private val SH_LABEL = Color(0xFF999999)

/**
 * Xbox: buttons placed SIDE-BY-SIDE (Row) — LT|LB on left, RB|RT on right.
 * PS:   buttons STACKED (Column) — L1 over L2, R1 over R2.
 */
@Composable
fun ShoulderButtons(
    side: ShoulderSide,
    layout: LayoutMode = LayoutMode.XBOX,
    size: Dp = 72.dp,
    onL1: ((Boolean) -> Unit)? = null,
    onL2: ((Boolean) -> Unit)? = null,
    onR1: ((Boolean) -> Unit)? = null,
    onR2: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val gap = 6.dp

    if (layout == LayoutMode.XBOX) {
        Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
            if (side == ShoulderSide.LEFT) {
                ShoulderCircle("LT", size, onL2)
                ShoulderCircle("LB", size, onL1)
            } else {
                ShoulderCircle("RB", size, onR1)
                ShoulderCircle("RT", size, onR2)
            }
        }
    } else {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
            if (side == ShoulderSide.LEFT) {
                ShoulderCircle("L1", size, onL1)
                ShoulderCircle("L2", size, onL2)
            } else {
                ShoulderCircle("R1", size, onR1)
                ShoulderCircle("R2", size, onR2)
            }
        }
    }
}

@Composable
private fun ShoulderCircle(
    label: String,
    size: Dp,
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
        Text(label, color = SH_LABEL, fontSize = (size.value * 0.18f).sp)
    }
}
