// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.pinchZoom(
    onZoom: (zoomChange: Float, centroid: Offset) -> Unit,
    onDoubleTap: () -> Unit,
): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        awaitEachGesture {
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val downChanges = event.changes.filter { it.pressed }
                if (downChanges.size >= 2) {
                    val zoomChange = event.calculateZoom()
                    val centroid = event.calculateCentroid()
                    if (!zoomChange.isNaN() && zoomChange > 0f && zoomChange != 1f && !zoomChange.isInfinite()) {
                        onZoom(zoomChange, centroid)
                        downChanges.forEach { it.consume() }
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }
).then(
    Modifier.pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = { _ -> onDoubleTap() }
        )
    }
)
