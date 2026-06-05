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
import com.herrderb.launcherli.data.weather.LocationInfo
import com.herrderb.launcherli.data.hydro.HydroData
import com.herrderb.launcherli.data.hydro.HydroProvider
import com.herrderb.launcherli.ui.theme.ThemeMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class HomeUiState(
    val favoriteApps: List<AppInfo> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val homescreenLocked: Boolean = true,
    val allApps: List<AppInfo> = emptyList(),
    val favoriteTextSize: Float = 18f,
    val favoriteAlignment: String = "left",
    val showDrawerIcons: Boolean = false,
    val isInSwitzerland: Boolean = true,
    val weather: WeatherData? = null,
    val hydro: HydroData? = null,
    val showWidgetLabels: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val settingsRepository = SettingsRepository(application)
    private val appRepository = AppRepository(application)
    private val stationLocator = StationLocator(application)
    private val hydroProvider = HydroProvider(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val refreshInterval = 15 * 60 * 1000L
    private var lastRefresh = 0L
    private val refreshMutex = Mutex()
    private var cachedLocationResult: LocationInfo? = null

    init {
        viewModelScope.launch {
            loadApps()

            val coreSettings = combine(
                settingsRepository.themeMode,
                settingsRepository.favoriteApps,
                settingsRepository.homescreenLocked,
                settingsRepository.favoriteTextSize,
                settingsRepository.favoriteAlignment,
                settingsRepository.showDrawerIcons
            ) { params ->
                val theme = params[0] as ThemeMode
                @Suppress("UNCHECKED_CAST")
                val favPackages = params[1] as List<String>
                val locked = params[2] as Boolean
                val textSize = params[3] as Float
                val alignment = params[4] as String
                val drawerIcons = params[5] as Boolean

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
                    showDrawerIcons = drawerIcons
                )
            }

            combine(coreSettings, settingsRepository.showWidgetLabels) { state, showLabels ->
                state.copy(showWidgetLabels = showLabels)
            }.collect { state ->
                _uiState.value = state
            }
        }

        // Refresh weather and hydro every 5 minutes
        viewModelScope.launch {
            while (true) {
                lastRefresh = System.currentTimeMillis()
                refreshWidgets()
                delay(refreshInterval)
            }
        }

        // On resume, refresh immediately if interval has elapsed
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (System.currentTimeMillis() - lastRefresh >= refreshInterval) {
                    viewModelScope.launch {
                        refreshWidgets()
                        lastRefresh = System.currentTimeMillis()
                    }
                }
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
    }

    private suspend fun refreshWidgets() = refreshMutex.withLock {
        val location = stationLocator.getLocation() ?: return@withLock
        coroutineScope {
            launch { refreshWeather(location) }
            launch { refreshHydro(location) }
        }
    }

    private suspend fun refreshWeather(location: LocationInfo) {
        val adapter = WeatherAdapterRegistry.getAdapter("openmeteo") ?: return
        val weather = adapter.fetchWeather(
                WeatherConfig(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            )?.let { data ->
                if (data.rateLimited) data.copy(stationName = "Rate limit hit")
                else data.copy(stationName = location.cityName)
            }

        if (weather != null) {
            _uiState.update { it.copy(weather = weather) }
        }
    }

    private suspend fun refreshHydro(location: LocationInfo) {
        if (location.inSwitzerland) {
            val hydro = hydroProvider.fetchNearestStation(location.latitude, location.longitude)
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

    fun setShowWidgetLabels(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowWidgetLabels(show)
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
