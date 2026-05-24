package com.rishabh.btgamepad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

private val COLOR_BASE  = Color(0xFF1A2035)
private val COLOR_THUMB = Color(0xFF3D5AFE) // vivid indigo thumb

@Composable
fun AnalogStick(
    label: String,
    scale: Float = 1f,
    onMove: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val baseRadius  = (54 * scale).dp
    val thumbRadius = (20 * scale).dp
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .size(baseRadius * 2)
            .background(COLOR_BASE, CircleShape)
            .pointerInput(Unit) {
                val maxPx = baseRadius.toPx() - thumbRadius.toPx()
                detectDragGestures(
                    onDragStart = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val raw = thumbOffset + dragAmount
                        val mag = sqrt(raw.x * raw.x + raw.y * raw.y)
                        thumbOffset = if (mag <= maxPx) raw
                                      else Offset(raw.x / mag * maxPx, raw.y / mag * maxPx)
                        onMove(thumbOffset.x / maxPx, thumbOffset.y / maxPx)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(thumbRadius * 2)
                .offset { IntOffset(thumbOffset.x.toInt(), thumbOffset.y.toInt()) }
                .background(COLOR_THUMB, CircleShape)
        )
        Text(label, color = Color(0x88FFFFFF), fontSize = (11 * scale).sp)
    }
}
