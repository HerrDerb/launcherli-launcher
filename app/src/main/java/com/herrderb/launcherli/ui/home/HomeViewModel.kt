package com.herrderb.launcherli.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herrderb.launcherli.data.AppInfo
import com.herrderb.launcherli.data.AppRepository
import com.herrderb.launcherli.data.SettingsRepository
import com.herrderb.launcherli.data.weather.WeatherAdapterRegistry
import com.herrderb.launcherli.data.weather.WeatherConfig
import com.herrderb.launcherli.data.weather.StationLocator
import com.herrderb.launcherli.data.hydro.HydroProvider
import com.herrderb.launcherli.ui.theme.ThemeMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

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

/**
 * Dependencies for HomeViewModel, extracted for testability.
 */
open class HomeViewModelDeps(
    val settingsRepository: SettingsRepository,
    val appRepository: AppRepository,
    val stationLocator: StationLocator,
    val hydroProvider: HydroProvider,
    val weatherAdapterRegistry: WeatherAdapterRegistry = WeatherAdapterRegistry,
    val processLifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get(),
    val clock: () -> Long = { System.currentTimeMillis() }
)

class HomeViewModel(application: Application, private val deps: HomeViewModelDeps? = null) : AndroidViewModel(application) {

    val settingsRepository: SettingsRepository = deps?.settingsRepository ?: SettingsRepository(application)
    private val appRepository: AppRepository = deps?.appRepository ?: AppRepository(application)
    private val stationLocator: StationLocator = deps?.stationLocator ?: StationLocator(application)
    private val hydroProvider: HydroProvider = deps?.hydroProvider ?: HydroProvider(application)
    private val weatherAdapterRegistry: WeatherAdapterRegistry = deps?.weatherAdapterRegistry ?: WeatherAdapterRegistry
    private val clock: () -> Long = deps?.clock ?: { System.currentTimeMillis() }
    private val processLifecycleOwner: LifecycleOwner = deps?.processLifecycleOwner ?: ProcessLifecycleOwner.get()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    internal val refreshInterval = 5 * 60 * 1000L
    internal var lastWeatherRefresh = 0L
    internal var lastHydroRefresh = 0L

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
                lastWeatherRefresh = clock()
                delay(refreshInterval)
            }
        }

        // Refresh hydro every 5 minutes
        viewModelScope.launch {
            while (true) {
                refreshHydro()
                lastHydroRefresh = clock()
                delay(refreshInterval)
            }
        }

        // On resume, refresh immediately if interval has elapsed
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val now = clock()
                if (now - lastWeatherRefresh >= refreshInterval) {
                    viewModelScope.launch { refreshWeather(); lastWeatherRefresh = clock() }
                }
                if (now - lastHydroRefresh >= refreshInterval) {
                    viewModelScope.launch { refreshHydro(); lastHydroRefresh = clock() }
                }
            }
        }
        processLifecycleOwner.lifecycle.addObserver(lifecycleObserver)
    }

    internal suspend fun refreshWeather() {
        val locationResult = stationLocator.getLocationResult()

        _uiState.update { it.copy(isInSwitzerland = locationResult.isInSwitzerland) }

        val weather = if (locationResult.isInSwitzerland && locationResult.nearestStationId != null) {
            // Use MeteoSwiss in Switzerland
            val adapter = weatherAdapterRegistry.getAdapter("meteoswiss") ?: return
            val config = WeatherConfig(
                stationId = locationResult.nearestStationId,
                latitude = locationResult.latitude,
                longitude = locationResult.longitude
            )
            adapter.fetchWeather(config)
        } else if (locationResult.latitude != 0.0) {
            // Use Open-Meteo outside Switzerland
            val adapter = weatherAdapterRegistry.getAdapter("openmeteo") ?: return
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

    internal suspend fun refreshHydro() {
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
