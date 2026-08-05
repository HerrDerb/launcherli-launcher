package com.herrderb.launcherli.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.herrderb.launcherli.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcherli_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val FAVORITE_APPS = stringPreferencesKey("favorite_apps")
        private val HOMESCREEN_LOCKED = booleanPreferencesKey("homescreen_locked")
        private val FAVORITE_TEXT_SIZE = floatPreferencesKey("favorite_text_size")
        private val FAVORITE_ALIGNMENT = stringPreferencesKey("favorite_alignment")
        private val SHOW_DRAWER_ICONS = booleanPreferencesKey("show_drawer_icons")
        private val SHOW_WIDGET_LABELS = booleanPreferencesKey("show_widget_labels")
        private val CALENDAR_ICS_URL = stringPreferencesKey("calendar_ics_url")
        private val SHOW_MOST_USED = booleanPreferencesKey("show_most_used")
        private val APP_USAGE_COUNTS = stringPreferencesKey("app_usage_counts")
        private val CONTACT_SEARCH_ENABLED = booleanPreferencesKey("contact_search_enabled")

        /** Min drawer launches before an app qualifies for the "most used" list. */
        const val MOST_USED_MIN_LAUNCHES = 5
        /** Max number of apps shown in the "most used" list. */
        const val MOST_USED_MAX = 4
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[THEME_MODE]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val favoriteApps: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[FAVORITE_APPS]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    val homescreenLocked: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[HOMESCREEN_LOCKED] ?: true
    }

    val favoriteTextSize: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[FAVORITE_TEXT_SIZE] ?: 18f
    }

    val favoriteAlignment: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[FAVORITE_ALIGNMENT] ?: "left"
    }

    val showDrawerIcons: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SHOW_DRAWER_ICONS] ?: false
    }

    val showWidgetLabels: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SHOW_WIDGET_LABELS] ?: false
    }

    val calendarIcsUrl: Flow<String> = context.dataStore.data.map { prefs ->
        SecretCipher.decrypt(prefs[CALENDAR_ICS_URL] ?: "")
    }

    val showMostUsedApps: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SHOW_MOST_USED] ?: true
    }

    /** Drawer launch counts, keyed by package name. */
    val appUsageCounts: Flow<Map<String, Int>> = context.dataStore.data.map { prefs ->
        decodeCounts(prefs[APP_USAGE_COUNTS])
    }

    val contactSearchEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[CONTACT_SEARCH_ENABLED] ?: false
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun setFavoriteApps(packageNames: List<String>) {
        context.dataStore.edit { it[FAVORITE_APPS] = packageNames.joinToString(",") }
    }

    suspend fun setHomescreenLocked(locked: Boolean) {
        context.dataStore.edit { it[HOMESCREEN_LOCKED] = locked }
    }

    suspend fun setFavoriteTextSize(size: Float) {
        context.dataStore.edit { it[FAVORITE_TEXT_SIZE] = size.coerceIn(12f, 32f) }
    }

    suspend fun setFavoriteAlignment(alignment: String) {
        context.dataStore.edit { it[FAVORITE_ALIGNMENT] = alignment }
    }

    suspend fun setShowDrawerIcons(show: Boolean) {
        context.dataStore.edit { it[SHOW_DRAWER_ICONS] = show }
    }

    suspend fun setShowWidgetLabels(show: Boolean) {
        context.dataStore.edit { it[SHOW_WIDGET_LABELS] = show }
    }

    suspend fun setCalendarIcsUrl(url: String) {
        context.dataStore.edit { it[CALENDAR_ICS_URL] = SecretCipher.encrypt(url.trim()) }
    }

    suspend fun setShowMostUsedApps(show: Boolean) {
        context.dataStore.edit { it[SHOW_MOST_USED] = show }
    }

    suspend fun setContactSearchEnabled(enabled: Boolean) {
        context.dataStore.edit { it[CONTACT_SEARCH_ENABLED] = enabled }
    }

    /** Clears all drawer launch counts, emptying the "most used" list. */
    suspend fun clearAppUsageCounts() {
        context.dataStore.edit { it.remove(APP_USAGE_COUNTS) }
    }

    /** Increments the drawer launch count for [packageName]. */
    suspend fun recordAppLaunch(packageName: String) {
        context.dataStore.edit { prefs ->
            val counts = decodeCounts(prefs[APP_USAGE_COUNTS]).toMutableMap()
            counts[packageName] = (counts[packageName] ?: 0) + 1
            prefs[APP_USAGE_COUNTS] = encodeCounts(counts)
        }
    }

    private fun decodeCounts(raw: String?): Map<String, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(',').mapNotNull { entry ->
            val sep = entry.lastIndexOf(':')
            if (sep <= 0) return@mapNotNull null
            val pkg = entry.substring(0, sep)
            val count = entry.substring(sep + 1).toIntOrNull() ?: return@mapNotNull null
            pkg to count
        }.toMap()
    }

    private fun encodeCounts(counts: Map<String, Int>): String =
        counts.entries.joinToString(",") { "${it.key}:${it.value}" }

}
