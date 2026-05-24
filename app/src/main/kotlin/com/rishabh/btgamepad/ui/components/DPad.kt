package com.rishabh.btgamepad.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rishabh.btgamepad.hid.HidConstants
import kotlin.math.abs
import kotlin.math.min

private val DPAD_COLOR  = Color(0xFF363636)
private val ARROW_COLOR = Color(0xFF888888)

private class CrossShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val armW = size.width / 3f
        val armH = size.height / 3f
        val r = min(armW, armH) * 0.35f
        val path = Path().apply {
            addRoundRect(RoundRect(0f, (size.height - armH) / 2f, size.width, (size.height + armH) / 2f, CornerRadius(r)))
            addRoundRect(RoundRect((size.width - armW) / 2f, 0f, (size.width + armW) / 2f, size.height, CornerRadius(r)))
        }
        return Outline.Generic(path)
    }
}

@Composable
fun DPad(
    onDirectionChange: (Byte) -> Unit,
    size: Dp = 140.dp,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .size(size)
            .clip(CrossShape())
            .background(DPAD_COLOR)
            .pointerInput(size) {
                val sizePx = size.toPx()
                var lastDir: Byte = HidConstants.DPAD_CENTERED
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedChange = event.changes.firstOrNull { it.pressed }
                        val dir: Byte = if (pressedChange == null) {
                            HidConstants.DPAD_CENTERED
                        } else {
                            val pos = pressedChange.position
                            val cx = sizePx / 2f; val cy = sizePx / 2f
                            val dx = pos.x - cx; val dy = pos.y - cy
                            val ax = abs(dx); val ay = abs(dy)
                            when {
                                ax > ay * 2f -> if (dx > 0) HidConstants.DPAD_RIGHT else HidConstants.DPAD_LEFT
                                ay > ax * 2f -> if (dy < 0) HidConstants.DPAD_UP   else HidConstants.DPAD_DOWN
                                dx > 0 && dy < 0 -> HidConstants.DPAD_UP_RIGHT
                                dx < 0 && dy < 0 -> HidConstants.DPAD_UP_LEFT
                                dx > 0 && dy > 0 -> HidConstants.DPAD_DOWN_RIGHT
                                else             -> HidConstants.DPAD_DOWN_LEFT
                            }
                        }
                        if (dir != lastDir) {
                            lastDir = dir
                            if (dir != HidConstants.DPAD_CENTERED)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDirectionChange(dir)
                        }
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = this.size.width
            val arm = s / 3f
            val cx = s / 2f; val cy = s / 2f
            val arrowSz = arm * 0.38f
            val inset = arm * 0.42f
            fun drawArrow(acx: Float, acy: Float, rot: Float) {
                rotate(rot, pivot = Offset(acx, acy)) {
                    val hw = arrowSz * 0.5f; val hh = arrowSz * 0.4f
                    val path = Path().apply {
                        moveTo(acx, acy - hh); lineTo(acx + hw, acy + hh); lineTo(acx - hw, acy + hh); close()
                    }
                    drawPath(path, ARROW_COLOR)
                }
            }
            drawArrow(cx, inset, 0f)
            drawArrow(cx, s - inset, 180f)
            drawArrow(inset, cy, -90f)
            drawArrow(s - inset, cy, 90f)
        }
    }
}
