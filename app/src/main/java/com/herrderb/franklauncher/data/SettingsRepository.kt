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
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val FAVORITE_APPS = stringPreferencesKey("favorite_apps")
        private val HOMESCREEN_LOCKED = booleanPreferencesKey("homescreen_locked")
        private val FAVORITE_TEXT_SIZE = floatPreferencesKey("favorite_text_size")
        private val FAVORITE_ALIGNMENT = stringPreferencesKey("favorite_alignment")
        private val SHOW_DRAWER_ICONS = booleanPreferencesKey("show_drawer_icons")
        private val WEATHER_APP = stringPreferencesKey("weather_app")
        private val WEATHER_APP_INTERNATIONAL = stringPreferencesKey("weather_app_international")
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

    val weatherApp: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[WEATHER_APP] ?: ""
    }

    val weatherAppInternational: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[WEATHER_APP_INTERNATIONAL] ?: ""
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

    suspend fun setWeatherApp(packageName: String) {
        context.dataStore.edit { it[WEATHER_APP] = packageName }
    }

    suspend fun setWeatherAppInternational(packageName: String) {
        context.dataStore.edit { it[WEATHER_APP_INTERNATIONAL] = packageName }
    }

}
