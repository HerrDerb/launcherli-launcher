package com.herrderb.launcherli.ui.home.widgets

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.herrderb.launcherli.data.weather.WeatherCondition
import com.herrderb.launcherli.data.weather.WeatherData
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun WeatherWidget(
    weather: WeatherData?,
    showWidgetLabels: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.CenterEnd) {
        weather?.let { w ->
            // On each launcher resume, briefly flip up to the day's max temperature
            // (only while that peak still lies ahead), then flip back to current.
            var showMax by remember { mutableStateOf(false) }
            var resumeTick by remember { mutableIntStateOf(0) }
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumeTick++ }
            LaunchedEffect(resumeTick, w.maxTempAhead, w.maxTemperature) {
                if (w.maxTempAhead && w.maxTemperature != null) {
                    showMax = false
                    delay(1500)
                    showMax = true
                    delay(2000)
                    showMax = false
                } else {
                    showMax = false
                }
            }

            Column(horizontalAlignment = Alignment.Start) {
                if ((showWidgetLabels || w.rateLimited) && w.stationName.isNotEmpty()) {
                    val cleaned = w.stationName
                        .replace(Regex("""\s*\([^)]+\)\s*$"""), "").trim()
                    cleaned.split("/")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .forEach { line ->
                            Text(
                                text = line,
                                fontSize = 11.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                maxLines = 1,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .basicMarquee(velocity = 20.dp)
                            )
                        }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clickable(onClick = onClick)
                ) {
                    if (w.rateLimited) {
                        // Icons carry no text baseline. Since the widget is placed in
                        // the time row with alignBy(LastBaseline), an icon-only state
                        // would have no baseline and snap to the top of the row. An
                        // invisible 20sp anchor keeps the baseline the temperature
                        // would have, so the icon stays aligned with the clock.
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "°",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Light,
                                modifier = Modifier.alpha(0f)
                            )
                            Icon(
                                imageVector = Icons.Outlined.HourglassEmpty,
                                contentDescription = "Rate limited",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        // A single progress value drives a vertical conveyor: 0 = current
                        // reading shown, 1 = day's max shown. The current row is pushed up
                        // and out of the clipped window while the max row slides in from
                        // below, both translated by the same measured row height so they
                        // move together with no resize jump.
                        val progress by animateFloatAsState(
                            targetValue = if (showMax && w.maxTemperature != null) 1f else 0f,
                            animationSpec = tween(450, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)),
                            label = "weather_flip"
                        )
                        var rowHeightPx by remember { mutableIntStateOf(0) }
                        Box(modifier = Modifier.clipToBounds()) {
                            // Current reading — pushed up and out.
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .onGloballyPositioned { rowHeightPx = it.size.height }
                                    .graphicsLayer { translationY = -rowHeightPx * progress }
                            ) {
                                w.forecastCondition?.let { forecast ->
                                    val trend = forecast.rank - w.condition.rank
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
                                    imageVector = when (w.condition) {
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
                                    text = "${w.temperature.roundToInt()}°",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Light,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            // Day's max — slides up from below.
                            if (w.maxTemperature != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.graphicsLayer {
                                        translationY = rowHeightPx * (1f - progress)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.VerticalAlignTop,
                                        contentDescription = "Day's high",
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .padding(end = 6.dp)
                                    )
                                    Text(
                                        text = "${w.maxTemperature.roundToInt()}°",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Light,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
