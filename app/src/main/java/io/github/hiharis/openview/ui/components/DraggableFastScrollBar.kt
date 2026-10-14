// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import io.github.hiharis.openview.R
import io.github.hiharis.openview.ui.theme.Dimens
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun DraggableFastScrollBar(
  listState: LazyListState,
  totalItems: Int,
  modifier: Modifier = Modifier,
) {
  if (totalItems <= 1) return

  val coroutineScope = rememberCoroutineScope()
  val density = LocalDensity.current
  var isDragging by remember { mutableStateOf(false) }
  var dragOffsetY by remember { mutableFloatStateOf(0f) }
  val scrollbarDesc = stringResource(id = R.string.content_desc_fast_scrollbar)

  BoxWithConstraints(
    modifier = modifier
      .fillMaxHeight()
      .width(Dimens.scrollbarTouchTargetWidth)
      .semantics { contentDescription = scrollbarDesc }
      .testTag("draggable_fast_scrollbar"),
    contentAlignment = Alignment.TopEnd,
  ) {
    val trackHeightPx = with(density) { maxHeight.toPx() }
    val thumbMinHeightPx = with(density) { Dimens.scrollbarThumbMinHeight.toPx() }

    // Estimate thumb height proportional to visible items, clamped to min height
    val visibleItems = listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
    val thumbHeightPx = (trackHeightPx * (visibleItems.toFloat() / totalItems.toFloat()))
      .coerceIn(thumbMinHeightPx, trackHeightPx)

    val maxScrollOffsetPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)

    // Calculate current scroll progress from LazyListState
    val scrollProgress by remember {
      derivedStateOf {
        val firstVisible = listState.firstVisibleItemIndex
        val offset = listState.firstVisibleItemScrollOffset
        val visibleItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
        val itemSize = (visibleItem?.size ?: 1).coerceAtLeast(1)
        val exactIndex = firstVisible + (offset.toFloat() / itemSize)
        val maxIndex = (totalItems - visibleItems).coerceAtLeast(1).toFloat()
        (exactIndex / maxIndex).coerceIn(0f, 1f)
      }
    }

    // Scroll to position based on relative Y within track
    fun scrollToIndex(y: Float) {
      val clampedY = y.coerceIn(0f, trackHeightPx)
      val fraction = clampedY / trackHeightPx
      val targetIndex = (fraction * (totalItems - 1)).roundToInt().coerceIn(0, totalItems - 1)
      coroutineScope.launch {
        listState.scrollToItem(targetIndex)
      }
    }

    val currentThumbOffsetPx = if (isDragging) {
      dragOffsetY.coerceIn(0f, maxScrollOffsetPx)
    } else {
      scrollProgress * maxScrollOffsetPx
    }

    // Background track
    Box(
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .fillMaxHeight()
        .width(Dimens.scrollbarTrackWidth)
        .background(
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
          shape = RoundedCornerShape(Dimens.scrollbarCornerRadius),
        ),
    )

    // Interactive Touch & Drag Area
    Box(
      modifier = Modifier
        .fillMaxHeight()
        .width(Dimens.scrollbarTouchTargetWidth)
        .pointerInput(totalItems, trackHeightPx) {
          detectTapGestures { offset ->
            scrollToIndex(offset.y)
          }
        }
        .pointerInput(totalItems, trackHeightPx) {
          detectDragGestures(
            onDragStart = { offset ->
              isDragging = true
              dragOffsetY = (offset.y - thumbHeightPx / 2f).coerceIn(0f, maxScrollOffsetPx)
              scrollToIndex(offset.y)
            },
            onDragEnd = {
              isDragging = false
            },
            onDragCancel = {
              isDragging = false
            },
            onDrag = { change, dragAmount ->
              change.consume()
              dragOffsetY = (dragOffsetY + dragAmount.y).coerceIn(0f, maxScrollOffsetPx)
              val targetCenterY = dragOffsetY + thumbHeightPx / 2f
              scrollToIndex(targetCenterY)
            },
          )
        },
    ) {
      // Draggable thumb
      val thumbHeightDp = with(density) { thumbHeightPx.toDp() }
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .offset { IntOffset(x = 0, y = currentThumbOffsetPx.roundToInt()) }
          .height(thumbHeightDp)
          .width(Dimens.scrollbarThumbWidth)
          .background(
            color = if (isDragging) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            },
            shape = RoundedCornerShape(Dimens.scrollbarCornerRadius),
          ),
      )
    }
  }
}
