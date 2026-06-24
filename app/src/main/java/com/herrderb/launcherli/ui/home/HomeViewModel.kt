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
import com.herrderb.launcherli.data.calendar.CalendarApp
import com.herrderb.launcherli.ui.theme.ThemeMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

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
    val showWidgetLabels: Boolean = false,
    val calendarIcsUrl: String = "",
    val todayAppointmentStarts: List<Long> = emptyList(),
    val tomorrowAppointments: Int = 0,
    val appointmentsLoaded: Boolean = false,
    val calendarProvider: CalendarApp? = null,
    val showMostUsedApps: Boolean = true,
    val mostUsedApps: List<AppInfo> = emptyList()
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val settingsRepository = SettingsRepository(application)
    private val appRepository = AppRepository(application)
    private val stationLocator = StationLocator(application)
    private val hydroProvider = HydroProvider(application)
    private val icsRepo = com.herrderb.launcherli.data.calendar.IcsCalendarRepository()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val refreshInterval = TimeUnit.MINUTES.toMillis(15)
    private var lastRefresh = 0L
    private val calendarRefreshInterval = TimeUnit.MINUTES.toMillis(30)
    private var lastCalendarRefresh = 0L
    private val refreshMutex = Mutex()

    init {
        viewModelScope.launch {
            loadApps()

            val coreSettings = combine(
                settingsRepository.themeMode,
                settingsRepository.favoriteApps,
                settingsRepository.homescreenLocked,
                settingsRepository.favoriteTextSize,
                settingsRepository.favoriteAlignment,
                settingsRepository.showDrawerIcons,
                settingsRepository.calendarIcsUrl,
                settingsRepository.showMostUsedApps,
                settingsRepository.appUsageCounts
            ) { params ->
                val theme = params[0] as ThemeMode
                @Suppress("UNCHECKED_CAST")
                val favPackages = params[1] as List<String>
                val locked = params[2] as Boolean
                val textSize = params[3] as Float
                val alignment = params[4] as String
                val drawerIcons = params[5] as Boolean
                val icsUrl = params[6] as String
                val showMostUsed = params[7] as Boolean
                @Suppress("UNCHECKED_CAST")
                val usageCounts = params[8] as Map<String, Int>

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
                    calendarIcsUrl = icsUrl,
                    showMostUsedApps = showMostUsed,
                    mostUsedApps = mostUsedFrom(usageCounts, allApps)
                )
            }

            combine(coreSettings, settingsRepository.showWidgetLabels) { state, showLabels ->
                state.copy(showWidgetLabels = showLabels)
            }.collect { state ->
                _uiState.value = state
            }
        }

        // Initial + on-change appointment fetch (cheap; only fires when the link changes).
        viewModelScope.launch {
            settingsRepository.calendarIcsUrl.distinctUntilChanged().collect { url ->
                refreshCalendar(url)
            }
        }

        // Periodic refresh runs ONLY while the launcher is in the foreground.
        // repeatOnLifecycle cancels these loops when the app is backgrounded and
        // restarts them on return, so no location/network work happens while the
        // user is in other apps. On each return we refresh once if data is stale,
        // then poll at the fixed interval until backgrounded again.
        val appLifecycle = ProcessLifecycleOwner.get()
        appLifecycle.lifecycleScope.launch {
            appLifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (System.currentTimeMillis() - lastRefresh >= refreshInterval) {
                    refreshWidgets()
                    lastRefresh = System.currentTimeMillis()
                }
                while (true) {
                    delay(refreshInterval)
                    refreshWidgets()
                    lastRefresh = System.currentTimeMillis()
                }
            }
        }
        appLifecycle.lifecycleScope.launch {
            appLifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (System.currentTimeMillis() - lastCalendarRefresh >= calendarRefreshInterval) {
                    refreshCalendar(settingsRepository.calendarIcsUrl.first())
                }
                while (true) {
                    delay(calendarRefreshInterval)
                    refreshCalendar(settingsRepository.calendarIcsUrl.first())
                }
            }
        }
    }

    private suspend fun refreshWidgets() = refreshMutex.withLock {
        val location = stationLocator.getLocation() ?: return@withLock
        coroutineScope {
            launch { refreshWeather(location) }
            launch { refreshHydro(location) }
        }
    }

    private suspend fun refreshCalendar(icsUrl: String) = withContext(Dispatchers.IO) {
        lastCalendarRefresh = System.currentTimeMillis()
        if (icsUrl.isBlank()) {
            _uiState.update {
                it.copy(
                    todayAppointmentStarts = emptyList(),
                    tomorrowAppointments = 0,
                    appointmentsLoaded = false
                )
            }
            return@withContext
        }
        val times = icsRepo.fetchTimes(icsUrl) ?: return@withContext
        _uiState.update {
            it.copy(
                todayAppointmentStarts = times.todayStarts,
                tomorrowAppointments = times.tomorrowStarts.size,
                appointmentsLoaded = true,
                calendarProvider = times.provider
            )
        }
    }

    fun setCalendarIcsUrl(url: String) {
        viewModelScope.launch {
            settingsRepository.setCalendarIcsUrl(url.trim())
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

    /** Top apps (count ≥ threshold), sorted by launches, mapped to installed apps. */
    private fun mostUsedFrom(counts: Map<String, Int>, allApps: List<AppInfo>): List<AppInfo> =
        counts.entries
            .filter { it.value >= SettingsRepository.MOST_USED_MIN_LAUNCHES }
            .sortedByDescending { it.value }
            .mapNotNull { e -> allApps.find { it.packageName == e.key } }
            .take(SettingsRepository.MOST_USED_MAX)

    /** @param countUsage when true, the launch is tallied for the "most used" list. */
    fun launchApp(appInfo: AppInfo, countUsage: Boolean = false) {
        appRepository.launchApp(appInfo)
        if (countUsage) {
            viewModelScope.launch { settingsRepository.recordAppLaunch(appInfo.packageName) }
        }
    }

    fun setShowMostUsedApps(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowMostUsedApps(show)
        }
    }

    fun resetAppUsage() {
        viewModelScope.launch {
            settingsRepository.clearAppUsageCounts()
        }
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
