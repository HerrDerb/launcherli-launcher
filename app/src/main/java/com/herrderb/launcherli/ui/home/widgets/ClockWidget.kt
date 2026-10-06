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
import androidx.compose.runtime.Stable
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Clock texts for one moment; pure so it can be tested and always uses the zone passed in. */
internal data class ClockText(val time: String = "", val date: String = "", val nextAlarm: String = "")

internal fun clockText(nowMs: Long, zone: ZoneId, locale: Locale, nextAlarmMs: Long?): ClockText {
    val time = DateTimeFormatter.ofPattern("HH:mm", locale)
    val date = DateTimeFormatter.ofPattern("EEE. d MMM", locale)
    val alarm = nextAlarmMs
        ?.takeIf { it - nowMs <= TimeUnit.DAYS.toMillis(1) }
        ?.let { time.format(Instant.ofEpochMilli(it).atZone(zone)) }
        .orEmpty()
    val now = Instant.ofEpochMilli(nowMs).atZone(zone)
    return ClockText(time.format(now), date.format(now), alarm)
}

/** Live clock/date/next-alarm state. Read it in the leaf composables that display it. */
@Stable
internal class ClockState {
    var text by mutableStateOf(ClockText())
    var nowMs by mutableLongStateOf(System.currentTimeMillis())
}

/**
 * Updates once a minute via ACTION_TIME_TICK, plus immediately on time, zone,
 * locale or next-alarm changes. The receiver exists only while the launcher is
 * visible, so the process is not woken every minute while it sits in the background.
 */
@Composable
internal fun rememberClockState(): ClockState {
    val context = LocalContext.current
    val state = remember { ClockState() }
    LifecycleStartEffect(Unit) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        fun update() {
            val now = System.currentTimeMillis()
            state.nowMs = now
            state.text = clockText(now, ZoneId.systemDefault(), Locale.getDefault(), alarmManager.nextAlarmClock?.triggerTime)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) = update()
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_LOCALE_CHANGED)
            addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
        }

        update()
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onStopOrDispose { context.unregisterReceiver(receiver) }
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
    clock: ClockState,
    onLineMeasured: (left: Float, right: Float) -> Unit,
    onPositioned: (startX: Float, endX: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Text(
        text = clock.text.time,
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
