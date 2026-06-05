package com.herrderb.launcherli.data.calendar

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Event start times (epoch millis) for today and tomorrow. Today's list lets the
 * UI count down live as `now` passes each start, without re-fetching. All-day
 * events get an end-of-day instant so they stay counted until midnight.
 */
data class AppointmentTimes(val todayStarts: List<Long>, val tomorrowStarts: List<Long>)

/**
 * Reads a public iCalendar (.ics) subscription URL — e.g. a Proton Calendar
 * "share via link" feed — and counts upcoming events for today and tomorrow.
 *
 * Only plaintext event metadata (DTSTART / DTEND / RRULE) is needed; no auth.
 * Recurring events are expanded for the small today/tomorrow window.
 */
class IcsCalendarRepository {

    /** One parsed VEVENT, reduced to what counting needs. */
    private data class Ev(
        val startDate: LocalDate,
        val startTime: LocalTime?,        // null = all-day
        val rrule: Map<String, String>?,
        val until: LocalDate?,
        val byDays: Set<Int>,             // 1=Mon..7=Sun (weekly BYDAY)
        val interval: Int,
        val exDates: Set<LocalDate>
    )

    fun fetchTimes(icsUrl: String): AppointmentTimes? {
        val text = try {
            download(icsUrl)
        } catch (e: Exception) {
            Log.e(TAG, "ICS fetch failed", e)
            return null
        }
        return try {
            collectWindow(text)
        } catch (e: Exception) {
            Log.e(TAG, "ICS parse failed", e)
            null
        }
    }

