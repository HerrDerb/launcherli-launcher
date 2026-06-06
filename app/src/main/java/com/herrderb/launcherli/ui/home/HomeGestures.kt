package com.herrderb.launcherli.ui.home

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/** Detects a leftward horizontal drag and reports progress to open the app drawer. */
internal fun Modifier.openDrawerOnDrag(
    onDragDrawer: (Float) -> Unit,
    onDragDrawerEnd: (Float) -> Unit
): Modifier = this.pointerInput(Unit) {
    var totalDrag = 0f
    detectHorizontalDragGestures(
        onDragStart = { totalDrag = 0f },
        onHorizontalDrag = { _, dragAmount ->
            totalDrag += dragAmount
            if (totalDrag < 0f) {
                onDragDrawer((-totalDrag / size.width).coerceIn(0f, 1f))
            }
        },
        onDragEnd = {
            onDragDrawerEnd((-totalDrag / size.width).coerceIn(0f, 1f))
            totalDrag = 0f
        },
        onDragCancel = {
            onDragDrawerEnd((-totalDrag / size.width).coerceIn(0f, 1f))
            totalDrag = 0f
        }
    )
}
