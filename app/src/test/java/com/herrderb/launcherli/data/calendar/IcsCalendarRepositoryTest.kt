package com.herrderb.launcherli.data.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class IcsCalendarRepositoryTest {

    private val zone = ZoneId.of("Europe/Zurich")
    private val today = LocalDate.of(2026, 10, 6) // Tuesday
    private val tomorrow = today.plusDays(1)
    private val repo = IcsCalendarRepository()

    private fun ics(vararg events: String) = buildString {
        append("BEGIN:VCALENDAR\r\nPRODID:-//Proton AG//ProtonCalendar//EN\r\n")
        events.forEach { append("BEGIN:VEVENT\r\n").append(it.trimIndent().replace("\n", "\r\n")).append("\r\nEND:VEVENT\r\n") }
        append("END:VCALENDAR\r\n")
    }

    private fun at(date: LocalDate, time: LocalTime) =
        ZonedDateTime.of(date, time, zone).toInstant().toEpochMilli()

    private fun window(text: String) = repo.collectWindow(text, today, zone)

    @Test
    fun singleTimedEventTodayIsCountedAtItsStart() {
        val result = window(ics("DTSTART:20261006T093000"))
        assertEquals(listOf(at(today, LocalTime.of(9, 30))), result.todayStarts)
        assertTrue(result.tomorrowStarts.isEmpty())
    }

    @Test
    fun allDayEventCountsUntilEndOfDay() {
        val result = window(ics("DTSTART;VALUE=DATE:20261007"))
        assertEquals(listOf(at(tomorrow, LocalTime.MAX)), result.tomorrowStarts)
    }

    @Test
    fun utcTimeIsShiftedIntoDeviceZone() {
        // 23:00 UTC on the 6th is 01:00 on the 7th in Zurich (CEST, UTC+2).
        val result = window(ics("DTSTART:20261006T230000Z"))
        assertTrue(result.todayStarts.isEmpty())
        assertEquals(listOf(at(tomorrow, LocalTime.of(1, 0))), result.tomorrowStarts)
    }

    @Test
    fun foldedLinesAreUnfolded() {
        val result = window(ics("DTSTART:20261006T\r\n 080000"))
        assertEquals(listOf(at(today, LocalTime.of(8, 0))), result.todayStarts)
    }

    @Test
    fun eventsOutsideTheWindowAreIgnored() {
        val result = window(ics("DTSTART:20261005T100000", "DTSTART:20261008T100000"))
        assertTrue(result.todayStarts.isEmpty())
        assertTrue(result.tomorrowStarts.isEmpty())
    }

    @Test
    fun startsAreSorted() {
        val result = window(ics("DTSTART:20261006T150000", "DTSTART:20261006T070000"))
        assertEquals(
            listOf(at(today, LocalTime.of(7, 0)), at(today, LocalTime.of(15, 0))),
            result.todayStarts
        )
    }

    @Test
    fun weeklyByDayExpandsToMatchingWeekdays() {
        val result = window(ics("DTSTART:20260901T100000\r\nRRULE:FREQ=WEEKLY;BYDAY=TU,WE"))
        assertEquals(1, result.todayStarts.size)
        assertEquals(1, result.tomorrowStarts.size)
    }

    @Test
    fun weeklyIntervalSkipsOffWeeks() {
        // Started Tue 2026-09-29, every 2nd week: next is 2026-10-13, not today.
        val result = window(ics("DTSTART:20260929T100000\r\nRRULE:FREQ=WEEKLY;INTERVAL=2"))
        assertTrue(result.todayStarts.isEmpty())
    }

    @Test
    fun dailyIntervalHonoursStep() {
        val result = window(ics("DTSTART:20261004T100000\r\nRRULE:FREQ=DAILY;INTERVAL=2"))
        assertEquals(1, result.todayStarts.size)
        assertTrue(result.tomorrowStarts.isEmpty())
    }

    @Test
    fun monthlyRepeatsOnSameDayOfMonth() {
        val result = window(ics("DTSTART:20260906T100000\r\nRRULE:FREQ=MONTHLY"))
        assertEquals(1, result.todayStarts.size)
    }

    @Test
    fun yearlyRepeatsOnSameDate() {
        val result = window(ics("DTSTART;VALUE=DATE:20201007\r\nRRULE:FREQ=YEARLY"))
        assertEquals(1, result.tomorrowStarts.size)
    }

    @Test
    fun exDateRemovesOccurrence() {
        val result = window(ics("DTSTART:20261001T100000\r\nRRULE:FREQ=DAILY\r\nEXDATE:20261006T100000"))
        assertTrue(result.todayStarts.isEmpty())
        assertEquals(1, result.tomorrowStarts.size)
    }

    @Test
    fun untilEndsTheSeries() {
        val result = window(ics("DTSTART:20261001T100000\r\nRRULE:FREQ=DAILY;UNTIL=20261006T215959Z"))
        assertEquals(1, result.todayStarts.size)
        assertTrue(result.tomorrowStarts.isEmpty())
    }

    @Test
    fun untilTimeIsRespectedOnItsLastDay() {
        // 23:59:59 UTC on the 6th is 01:59:59 on the 7th locally, before the 10:00 start.
        val result = window(ics("DTSTART:20261001T100000\r\nRRULE:FREQ=DAILY;UNTIL=20261006T235959Z"))
        assertEquals(1, result.todayStarts.size)
        assertTrue(result.tomorrowStarts.isEmpty())
    }

    @Test
    fun untilIsInclusiveOfAnOccurrenceStartingExactlyThen() {
        // 08:00 UTC on the 7th is 10:00 locally, the same instant as the occurrence.
        val result = window(ics("DTSTART:20261001T100000\r\nRRULE:FREQ=DAILY;UNTIL=20261007T080000Z"))
        assertEquals(1, result.tomorrowStarts.size)
    }

    @Test
    fun dateOnlyUntilIncludesThatWholeDay() {
        val result = window(ics("DTSTART;VALUE=DATE:20261001\r\nRRULE:FREQ=DAILY;UNTIL=20261007"))
        assertEquals(1, result.tomorrowStarts.size)
    }

    @Test
    fun recurringEventDoesNotOccurBeforeItsStart() {
        val result = window(ics("DTSTART:20261007T100000\r\nRRULE:FREQ=DAILY"))
        assertTrue(result.todayStarts.isEmpty())
        assertEquals(1, result.tomorrowStarts.size)
    }

    @Test
    fun providerIsDetectedFromProdId() {
        assertEquals(SystemCalendarApp, window(ics()).provider)
    }
}
