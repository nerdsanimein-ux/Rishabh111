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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/**
 * Circular drag-gesture joystick. Reports normalized [-1, 1] x/y on every move.
 * Thumb snaps to center on finger lift, sending a (0, 0) report.
 */
@Composable
fun AnalogStick(
    label: String,
    onMove: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val baseRadius = 56.dp
    val thumbRadius = 20.dp
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(baseRadius * 2)
            .background(Color(0xFF2E2E2E), CircleShape)
            .pointerInput(Unit) {
                val maxPx = baseRadius.toPx() - thumbRadius.toPx()
                detectDragGestures(
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
                .background(Color(0xFF9E9E9E), CircleShape)
        )
        Text(label, color = Color.White)
    }
}
