package com.herrderb.launcherli.ui.home.widgets

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.AlarmClock
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Live clock/date/next-alarm state, refreshed every minute via ACTION_TIME_TICK. */
internal class ClockState {
    var time by mutableStateOf("")
    var date by mutableStateOf("")
    var nextAlarm by mutableStateOf("")
    var nowMs by mutableLongStateOf(System.currentTimeMillis())
}

@Composable
internal fun rememberClockState(): ClockState {
    val context = LocalContext.current
    val state = remember { ClockState() }
    DisposableEffect(Unit) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormatter = SimpleDateFormat("EEE. d MMM", Locale.getDefault())

        fun update() {
            val now = System.currentTimeMillis()
            state.nowMs = now
            val date = Date(now)
            state.time = timeFormatter.format(date)
            state.date = dateFormatter.format(date)
            val nextAlarm = alarmManager.nextAlarmClock
            state.nextAlarm = if (nextAlarm != null && nextAlarm.triggerTime - now <= 24 * 60 * 60 * 1000L) {
                timeFormatter.format(Date(nextAlarm.triggerTime))
            } else ""
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) = update()
        }

        update()
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_TIME_TICK))
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}

/**
 * The central clock readout. Tapping opens the system alarm app.
 *
 * @param onLineMeasured reports the left/right x of the rendered glyphs (px),
 *   used by the caller to align the date row and favorites under the time.
 * @param onPositioned reports the clock's start/end x within the root (px).
 */
@Composable
internal fun ClockWidget(
    time: String,
    onLineMeasured: (left: Float, right: Float) -> Unit,
    onPositioned: (startX: Float, endX: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Text(
        text = time,
        fontSize = 64.sp,
        fontWeight = FontWeight.Light,
        color = MaterialTheme.colorScheme.onBackground,
        onTextLayout = { layout ->
            if (layout.lineCount > 0) {
                onLineMeasured(layout.getLineLeft(0), layout.getLineRight(0))
            }
        },
        modifier = modifier
            .onGloballyPositioned { coords ->
                val startX = coords.positionInRoot().x
                onPositioned(startX, startX + coords.size.width)
            }
            .clickable {
                val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                }
            }
    )
}
