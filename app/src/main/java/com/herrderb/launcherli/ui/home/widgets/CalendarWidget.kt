package com.herrderb.launcherli.ui.home.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.herrderb.launcherli.data.calendar.CalendarApp
import java.util.concurrent.TimeUnit

/**
 * Today/tomorrow appointment counts from the ICS feed. When [provider] is a
 * recognized calendar app, each row deep-links into that app on its own day.
 */
@Composable
internal fun CalendarWidget(
    todayStarts: List<Long>,
    tomorrowCount: Int,
    nowMs: Long,
    provider: CalendarApp?,
    endPad: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Opens the provider on the day containing [atMillis]; no-op without a provider.
    val openDay: (Long) -> Unit = { atMillis ->
        provider?.openDayIntent(context, atMillis)?.let { intent ->
            runCatching { context.startActivity(intent) }
        }
    }

    Row(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.weight(1f))
        val todayCount = todayStarts.count { it > nowMs }
        Column(horizontalAlignment = Alignment.End) {
            if (todayCount > 0) {
                CountLine(
                    text = "Today · $todayCount",
                    onClick = provider?.let { { openDay(nowMs) } }
                )
            }
            if (tomorrowCount > 0) {
                CountLine(
                    text = "Tomorrow · $tomorrowCount",
                    onClick = provider?.let { { openDay(nowMs + TimeUnit.DAYS.toMillis(1)) } }
                )
            }
        }
        Spacer(modifier = Modifier.width(endPad))
    }
}

/** One appointment-count line, tappable only when [onClick] is provided. */
@Composable
private fun CountLine(text: String, onClick: (() -> Unit)?) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Light,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    )
}
