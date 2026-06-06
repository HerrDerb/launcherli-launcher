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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.herrderb.launcherli.data.calendar.CalendarProvider

/** Today/tomorrow appointment counts from the ICS feed. Tapping opens the
 * provider's app when it is a recognized provider (e.g. Proton Calendar). */
@Composable
internal fun CalendarWidget(
    todayStarts: List<Long>,
    tomorrowCount: Int,
    nowMs: Long,
    provider: CalendarProvider,
    endPad: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.weight(1f))
        val todayCount = todayStarts.count { it > nowMs }
        val calendarLaunch = remember(provider) {
            if (provider == CalendarProvider.PROTON)
                context.packageManager.getLaunchIntentForPackage("me.proton.android.calendar")
            else null
        }
        Column(
            horizontalAlignment = Alignment.End,
            modifier = if (calendarLaunch != null) {
                Modifier.clickable { context.startActivity(calendarLaunch) }
            } else Modifier
        ) {
            if (todayCount > 0) {
                Text(
                    text = "Today · $todayCount",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                )
            }
            if (tomorrowCount > 0) {
                Text(
                    text = "Tomorrow · $tomorrowCount",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                )
            }
        }
        Spacer(modifier = Modifier.width(endPad))
    }
}
