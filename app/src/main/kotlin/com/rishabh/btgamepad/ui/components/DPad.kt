package com.rishabh.btgamepad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishabh.btgamepad.hid.HidConstants

private val DPAD_COLOR = Color(0xFF283593) // indigo

@Composable
fun DPad(
    onDirectionChange: (Byte) -> Unit,
    scale: Float = 1f,
    modifier: Modifier = Modifier
) {
    val btnSize = (48 * scale).dp   // cross = 144dp at scale=1
    var up    by remember { mutableStateOf(false) }
    var down  by remember { mutableStateOf(false) }
    var left  by remember { mutableStateOf(false) }
    var right by remember { mutableStateOf(false) }

    fun recompute() {
        val dir: Byte = when {
            up && right   -> HidConstants.DPAD_UP_RIGHT
            up && left    -> HidConstants.DPAD_UP_LEFT
            down && right -> HidConstants.DPAD_DOWN_RIGHT
            down && left  -> HidConstants.DPAD_DOWN_LEFT
            up            -> HidConstants.DPAD_UP
            right         -> HidConstants.DPAD_RIGHT
            down          -> HidConstants.DPAD_DOWN
            left          -> HidConstants.DPAD_LEFT
            else          -> HidConstants.DPAD_CENTERED
        }
        onDirectionChange(dir)
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        DPadKey("▲", btnSize, { up = true; recompute() }, { up = false; recompute() })
        Row {
            DPadKey("◀", btnSize, { left = true; recompute() }, { left = false; recompute() })
            Spacer(Modifier.size(btnSize))
            DPadKey("▶", btnSize, { right = true; recompute() }, { right = false; recompute() })
        }
        DPadKey("▼", btnSize, { down = true; recompute() }, { down = false; recompute() })
    }
}

@Composable
private fun DPadKey(
    label: String,
    size: androidx.compose.ui.unit.Dp,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .background(DPAD_COLOR, RoundedCornerShape(6.dp))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.pressed }) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPress()
                        } else {
                            onRelease()
                        }
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        Text(label, color = Color.White, fontSize = (14 * (size.value / 46f)).sp)
    }
}
