package com.herrderb.launcherli.data.calendar

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * A calendar app the launcher can recognize from an iCal feed and deep-link into.
 *
 * Add a new provider by implementing this interface and registering the instance
 * in [CalendarApps]. Nothing else in the UI or data layer needs to change.
 */
interface CalendarApp {

    /** True if this app should handle the feed, judged from its iCal PRODID line. */
    fun matchesProdId(prodId: String): Boolean

    /**
     * Intent that opens the calendar focused on the day containing [atMillis], or
     * null if no app can handle it.
     */
    fun openDayIntent(context: Context, atMillis: Long): Intent?
}

/**
 * Generic calendar target. Uses the standard AOSP "view time" intent without
 * pinning a package, so it opens the device's default calendar app on that day.
 * This intent is honored by Proton Calendar, Google Calendar, and most others.
 */
object SystemCalendarApp : CalendarApp {

    override fun matchesProdId(prodId: String): Boolean = true

    override fun openDayIntent(context: Context, atMillis: Long): Intent? {
        val view = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("content://com.android.calendar/time/$atMillis")
        }
        return if (view.resolveActivity(context.packageManager) != null) view else null
    }
}

/** Registry of recognized calendar apps, tried in order. */
object CalendarApps {

    private val all: List<CalendarApp> = listOf(SystemCalendarApp)

    /** The app that should handle a feed with the given PRODID, or null if none. */
    fun detect(prodId: String): CalendarApp? = all.firstOrNull { it.matchesProdId(prodId) }
}
