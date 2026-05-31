package com.herrderb.launcherli.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herrderb.launcherli.data.AppInfo
import com.herrderb.launcherli.data.AppRepository
import com.herrderb.launcherli.data.SettingsRepository
import com.herrderb.launcherli.data.weather.WeatherAdapterRegistry
import com.herrderb.launcherli.data.weather.WeatherConfig
import com.herrderb.launcherli.data.weather.WeatherData
import com.herrderb.launcherli.data.weather.StationLocator
import com.herrderb.launcherli.data.hydro.HydroData
import com.herrderb.launcherli.data.hydro.HydroProvider
import com.herrderb.launcherli.ui.theme.ThemeMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
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

    private val refreshInterval = 5 * 60 * 1000L
    private var lastWeatherRefresh = 0L
    private var lastHydroRefresh = 0L

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
                lastWeatherRefresh = System.currentTimeMillis()
                delay(refreshInterval)
            }
        }

        // Refresh hydro every 5 minutes
        viewModelScope.launch {
            while (true) {
                refreshHydro()
                lastHydroRefresh = System.currentTimeMillis()
                delay(refreshInterval)
            }
        }

        // On resume, refresh immediately if interval has elapsed
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val now = System.currentTimeMillis()
                if (now - lastWeatherRefresh >= refreshInterval) {
                    viewModelScope.launch { refreshWeather(); lastWeatherRefresh = System.currentTimeMillis() }
                }
                if (now - lastHydroRefresh >= refreshInterval) {
                    viewModelScope.launch { refreshHydro(); lastHydroRefresh = System.currentTimeMillis() }
                }
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
    }

    private suspend fun refreshWeather() {
        val locationResult = stationLocator.getLocationResult()

        _uiState.update { it.copy(isInSwitzerland = locationResult.isInSwitzerland) }

        val weather = if (locationResult.isInSwitzerland && locationResult.nearestStationId != null) {
            // Use MeteoSwiss in Switzerland
            val adapter = WeatherAdapterRegistry.getAdapter("meteoswiss") ?: return
            val config = WeatherConfig(
                stationId = locationResult.nearestStationId,
                latitude = locationResult.latitude,
                longitude = locationResult.longitude
            )
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
