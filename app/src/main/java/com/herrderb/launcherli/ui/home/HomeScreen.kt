package com.herrderb.launcherli.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herrderb.launcherli.data.AppInfo
import com.herrderb.launcherli.ui.home.widgets.CalendarWidget
import com.herrderb.launcherli.ui.home.widgets.ClockWidget
import com.herrderb.launcherli.ui.home.widgets.HydroWidget
import com.herrderb.launcherli.ui.home.widgets.WeatherWidget
import com.herrderb.launcherli.ui.home.widgets.rememberClockState

@OptIn(ExperimentalFoundationApi::class)
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
    val density = LocalDensity.current
    val clock = rememberClockState()

    // Horizontal positions of the clock glyphs, used to align the date row and
    // the favorites column under the visible edges of the time text.
    var clockStartX by remember { mutableFloatStateOf(0f) }
    var clockEndX by remember { mutableFloatStateOf(0f) }
    var clockLineLeft by remember { mutableFloatStateOf(0f) }
    var clockLineRight by remember { mutableFloatStateOf(0f) }
    var dateRowStartX by remember { mutableFloatStateOf(0f) }
    var dateRowWidth by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = {},
                onLongClick = { showBottomSheet = true }
            )
            .openDrawerOnDrag(onDragDrawer, onDragDrawerEnd)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    WeatherWidget(
                        weather = uiState.weather,
                        showWidgetLabels = uiState.showWidgetLabels,
                        onClick = onWeatherClick,
                        modifier = Modifier
                            .weight(1f)
                            .alignBy(LastBaseline)
                    )

                    // Clock time (center)
                    ClockWidget(
                        time = clock.time,
                        onLineMeasured = { left, right ->
                            clockLineLeft = left
                            clockLineRight = right
                        },
                        onPositioned = { startX, endX ->
                            clockStartX = startX
                            clockEndX = endX
                        },
                        modifier = Modifier.alignBy(LastBaseline)
                    )

                    HydroWidget(
                        hydro = uiState.hydro,
                        showWidgetLabels = uiState.showWidgetLabels,
                        onClick = onHydroClick,
                        modifier = Modifier
                            .weight(1f)
                            .alignBy(LastBaseline)
                    )
                }

                // Date + alarm row (date aligned with clock left, alarm with clock right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            dateRowStartX = coords.positionInRoot().x
                            dateRowWidth = coords.size.width.toFloat()
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val alarmEndPad = if (clockEndX > 0f && dateRowWidth > 0f) {
                        val visibleClockRight = clockStartX + clockLineRight
                        with(density) { (dateRowStartX + dateRowWidth - visibleClockRight).coerceAtLeast(0f).toDp() }
                    } else 0.dp

                    Spacer(modifier = Modifier.weight(1f))
                    if (clock.nextAlarm.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Outlined.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = clock.nextAlarm,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text = clock.date,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(alarmEndPad))
                }

                // Appointment counts (today / tomorrow), aligned under the date
                if (uiState.calendarIcsUrl.isNotBlank() && uiState.appointmentsLoaded) {
                    val apptEndPad = if (clockEndX > 0f && dateRowWidth > 0f) {
                        val visibleClockRight = clockStartX + clockLineRight
                        with(density) { (dateRowStartX + dateRowWidth - visibleClockRight).coerceAtLeast(0f).toDp() }
                    } else 0.dp
                    CalendarWidget(
                        todayStarts = uiState.todayAppointmentStarts,
                        tomorrowCount = uiState.tomorrowAppointments,
                        nowMs = clock.nowMs,
                        provider = uiState.calendarProvider,
                        endPad = apptEndPad
                    )
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
            val startPadding = if (uiState.favoriteAlignment == "centered" && clockStartX > 0f) {
                with(density) { (clockStartX + clockLineLeft).toDp() }
            } else 24.dp
            FavoritesList(
                favoriteApps = uiState.favoriteApps,
                homescreenLocked = uiState.homescreenLocked,
                favoriteTextSize = uiState.favoriteTextSize,
                startPadding = startPadding,
                onAppLaunch = onAppLaunch,
                onRemoveFavorite = onRemoveFavorite,
                onReorderFavorites = onReorderFavorites,
                onDragDrawer = onDragDrawer,
                onDragDrawerEnd = onDragDrawerEnd
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }

    // Bottom sheet menu on long press
    if (showBottomSheet) {
        HomeMenuSheet(
            homescreenLocked = uiState.homescreenLocked,
            onToggleLock = onToggleLock,
            onOpenSettings = onOpenSettings,
            onDismiss = { showBottomSheet = false }
        )
    }
}
