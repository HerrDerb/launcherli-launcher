package com.herrderb.launcherli.ui.home.widgets

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
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herrderb.launcherli.data.weather.WeatherCondition
import com.herrderb.launcherli.data.weather.WeatherData
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
                        Icon(
                            imageVector = Icons.Outlined.HourglassEmpty,
                            contentDescription = "Rate limited",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
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
                }
            }
        }
    }
}
