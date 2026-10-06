package com.herrderb.launcherli.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herrderb.launcherli.data.AppInfo
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FavoritesList(
    favoriteApps: List<AppInfo>,
    homescreenLocked: Boolean,
    favoriteTextSize: Float,
    startPadding: Dp,
    onAppLaunch: (AppInfo) -> Unit,
    onRemoveFavorite: (AppInfo) -> Unit,
    onReorderFavorites: (List<AppInfo>) -> Unit,
    onDragDrawer: (Float) -> Unit,
    onDragDrawerEnd: (Float) -> Unit
) {
    val density = LocalDensity.current
    val itemSpacing = 12.dp
    val listState = rememberLazyListState()
    // While dragging, the order lives here and is persisted once on release.
    // Reset whenever the stored favorites change.
    var order by remember(favoriteApps) { mutableStateOf(favoriteApps) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val currentFavorites by rememberUpdatedState(favoriteApps)
    val currentOnReorder by rememberUpdatedState(onReorderFavorites)
    val currentOnRemove by rememberUpdatedState(onRemoveFavorite)

    // Unlock mode indicator
    if (!homescreenLocked) {
        Text(
            text = "✎ Editing — tap below widgets to lock",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp)
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding, end = 24.dp, top = 16.dp, bottom = 16.dp)
            .then(
                if (homescreenLocked) {
                    Modifier.openDrawerOnDrag(onDragDrawer, onDragDrawerEnd)
                } else Modifier
            ),
        verticalArrangement = Arrangement.spacedBy(itemSpacing)
    ) {
        items(items = order, key = { it.key }) { app ->
            var swipeOffsetX by remember { mutableFloatStateOf(0f) }
            val swipeThreshold = with(density) { 100.dp.toPx() }
            val isSwiped = swipeOffsetX < -swipeThreshold
            val swipeFraction = ((-swipeOffsetX) / swipeThreshold).coerceIn(0f, 1.5f)

            val animatedColor by animateColorAsState(
                targetValue = if (isSwiped) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onBackground,
                label = "swipe_color"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
            ) {
                // Remove indicator behind the item
                if (!homescreenLocked && swipeOffsetX < 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .matchParentSize()
                            .alpha(swipeFraction.coerceAtMost(1f)),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = if (isSwiped) "Release to remove" else "← Remove",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(swipeOffsetX.roundToInt(), 0) }
                        .then(
                            if (!homescreenLocked) {
                                Modifier.pointerInput(app.key) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { swipeOffsetX = 0f },
                                        onHorizontalDrag = { _, dragAmount ->
                                            swipeOffsetX =
                                                (swipeOffsetX + dragAmount).coerceAtMost(0f)
                                        },
                                        onDragEnd = {
                                            if (swipeOffsetX < -swipeThreshold) {
                                                currentOnRemove(app)
                                            }
                                            swipeOffsetX = 0f
                                        },
                                        onDragCancel = {
                                            swipeOffsetX = 0f
                                        }
                                    )
                                }
                            } else Modifier
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = app.label,
                        fontSize = favoriteTextSize.sp,
                        fontWeight = FontWeight.Normal,
                        color = animatedColor,
                        modifier = Modifier
                            .weight(1f)
                            .combinedClickable(
                                onClick = { onAppLaunch(app) }
                            )
                            .padding(vertical = 4.dp)
                    )
                    if (!homescreenLocked && swipeOffsetX == 0f) {
                        Text(
                            text = "≡",
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier
                                .padding(start = 12.dp)
                                // Keyed by the app, not the list: a list change must not
                                // restart the detector and end the drag after one step.
                                .pointerInput(app.key) {
                                    fun commit() {
                                        dragOffsetY = 0f
                                        if (order != currentFavorites) currentOnReorder(order)
                                    }
                                    detectDragGestures(
                                        onDragStart = { dragOffsetY = 0f },
                                        onDrag = { change, offset ->
                                            change.consume()
                                            dragOffsetY += offset.y
                                            val rowPx = listState.layoutInfo.visibleItemsInfo
                                                .firstOrNull { it.key == app.key }?.size
                                            val from = order.indexOfFirst { it.key == app.key }
                                            if (rowPx != null && from >= 0) {
                                                val step = reorderStep(
                                                    order, from, dragOffsetY,
                                                    stepPx = rowPx + itemSpacing.toPx()
                                                )
                                                order = step.items
                                                dragOffsetY = step.offsetPx
                                            }
                                        },
                                        onDragEnd = { commit() },
                                        onDragCancel = { commit() }
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}

internal data class ReorderStep<T>(val items: List<T>, val index: Int, val offsetPx: Float)

/**
 * Moves the item at [fromIndex] by as many whole [stepPx] rows as [offsetPx] covers
 * (clamped to the list), keeping the leftover fraction so the row tracks the finger.
 */
internal fun <T> reorderStep(items: List<T>, fromIndex: Int, offsetPx: Float, stepPx: Float): ReorderStep<T> {
    val steps = (offsetPx / stepPx).toInt()
    val toIndex = (fromIndex + steps).coerceIn(0, items.lastIndex)
    if (toIndex == fromIndex) return ReorderStep(items, fromIndex, if (steps == 0) offsetPx else offsetPx - steps * stepPx)
    val moved = items.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
    return ReorderStep(moved, toIndex, offsetPx - steps * stepPx)
}
