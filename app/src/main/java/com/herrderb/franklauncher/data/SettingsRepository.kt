package com.herrderb.franklauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.herrderb.franklauncher.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "frank_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val WIDGET_HEIGHT_FRACTION = floatPreferencesKey("widget_height_fraction")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val FAVORITE_APPS = stringPreferencesKey("favorite_apps")
        private val HOMESCREEN_LOCKED = booleanPreferencesKey("homescreen_locked")
    }

    val widgetHeightFraction: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[WIDGET_HEIGHT_FRACTION] ?: 0.5f
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

    suspend fun setWidgetHeightFraction(fraction: Float) {
        context.dataStore.edit { it[WIDGET_HEIGHT_FRACTION] = fraction.coerceIn(0.2f, 0.8f) }
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
}
