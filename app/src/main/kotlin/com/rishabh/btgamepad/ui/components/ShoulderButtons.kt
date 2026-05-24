package com.rishabh.btgamepad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishabh.btgamepad.ui.LayoutMode

enum class ShoulderSide { LEFT, RIGHT }

private val TRIGGER_COLOR = Color(0xFF1565C0) // L2/R2 darker
private val BUMPER_COLOR  = Color(0xFF1E88E5) // L1/R1 lighter

// LEFT side: [Trigger][Bumper]  (LT/L2 on left, LB/L1 on right)
// RIGHT side: [Bumper][Trigger] (RB/R1 on left, RT/R2 on right)

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
    val size = 46.dp

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (side == ShoulderSide.LEFT) {
            val trigLabel = if (layout == LayoutMode.XBOX) "LT" else "L2"
            val bmpLabel  = if (layout == LayoutMode.XBOX) "LB" else "L1"
            ShoulderCircle(trigLabel, TRIGGER_COLOR, size, onL2)
            ShoulderCircle(bmpLabel,  BUMPER_COLOR,  size, onL1)
        } else {
            val bmpLabel  = if (layout == LayoutMode.XBOX) "RB" else "R1"
            val trigLabel = if (layout == LayoutMode.XBOX) "RT" else "R2"
            ShoulderCircle(bmpLabel,  BUMPER_COLOR,  size, onR1)
            ShoulderCircle(trigLabel, TRIGGER_COLOR, size, onR2)
        }
    }
}

@Composable
private fun ShoulderCircle(
    label: String,
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    onPressed: ((Boolean) -> Unit)?
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
                        onPressed?.invoke(pressed)
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}
