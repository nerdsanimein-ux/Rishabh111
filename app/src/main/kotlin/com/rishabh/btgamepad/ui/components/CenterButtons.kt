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

private val SYS_COLOR = Color(0xFF2E2E2E)
private val SYS_LABEL = Color(0xFF777777)

// Three circular system buttons: Back (←), Home (⊙), Start (▶)
@Composable
fun CenterButtons(
    onSelect: (Boolean) -> Unit,
    onStart: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SysButton("←", onSelect)
        SysButton("⊙", {})       // home indicator — no HID mapping
        SysButton("▶", onStart)
    }
}

@Composable
private fun SysButton(label: String, onPressed: (Boolean) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .background(SYS_COLOR, CircleShape)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.any { it.pressed }
                        if (pressed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPressed(pressed)
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        Text(label, color = SYS_LABEL, fontSize = 14.sp)
    }
}
