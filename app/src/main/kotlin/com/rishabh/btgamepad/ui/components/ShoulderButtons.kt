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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

enum class ShoulderSide { LEFT, RIGHT }

@Composable
fun ShoulderButtons(
    side: ShoulderSide,
    onL1: ((Boolean) -> Unit)? = null,
    onL2: ((Boolean) -> Unit)? = null,
    onR1: ((Boolean) -> Unit)? = null,
    onR2: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (side == ShoulderSide.LEFT) {
            ShoulderKey("L1", onL1)
            Spacer(Modifier.height(4.dp))
            ShoulderKey("L2", onL2)
        } else {
            ShoulderKey("R1", onR1)
            Spacer(Modifier.height(4.dp))
            ShoulderKey("R2", onR2)
        }
    }
}

@Composable
private fun ShoulderKey(label: String, onPressed: ((Boolean) -> Unit)?) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 72.dp, height = 36.dp)
            .background(Color(0xFF616161), RoundedCornerShape(6.dp))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        onPressed?.invoke(event.changes.any { it.pressed })
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        Text(label, color = Color.White)
    }
}
