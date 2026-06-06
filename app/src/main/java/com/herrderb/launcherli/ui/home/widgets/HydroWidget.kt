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
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herrderb.launcherli.data.hydro.HydroData

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HydroWidget(
    hydro: HydroData?,
    showWidgetLabels: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        hydro?.let { h ->
            Column(horizontalAlignment = Alignment.Start) {
                val hydroLabel = h.stationLabel.takeIf { it.isNotBlank() }
                if (showWidgetLabels && hydroLabel != null) {
                    hydroLabel.split("-")
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
                                    .padding(start = 12.dp)
                                    .basicMarquee(velocity = 20.dp)
                            )
                        }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clickable(onClick = onClick)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Water,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 4.dp)
                    )
                    Text(
                        text = "${"%.1f".format(h.temperature)}°",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}
