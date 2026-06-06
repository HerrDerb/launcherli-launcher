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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    val itemHeight = 40.dp
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

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
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding, end = 24.dp, top = 16.dp, bottom = 16.dp)
            .then(
                if (homescreenLocked) {
                    Modifier.openDrawerOnDrag(onDragDrawer, onDragDrawerEnd)
                } else Modifier
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = favoriteApps,
            key = { _, app -> app.packageName }
        ) { index, app ->
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
                                Modifier.pointerInput(favoriteApps) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { swipeOffsetX = 0f },
                                        onHorizontalDrag = { _, dragAmount ->
                                            swipeOffsetX =
                                                (swipeOffsetX + dragAmount).coerceAtMost(0f)
                                        },
                                        onDragEnd = {
                                            if (swipeOffsetX < -swipeThreshold) {
                                                onRemoveFavorite(app)
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
                                .pointerInput(favoriteApps) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggedIndex = index
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { _, offset ->
                                            dragOffsetY += offset.y
                                            val itemHeightPx =
                                                with(density) { (itemHeight + 12.dp).toPx() }
                                            val moveBy =
                                                (dragOffsetY / itemHeightPx).toInt()
                                            if (moveBy != 0 && draggedIndex != null) {
                                                val fromIndex = draggedIndex!!
                                                val toIndex = (fromIndex + moveBy)
                                                    .coerceIn(
                                                        0,
                                                        favoriteApps.size - 1
                                                    )
                                                if (fromIndex != toIndex) {
                                                    val list =
                                                        favoriteApps.toMutableList()
                                                    val item2 = list.removeAt(fromIndex)
                                                    list.add(toIndex, item2)
                                                    onReorderFavorites(list)
                                                    draggedIndex = toIndex
                                                    dragOffsetY = 0f
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            draggedIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggedIndex = null
                                            dragOffsetY = 0f
                                        }
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}
