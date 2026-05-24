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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** ABXY face buttons in standard diamond layout with accurate press/release reporting. */
@Composable
fun ActionButtons(
    onA: (Boolean) -> Unit,
    onB: (Boolean) -> Unit,
    onX: (Boolean) -> Unit,
    onY: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val btnSize = 52.dp

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        FaceButton("Y", Color(0xFFFFEB3B), btnSize, onY)
        Row(verticalAlignment = Alignment.CenterVertically) {
            FaceButton("X", Color(0xFF2196F3), btnSize, onX)
            Spacer(Modifier.size(btnSize))
            FaceButton("B", Color(0xFFF44336), btnSize, onB)
        }
        FaceButton("A", Color(0xFF4CAF50), btnSize, onA)
    }
}

@Composable
private fun FaceButton(
    label: String,
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    onPressedChanged: (Boolean) -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .background(color, CircleShape)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        onPressedChanged(event.changes.any { it.pressed })
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        Text(label, color = Color.White, fontSize = 16.sp)
    }
}
