package com.herrderb.launcherli.ui.home.widgets

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class ClockTextTest {

    private val zurich = ZoneId.of("Europe/Zurich")
    private val now = ZonedDateTime.of(2026, 10, 6, 14, 5, 0, 0, zurich).toInstant().toEpochMilli()

    @Test
    fun formatsTimeAndDateInTheGivenZone() {
        val text = clockText(now, zurich, Locale.ENGLISH, nextAlarmMs = null)
        assertEquals("14:05", text.time)
        assertEquals("Tue. 6 Oct", text.date)
    }

    @Test
    fun sameInstantFollowsAZoneChange() {
        val tokyo = clockText(now, ZoneId.of("Asia/Tokyo"), Locale.ENGLISH, nextAlarmMs = null)
        assertEquals("21:05", tokyo.time)
        assertEquals("Tue. 6 Oct", tokyo.date)
    }

    @Test
    fun alarmWithinADayIsShown() {
        val alarm = ZonedDateTime.of(2026, 10, 7, 7, 30, 0, 0, zurich).toInstant().toEpochMilli()
        assertEquals("07:30", clockText(now, zurich, Locale.ENGLISH, alarm).nextAlarm)
    }

    @Test
    fun alarmFurtherThanADayIsHidden() {
        val alarm = ZonedDateTime.of(2026, 10, 7, 14, 6, 0, 0, zurich).toInstant().toEpochMilli()
        assertEquals("", clockText(now, zurich, Locale.ENGLISH, alarm).nextAlarm)
    }
}
