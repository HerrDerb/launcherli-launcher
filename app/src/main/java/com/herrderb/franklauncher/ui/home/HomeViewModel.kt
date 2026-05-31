package com.herrderb.franklauncher.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herrderb.franklauncher.data.AppInfo
import com.herrderb.franklauncher.data.AppRepository
import com.herrderb.franklauncher.data.SettingsRepository
import com.herrderb.franklauncher.data.weather.WeatherAdapterRegistry
import com.herrderb.franklauncher.data.weather.WeatherConfig
import com.herrderb.franklauncher.data.weather.WeatherData
import com.herrderb.franklauncher.data.weather.StationLocator
import com.herrderb.franklauncher.data.hydro.HydroData
import com.herrderb.franklauncher.data.hydro.HydroProvider
import com.herrderb.franklauncher.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val favoriteApps: List<AppInfo> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val homescreenLocked: Boolean = true,
    val allApps: List<AppInfo> = emptyList(),
    val favoriteTextSize: Float = 18f,
    val favoriteAlignment: String = "left",
    val showDrawerIcons: Boolean = false,
    val weatherApp: String = "",
    val weatherAppInternational: String = "",
    val isInSwitzerland: Boolean = true,
    val weather: WeatherData? = null,
    val hydro: HydroData? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val settingsRepository = SettingsRepository(application)
    private val appRepository = AppRepository(application)
    private val stationLocator = StationLocator(application)
    private val hydroProvider = HydroProvider(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadApps()

            combine(
                settingsRepository.themeMode,
                settingsRepository.favoriteApps,
                settingsRepository.homescreenLocked,
                settingsRepository.favoriteTextSize,
                settingsRepository.favoriteAlignment,
                settingsRepository.showDrawerIcons,
                settingsRepository.weatherApp,
                settingsRepository.weatherAppInternational
            ) { params ->
                val theme = params[0] as ThemeMode
                @Suppress("UNCHECKED_CAST")
                val favPackages = params[1] as List<String>
                val locked = params[2] as Boolean
                val textSize = params[3] as Float
                val alignment = params[4] as String
                val drawerIcons = params[5] as Boolean
                val weatherAppPkg = params[6] as String
                val weatherAppIntlPkg = params[7] as String

                val allApps = _uiState.value.allApps
                val favApps = favPackages.mapNotNull { pkg ->
                    allApps.find { it.packageName == pkg }
                }
                _uiState.value.copy(
                    themeMode = theme,
                    favoriteApps = favApps,
                    homescreenLocked = locked,
                    allApps = allApps,
                    favoriteTextSize = textSize,
                    favoriteAlignment = alignment,
                    showDrawerIcons = drawerIcons,
                    weatherApp = weatherAppPkg,
                    weatherAppInternational = weatherAppIntlPkg
                )
            }.collect { state ->
                _uiState.value = state
            }
        }

        // Refresh weather every 5 minutes
        viewModelScope.launch {
            while (true) {
                refreshWeather()
                delay(5 * 60 * 1000L)
            }
        }

        // Refresh hydro every 5 minutes
        viewModelScope.launch {
            while (true) {
                refreshHydro()
                delay(5 * 60 * 1000L)
            }
        }
    }

    private suspend fun refreshWeather() {
        val locationResult = stationLocator.getLocationResult()

        _uiState.update { it.copy(isInSwitzerland = locationResult.isInSwitzerland) }

        val weather = if (locationResult.isInSwitzerland && locationResult.nearestStationId != null) {
            // Use MeteoSwiss in Switzerland
            val adapter = WeatherAdapterRegistry.getAdapter("meteoswiss") ?: return
            val config = WeatherConfig(stationId = locationResult.nearestStationId)
            adapter.fetchWeather(config)
        } else if (locationResult.latitude != 0.0) {
            // Use Open-Meteo outside Switzerland
            val adapter = WeatherAdapterRegistry.getAdapter("openmeteo") ?: return
            val config = WeatherConfig(
                latitude = locationResult.latitude,
                longitude = locationResult.longitude
            )
            adapter.fetchWeather(config)
        } else null

        if (weather != null) {
            _uiState.update { it.copy(weather = weather) }
        }
    }

    private suspend fun refreshHydro() {
        val locationResult = stationLocator.getLocationResult()
        if (locationResult.isInSwitzerland && locationResult.latitude != 0.0) {
            val hydro = hydroProvider.fetchNearestStation(
                locationResult.latitude, locationResult.longitude
            )
            if (hydro != null) {
                _uiState.update { it.copy(hydro = hydro) }
            }
        }
    }

    suspend fun loadApps() {
        val apps = withContext(Dispatchers.IO) {
            appRepository.getInstalledApps()
        }
        _uiState.update { it.copy(allApps = apps) }
    }

    fun launchApp(appInfo: AppInfo) {
        appRepository.launchApp(appInfo)
    }

    fun toggleHomescreenLock() {
        viewModelScope.launch {
            settingsRepository.setHomescreenLocked(!_uiState.value.homescreenLocked)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setFavoriteTextSize(size: Float) {
        viewModelScope.launch {
            settingsRepository.setFavoriteTextSize(size)
        }
    }

    fun setFavoriteAlignment(alignment: String) {
        viewModelScope.launch {
            settingsRepository.setFavoriteAlignment(alignment)
        }
    }

    fun setShowDrawerIcons(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowDrawerIcons(show)
        }
    }

    fun setWeatherApp(packageName: String) {
        viewModelScope.launch {
            settingsRepository.setWeatherApp(packageName)
        }
    }

    fun setWeatherAppInternational(packageName: String) {
        viewModelScope.launch {
            settingsRepository.setWeatherAppInternational(packageName)
        }
    }

    fun addFavoriteApp(appInfo: AppInfo) {
        viewModelScope.launch {
            val current = _uiState.value.favoriteApps.map { it.packageName }.toMutableList()
            if (appInfo.packageName !in current) {
                current.add(appInfo.packageName)
                settingsRepository.setFavoriteApps(current)
            }
        }
    }

    fun removeFavoriteApp(appInfo: AppInfo) {
        viewModelScope.launch {
            val current = _uiState.value.favoriteApps.map { it.packageName }.toMutableList()
            current.remove(appInfo.packageName)
            settingsRepository.setFavoriteApps(current)
        }
    }

    fun reorderFavorites(apps: List<AppInfo>) {
        viewModelScope.launch {
            settingsRepository.setFavoriteApps(apps.map { it.packageName })
        }
    }

}
