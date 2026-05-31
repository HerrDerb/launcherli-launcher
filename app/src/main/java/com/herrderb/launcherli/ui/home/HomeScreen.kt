package com.herrderb.launcherli.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Water
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herrderb.launcherli.data.AppInfo
import com.herrderb.launcherli.data.weather.WeatherCondition
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAppLaunch: (AppInfo) -> Unit,
    onSwipeLeft: () -> Unit,
    onDragDrawer: (Float) -> Unit,
    onDragDrawerEnd: (Float) -> Unit,
    onToggleLock: () -> Unit,
    onOpenSettings: () -> Unit,
    onWeatherClick: () -> Unit,
    onHydroClick: () -> Unit,
    onRemoveFavorite: (AppInfo) -> Unit,
    onReorderFavorites: (List<AppInfo>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = {},
                onLongClick = { showBottomSheet = true }
            )
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                        if (totalDrag < 0f) {
                            val progress = (-totalDrag / size.width).coerceIn(0f, 1f)
                            onDragDrawer(progress)
                        }
                    },
                    onDragEnd = {
                        val progress = (-totalDrag / size.width).coerceIn(0f, 1f)
                        onDragDrawerEnd(progress)
                        totalDrag = 0f
                    }
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Weather + Digital clock row
            var currentTime by remember { mutableStateOf("") }
            var currentDate by remember { mutableStateOf("") }
            var nextAlarmText by remember { mutableStateOf("") }
            val alarmManager = remember {
                context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            }
            LaunchedEffect(Unit) {
                val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                val dateFormatter = SimpleDateFormat("EEE. d MMM", Locale.getDefault())
                val alarmFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                while (true) {
                    val now = System.currentTimeMillis()
                    val date = Date(now)
                    currentTime = timeFormatter.format(date)
                    currentDate = dateFormatter.format(date)
                    val nextAlarm = alarmManager.nextAlarmClock
                    nextAlarmText = if (nextAlarm != null) {
                        alarmFormatter.format(Date(nextAlarm.triggerTime))
                    } else ""
                    // Sleep exactly until the next minute boundary
                    val msUntilNextMinute = 60_000L - (now % 60_000L)
                    delay(msUntilNextMinute)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Time row: weather (left) | clock (center) | hydro (right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Weather widget (left side, clickable)
                    Box(
                        modifier = Modifier.weight(1f).alignBy(LastBaseline),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        uiState.weather?.let { weather ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clickable(onClick = onWeatherClick)
                            ) {
                                // Trend arrow based on +1h forecast
                                weather.forecastCondition?.let { forecast ->
                                    val trend = forecast.rank - weather.condition.rank
                                    if (trend != 0) {
                                        Icon(
                                            imageVector = if (trend < 0) Icons.Outlined.ArrowUpward
                                                else Icons.Outlined.ArrowDownward,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = when (weather.condition) {
                                        WeatherCondition.CLEAR -> Icons.Outlined.WbSunny
                                        WeatherCondition.CLOUDY -> Icons.Outlined.Cloud
                                        WeatherCondition.SNOWY -> Icons.Outlined.AcUnit
                                        WeatherCondition.RAINY -> Icons.Outlined.WaterDrop
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(end = 6.dp)
                                )
                                Text(
                                    text = "${weather.temperature.toInt()}°",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Light,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }

                    // Clock time (center)
                    Text(
                        text = currentTime,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .alignBy(LastBaseline)
                            .clickable {
                                val intent = android.content.Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS)
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            }
                    )

                    // Hydro widget (right side, clickable)
                    Box(
                        modifier = Modifier.weight(1f).alignBy(LastBaseline),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .then(
                                    if (uiState.hydro != null) Modifier.clickable(onClick = onHydroClick)
                                    else Modifier
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Water,
                                contentDescription = null,
                                tint = if (uiState.hydro != null)
                                    MaterialTheme.colorScheme.onBackground
                                else
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 4.dp)
                            )
                            if (uiState.hydro != null) {
                                Text(
                                    text = "${uiState.hydro.temperature.toInt()}°",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Light,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }

                // Date + alarm row (below, centered)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentDate,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    if (nextAlarmText.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Outlined.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = nextAlarmText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(
                        if (!uiState.homescreenLocked) {
                            Modifier.combinedClickable(onClick = { onToggleLock() })
                        } else Modifier
                    )
            )

            // Favorite apps list (bottom - text only)
            Spacer(modifier = Modifier.weight(1f))
            val itemHeight = 40.dp
            val density = LocalDensity.current
            var draggedIndex by remember { mutableStateOf<Int?>(null) }
            var dragOffsetY by remember { mutableFloatStateOf(0f) }

            // Unlock mode indicator
            if (!uiState.homescreenLocked) {
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

            val startPadding = if (uiState.favoriteAlignment == "centered") {
                (LocalConfiguration.current.screenWidthDp * 0.35f).dp
            } else 24.dp

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = startPadding, end = 24.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(
                    items = uiState.favoriteApps,
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
                        if (!uiState.homescreenLocked && swipeOffsetX < 0f) {
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
                                    if (!uiState.homescreenLocked) {
                                        Modifier.pointerInput(uiState.favoriteApps) {
                                            detectHorizontalDragGestures(
                                                onDragStart = { swipeOffsetX = 0f },
                                                onHorizontalDrag = { _, dragAmount ->
                                                    swipeOffsetX = (swipeOffsetX + dragAmount).coerceAtMost(0f)
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
                                fontSize = uiState.favoriteTextSize.sp,
                                fontWeight = FontWeight.Normal,
                                color = animatedColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .combinedClickable(
                                        onClick = { onAppLaunch(app) }
                                    )
                                    .padding(vertical = 4.dp)
                            )
                            if (!uiState.homescreenLocked && swipeOffsetX == 0f) {
                                    Text(
                                        text = "≡",
                                        fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .padding(start = 12.dp)
                                        .pointerInput(uiState.favoriteApps) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    draggedIndex = index
                                                    dragOffsetY = 0f
                                                },
                                                onDrag = { _, offset ->
                                                    dragOffsetY += offset.y
                                                    val itemHeightPx = with(density) { (itemHeight + 12.dp).toPx() }
                                                    val moveBy = (dragOffsetY / itemHeightPx).toInt()
                                                    if (moveBy != 0 && draggedIndex != null) {
                                                        val fromIndex = draggedIndex!!
                                                        val toIndex = (fromIndex + moveBy)
                                                            .coerceIn(0, uiState.favoriteApps.size - 1)
                                                        if (fromIndex != toIndex) {
                                                            val list = uiState.favoriteApps.toMutableList()
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
            Spacer(modifier = Modifier.weight(1f))
        }
    }

    // Bottom sheet menu on long press
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(onClick = {
                            onToggleLock()
                            showBottomSheet = false
                        })
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (uiState.homescreenLocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (uiState.homescreenLocked) "Unlock Homescreen" else "Lock Homescreen",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(onClick = {
                            onOpenSettings()
                            showBottomSheet = false
                        })
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Settings",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(onClick = {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                android.net.Uri.parse("package:com.herrderb.launcherli")
                            )
                            context.startActivity(intent)
                            showBottomSheet = false
                        })
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "App Info",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