    private fun download(urlStr: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "frank-launcher")
            setRequestProperty("Accept", "text/calendar, */*")
        }
        conn.inputStream.use { return it.bufferedReader().readText() }
    }

    private fun collectWindow(ics: String): AppointmentTimes {
        // RFC 5545 line unfolding: a CRLF followed by space/tab continues the line.
        val unfolded = ics.replace(Regex("\\r?\\n[ \\t]"), "")
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val tomorrow = today.plusDays(1)

        val todayStarts = mutableListOf<Long>()
        val tomorrowStarts = mutableListOf<Long>()

        for (block in eventBlocks(unfolded)) {
            val ev = parseEvent(block, zone) ?: continue
            if (occursOn(ev, today)) todayStarts.add(instantOn(today, ev.startTime, zone))
            if (occursOn(ev, tomorrow)) tomorrowStarts.add(instantOn(tomorrow, ev.startTime, zone))
        }
        return AppointmentTimes(todayStarts.sorted(), tomorrowStarts.sorted())
    }

    /** Epoch millis for an occurrence; all-day (null time) → end of that day. */
    private fun instantOn(date: LocalDate, time: LocalTime?, zone: ZoneId): Long {
        val zdt = ZonedDateTime.of(date, time ?: LocalTime.MAX, zone)
        return zdt.toInstant().toEpochMilli()
    }

    private fun eventBlocks(text: String): List<String> {
        val result = mutableListOf<String>()
        var idx = 0
        while (true) {
            val begin = text.indexOf("BEGIN:VEVENT", idx)
            if (begin < 0) break
            val end = text.indexOf("END:VEVENT", begin)
            if (end < 0) break
            result.add(text.substring(begin, end))
            idx = end + 10
        }
        return result
    }

    private fun parseEvent(block: String, zone: ZoneId): Ev? {
        var startDate: LocalDate? = null
        var startTime: LocalTime? = null
        var rrule: Map<String, String>? = null
        val exDates = mutableSetOf<LocalDate>()

        for (raw in block.lineSequence()) {
            val line = raw.trim()
            val colon = line.indexOf(':')
            if (colon < 0) continue
            val name = line.substring(0, colon)
            val value = line.substring(colon + 1).trim()
            val key = name.substringBefore(';').uppercase()
            when (key) {
                "DTSTART" -> {
                    val parsed = parseDateTime(name, value, zone)
                    startDate = parsed.first
                    startTime = parsed.second
                }
                "RRULE" -> rrule = value.split(';').mapNotNull {
                    val kv = it.split('=', limit = 2)
                    if (kv.size == 2) kv[0].uppercase() to kv[1] else null
                }.toMap()
                "EXDATE" -> value.split(',').forEach { v ->
                    runCatching { parseDateTime(name, v.trim(), zone).first }.getOrNull()?.let(exDates::add)
                }
            }
        }

        val sd = startDate ?: return null
        if (rrule == null) {
            return Ev(sd, startTime, null, null, emptySet(), 1, exDates)
        }
        val until = rrule["UNTIL"]?.let { runCatching { parseDateTime("UNTIL", it, zone).first }.getOrNull() }
        val interval = rrule["INTERVAL"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val byDays = rrule["BYDAY"]?.split(',')
            ?.mapNotNull { dayCode(it.takeLast(2)) }?.toSet() ?: emptySet()
        return Ev(sd, startTime, rrule, until, byDays, interval, exDates)
    }

    /**
     * Parses an iCal date/datetime value.
     * @return (localDate, localTime?) where time is null for all-day (VALUE=DATE).
     */
    private fun parseDateTime(name: String, value: String, zone: ZoneId): Pair<LocalDate, LocalTime?> {
        val isDateOnly = name.contains("VALUE=DATE", true) && !name.contains("DATE-TIME", true)
        if (isDateOnly || value.length == 8) {
            val d = LocalDate.of(value.substring(0, 4).toInt(), value.substring(4, 6).toInt(), value.substring(6, 8).toInt())
            return d to null
        }
        val year = value.substring(0, 4).toInt()
        val month = value.substring(4, 6).toInt()
        val day = value.substring(6, 8).toInt()
        val hour = value.substring(9, 11).toInt()
        val min = value.substring(11, 13).toInt()
        val sec = if (value.length >= 15) value.substring(13, 15).toInt() else 0
        return if (value.endsWith("Z")) {
            // UTC instant → convert to the device zone.
            val z = ZonedDateTime.of(year, month, day, hour, min, sec, 0, ZoneOffset.UTC)
                .withZoneSameInstant(zone)
            z.toLocalDate() to z.toLocalTime()
        } else {
            // Floating / TZID local wall-clock; treat as device-local for bucketing.
            LocalDate.of(year, month, day) to LocalTime.of(hour, min, sec)
        }
    }

    private fun occursOn(ev: Ev, date: LocalDate): Boolean {
        if (date in ev.exDates) return false
        val rule = ev.rrule ?: return date == ev.startDate
        if (date.isBefore(ev.startDate)) return false
        ev.until?.let { if (date.isAfter(it)) return false }

        return when (rule["FREQ"]?.uppercase()) {
            "DAILY" -> ChronoUnit.DAYS.between(ev.startDate, date) % ev.interval == 0L
            "WEEKLY" -> {
                val days = ev.byDays.ifEmpty { setOf(ev.startDate.dayOfWeek.value) }
                if (date.dayOfWeek.value !in days) false
                else (ChronoUnit.DAYS.between(ev.startDate, date) / 7) % ev.interval == 0L
            }
            "MONTHLY" -> date.dayOfMonth == ev.startDate.dayOfMonth &&
                ChronoUnit.MONTHS.between(ev.startDate.withDayOfMonth(1), date.withDayOfMonth(1)) % ev.interval == 0L
            "YEARLY" -> date.dayOfMonth == ev.startDate.dayOfMonth &&
                date.monthValue == ev.startDate.monthValue &&
                (date.year - ev.startDate.year) % ev.interval == 0
            else -> date == ev.startDate
        }
    }

    /** "MO".."SU" → 1..7 (ISO, Mon=1). */
    private fun dayCode(code: String): Int? = when (code.uppercase()) {
        "MO" -> 1; "TU" -> 2; "WE" -> 3; "TH" -> 4; "FR" -> 5; "SA" -> 6; "SU" -> 7
        else -> null
    }

    companion object {
        private const val TAG = "IcsCalendar"
    }
}
