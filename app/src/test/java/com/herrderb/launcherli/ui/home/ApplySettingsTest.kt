package com.herrderb.launcherli.ui.home

import com.herrderb.launcherli.data.AppInfo
import com.herrderb.launcherli.data.hydro.HydroData
import com.herrderb.launcherli.data.weather.WeatherCondition
import com.herrderb.launcherli.data.weather.WeatherData
import com.herrderb.launcherli.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ApplySettingsTest {

    private val a = AppInfo("A", "com.a", "com.a.Main")
    private val b = AppInfo("B", "com.b", "com.b.Main")

    private val settings = SettingsSnapshot(
        themeMode = ThemeMode.DARK,
        favoriteKeys = listOf(b.key, "com.a", "com.gone/x"),
        homescreenLocked = false,
        favoriteTextSize = 22f,
        favoriteAlignment = "centered",
        showDrawerIcons = true,
        calendarIcsUrl = "https://cal",
        showMostUsedApps = true,
        usageCounts = mapOf(a.key to 7),
        showWidgetLabels = true,
        contactSearchEnabled = true
    )

    @Test
    fun keepsWidgetDataThatArrivedConcurrently() {
        val weather = WeatherData(12f, WeatherCondition.CLEAR)
        val hydro = HydroData("1", "Aare", 17.2, "u")
        val before = HomeUiState(weather = weather, hydro = hydro, todayAppointmentStarts = listOf(1L), appointmentsLoaded = true)

        val after = before.withSettings(settings, listOf(a, b))

        assertEquals(weather, after.weather)
        assertEquals(hydro, after.hydro)
        assertEquals(listOf(1L), after.todayAppointmentStarts)
        assertEquals(true, after.appointmentsLoaded)
    }

    @Test
    fun resolvesFavoritesInOrderIncludingLegacyEntries() {
        assertEquals(listOf(b, a), HomeUiState().withSettings(settings, listOf(a, b)).favoriteApps)
    }

    @Test
    fun copiesSettingsAndApps() {
        val s = HomeUiState().withSettings(settings, listOf(a, b))
        assertEquals(ThemeMode.DARK, s.themeMode)
        assertEquals(listOf(a, b), s.allApps)
        assertEquals(listOf(a), s.mostUsedApps)
        assertEquals(true, s.contactSearchEnabled)
        assertEquals("https://cal", s.calendarIcsUrl)
    }

    @Test
    fun legacyAndKeyedFavoriteForTheSameAppAppearOnce() {
        val dup = settings.copy(favoriteKeys = listOf("com.a", a.key, b.key))
        assertEquals(listOf(a, b), HomeUiState().withSettings(dup, listOf(a, b)).favoriteApps)
    }
}
