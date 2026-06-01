package com.herrderb.launcherli.ui.home

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.herrderb.launcherli.data.AppInfo
import com.herrderb.launcherli.data.AppRepository
import com.herrderb.launcherli.data.SettingsRepository
import com.herrderb.launcherli.data.hydro.HydroData
import com.herrderb.launcherli.data.hydro.HydroProvider
import com.herrderb.launcherli.data.weather.*
import com.herrderb.launcherli.ui.theme.ThemeMode
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for HomeViewModel widget (weather/hydro) update behavior:
 * - Regular periodic refresh every 5 minutes
 * - On-resume refresh when the app returns from background after the interval elapsed
 * - On-resume skip refresh when the interval hasn't elapsed
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelWidgetUpdateTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var application: Application
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var appRepository: AppRepository
    private lateinit var stationLocator: StationLocator
    private lateinit var hydroProvider: HydroProvider
    private lateinit var weatherAdapterRegistry: WeatherAdapterRegistry
    private lateinit var weatherAdapter: WeatherAdapter
    private lateinit var lifecycleOwner: LifecycleOwner
    private lateinit var lifecycleRegistry: LifecycleRegistry

    private var currentTime = 1000000L

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        application = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        appRepository = mockk(relaxed = true)
        stationLocator = mockk(relaxed = true)
        hydroProvider = mockk(relaxed = true)
        weatherAdapterRegistry = mockk(relaxed = true)
        weatherAdapter = mockk(relaxed = true)

        lifecycleRegistry = LifecycleRegistry(mockk(relaxed = true))
        lifecycleOwner = mockk(relaxed = true)
        every { lifecycleOwner.lifecycle } returns lifecycleRegistry

        // Default settings flows
        every { settingsRepository.themeMode } returns flowOf(ThemeMode.SYSTEM)
        every { settingsRepository.favoriteApps } returns flowOf(emptyList())
        every { settingsRepository.homescreenLocked } returns flowOf(true)
        every { settingsRepository.favoriteTextSize } returns flowOf(18f)
        every { settingsRepository.favoriteAlignment } returns flowOf("left")
        every { settingsRepository.showDrawerIcons } returns flowOf(false)
        every { settingsRepository.weatherApp } returns flowOf("")
        every { settingsRepository.weatherAppInternational } returns flowOf("")

        // Default app repo returns empty list
        coEvery { appRepository.getInstalledApps() } returns emptyList()

        // Default location: Switzerland with station
        coEvery { stationLocator.getLocationResult() } returns LocationResult(
            latitude = 47.3769,
            longitude = 8.5417,
            isInSwitzerland = true,
            nearestStationId = "sma"
        )

        // Default weather adapter
        every { weatherAdapterRegistry.getAdapter("meteoswiss") } returns weatherAdapter
        every { weatherAdapterRegistry.getAdapter("openmeteo") } returns weatherAdapter
        coEvery { weatherAdapter.fetchWeather(any()) } returns WeatherData(
            temperature = 20.5f,
            condition = WeatherCondition.CLEAR
        )

        // Default hydro provider
        coEvery { hydroProvider.fetchNearestStation(any(), any()) } returns HydroData(
            stationKey = "2135",
            stationLabel = "Zürich - Limmat",
            temperature = 15.3,
            url = "https://www.hydrodaten.admin.ch/de/seen-und-fluesse/stationen-und-daten/2135"
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        val deps = HomeViewModelDeps(
            settingsRepository = settingsRepository,
            appRepository = appRepository,
            stationLocator = stationLocator,
            hydroProvider = hydroProvider,
            weatherAdapterRegistry = weatherAdapterRegistry,
            processLifecycleOwner = lifecycleOwner,
            clock = { currentTime }
        )
        return HomeViewModel(application, deps)
    }

    // ==========================================
    // Regular periodic weather refresh tests
    // ==========================================

    @Test
    fun `weather widget updates on initial creation`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull("Weather should be fetched on init", state.weather)
        assertEquals(20.5f, state.weather!!.temperature)
        assertEquals(WeatherCondition.CLEAR, state.weather!!.condition)
    }

    @Test
    fun `hydro widget updates on initial creation`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull("Hydro should be fetched on init", state.hydro)
        assertEquals(15.3, state.hydro!!.temperature, 0.01)
        assertEquals("Zürich - Limmat", state.hydro!!.stationLabel)
    }

    @Test
    fun `weather widget refreshes after 5 minute interval`() = runTest(testDispatcher) {
        var callCount = 0
        coEvery { weatherAdapter.fetchWeather(any()) } answers {
            callCount++
            WeatherData(
                temperature = 20.0f + callCount,
                condition = WeatherCondition.CLEAR
            )
        }

        val viewModel = createViewModel()
        advanceUntilIdle()

        // First fetch should have occurred
        assertEquals(1, callCount)
        assertEquals(21.0f, viewModel.uiState.value.weather!!.temperature)

        // Advance time by 5 minutes
        currentTime += 5 * 60 * 1000L
        advanceTimeBy(5 * 60 * 1000L)
        advanceUntilIdle()

        // Second fetch should have occurred
        assertEquals(2, callCount)
        assertEquals(22.0f, viewModel.uiState.value.weather!!.temperature)
    }

    @Test
    fun `hydro widget refreshes after 5 minute interval`() = runTest(testDispatcher) {
        var callCount = 0
        coEvery { hydroProvider.fetchNearestStation(any(), any()) } answers {
            callCount++
            HydroData(
                stationKey = "2135",
                stationLabel = "Zürich - Limmat",
                temperature = 15.0 + callCount,
                url = "https://example.com"
            )
        }

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1, callCount)
        assertEquals(16.0, viewModel.uiState.value.hydro!!.temperature, 0.01)

        // Advance time by 5 minutes
        currentTime += 5 * 60 * 1000L
        advanceTimeBy(5 * 60 * 1000L)
        advanceUntilIdle()

        assertEquals(2, callCount)
        assertEquals(17.0, viewModel.uiState.value.hydro!!.temperature, 0.01)
    }

    @Test
    fun `weather does not refresh before interval elapses`() = runTest(testDispatcher) {
        var callCount = 0
        coEvery { weatherAdapter.fetchWeather(any()) } answers {
            callCount++
            WeatherData(temperature = 20.0f, condition = WeatherCondition.CLEAR)
        }

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1, callCount)

        // Advance 3 minutes - should not refresh yet
        currentTime += 3 * 60 * 1000L
        advanceTimeBy(3 * 60 * 1000L)
        advanceUntilIdle()

        assertEquals(1, callCount)
    }

    @Test
    fun `multiple periodic refresh cycles work correctly`() = runTest(testDispatcher) {
        var callCount = 0
        coEvery { weatherAdapter.fetchWeather(any()) } answers {
            callCount++
            WeatherData(temperature = 20.0f + callCount, condition = WeatherCondition.CLEAR)
        }

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(1, callCount)

        // Advance through 3 cycles (15 minutes)
        repeat(3) {
            currentTime += 5 * 60 * 1000L
            advanceTimeBy(5 * 60 * 1000L)
            advanceUntilIdle()
        }

        assertEquals(4, callCount) // initial + 3 periodic
        assertEquals(24.0f, viewModel.uiState.value.weather!!.temperature)
    }

    // ==========================================
    // On-resume refresh tests
    // ==========================================

    @Test
    fun `weather refreshes on resume when interval has elapsed`() = runTest(testDispatcher) {
        var callCount = 0
        coEvery { weatherAdapter.fetchWeather(any()) } answers {
            callCount++
            WeatherData(temperature = 20.0f + callCount, condition = WeatherCondition.CLOUDY)
        }

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(1, callCount)

        // Simulate time passing beyond interval (app in background)
        currentTime += 6 * 60 * 1000L

        // Simulate ON_RESUME lifecycle event
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        advanceUntilIdle()

        // Should have triggered a refresh
        assertTrue("Weather should refresh on resume after interval", callCount >= 2)
        assertEquals(WeatherCondition.CLOUDY, viewModel.uiState.value.weather!!.condition)
    }

    @Test
    fun `hydro refreshes on resume when interval has elapsed`() = runTest(testDispatcher) {
        var callCount = 0
        coEvery { hydroProvider.fetchNearestStation(any(), any()) } answers {
            callCount++
            HydroData(
                stationKey = "2135",
                stationLabel = "Zürich - Limmat",
                temperature = 15.0 + callCount,
                url = "https://example.com"
            )
        }

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(1, callCount)

        // Simulate time passing beyond interval
        currentTime += 6 * 60 * 1000L

        // Simulate ON_RESUME
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        advanceUntilIdle()

        assertTrue("Hydro should refresh on resume after interval", callCount >= 2)
    }

    @Test
    fun `weather does NOT refresh on resume when interval has NOT elapsed`() = runTest(testDispatcher) {
        var callCount = 0
        coEvery { weatherAdapter.fetchWeather(any()) } answers {
            callCount++
            WeatherData(temperature = 20.0f, condition = WeatherCondition.CLEAR)
        }

        val viewModel = createViewModel()
        advanceUntilIdle()
        val countAfterInit = callCount

        // Only 2 minutes pass (less than 5-minute interval)
        currentTime += 2 * 60 * 1000L

        // Simulate ON_RESUME
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        advanceUntilIdle()

        assertEquals(
            "Weather should NOT refresh on resume before interval",
            countAfterInit,
            callCount
        )
    }

    @Test
    fun `hydro does NOT refresh on resume when interval has NOT elapsed`() = runTest(testDispatcher) {
        var callCount = 0
        coEvery { hydroProvider.fetchNearestStation(any(), any()) } answers {
            callCount++
            HydroData(
                stationKey = "2135",
                stationLabel = "Zürich - Limmat",
                temperature = 15.0,
                url = "https://example.com"
            )
        }

        val viewModel = createViewModel()
        advanceUntilIdle()
        val countAfterInit = callCount

        // Only 2 minutes pass
        currentTime += 2 * 60 * 1000L

        // Simulate ON_RESUME
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        advanceUntilIdle()

        assertEquals(
            "Hydro should NOT refresh on resume before interval",
            countAfterInit,
            callCount
        )
    }

    @Test
    fun `on resume exactly at interval boundary triggers refresh`() = runTest(testDispatcher) {
        var callCount = 0
        coEvery { weatherAdapter.fetchWeather(any()) } answers {
            callCount++
            WeatherData(temperature = 20.0f + callCount, condition = WeatherCondition.CLEAR)
        }

        val viewModel = createViewModel()
        advanceUntilIdle()
        val countAfterInit = callCount

        // Advance exactly by the refresh interval
        currentTime += viewModel.refreshInterval

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        advanceUntilIdle()

        assertTrue(
            "Weather should refresh at exactly the interval boundary",
            callCount > countAfterInit
        )
    }

    @Test
    fun `multiple resume events with elapsed interval each trigger refresh`() = runTest(testDispatcher) {
        var callCount = 0
        coEvery { weatherAdapter.fetchWeather(any()) } answers {
            callCount++
            WeatherData(temperature = 20.0f + callCount, condition = WeatherCondition.CLEAR)
        }

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(1, callCount)

        // First resume after interval
        currentTime += 6 * 60 * 1000L
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        advanceUntilIdle()

        val countAfterFirstResume = callCount
        assertTrue(countAfterFirstResume > 1)

        // Second resume after another interval
        currentTime += 6 * 60 * 1000L
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        advanceUntilIdle()

        assertTrue(
            "Each resume after interval should trigger refresh",
            callCount > countAfterFirstResume
        )
    }

    // ==========================================
    // Weather data source selection tests
    // ==========================================

    @Test
    fun `uses MeteoSwiss adapter when in Switzerland`() = runTest(testDispatcher) {
        coEvery { stationLocator.getLocationResult() } returns LocationResult(
            latitude = 47.3769,
            longitude = 8.5417,
            isInSwitzerland = true,
            nearestStationId = "sma"
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        verify { weatherAdapterRegistry.getAdapter("meteoswiss") }
        assertTrue(viewModel.uiState.value.isInSwitzerland)
    }

    @Test
    fun `uses OpenMeteo adapter when outside Switzerland`() = runTest(testDispatcher) {
        coEvery { stationLocator.getLocationResult() } returns LocationResult(
            latitude = 48.8566,
            longitude = 2.3522,
            isInSwitzerland = false,
            nearestStationId = null
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        verify { weatherAdapterRegistry.getAdapter("openmeteo") }
        assertFalse(viewModel.uiState.value.isInSwitzerland)
    }

    @Test
    fun `weather state is null when location unavailable`() = runTest(testDispatcher) {
        coEvery { stationLocator.getLocationResult() } returns LocationResult(
            latitude = 0.0,
            longitude = 0.0,
            isInSwitzerland = false,
            nearestStationId = null
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNull("Weather should be null when location is unavailable", viewModel.uiState.value.weather)
    }

    @Test
    fun `hydro is not fetched when outside Switzerland`() = runTest(testDispatcher) {
        coEvery { stationLocator.getLocationResult() } returns LocationResult(
            latitude = 48.8566,
            longitude = 2.3522,
            isInSwitzerland = false,
            nearestStationId = null
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        coVerify(exactly = 0) { hydroProvider.fetchNearestStation(any(), any()) }
        assertNull(viewModel.uiState.value.hydro)
    }

    // ==========================================
    // Error handling tests
    // ==========================================

    @Test
    fun `weather state unchanged when adapter returns null`() = runTest(testDispatcher) {
        coEvery { weatherAdapter.fetchWeather(any()) } returns null

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNull("Weather should remain null when adapter fails", viewModel.uiState.value.weather)
    }

    @Test
    fun `hydro state unchanged when provider returns null`() = runTest(testDispatcher) {
        coEvery { hydroProvider.fetchNearestStation(any(), any()) } returns null

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNull("Hydro should remain null when provider fails", viewModel.uiState.value.hydro)
    }

    @Test
    fun `weather adapter not found does not crash`() = runTest(testDispatcher) {
        every { weatherAdapterRegistry.getAdapter("meteoswiss") } returns null

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Should not crash, weather remains null
        assertNull(viewModel.uiState.value.weather)
    }

    @Test
    fun `weather updates after failed refresh followed by successful one`() = runTest(testDispatcher) {
        var callCount = 0
        coEvery { weatherAdapter.fetchWeather(any()) } answers {
            callCount++
            if (callCount == 1) null // First call fails
            else WeatherData(temperature = 25.0f, condition = WeatherCondition.RAINY)
        }

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNull("First fetch fails, weather should be null", viewModel.uiState.value.weather)

        // Advance to next refresh cycle
        currentTime += 5 * 60 * 1000L
        advanceTimeBy(5 * 60 * 1000L)
        advanceUntilIdle()

        assertNotNull("Second fetch succeeds", viewModel.uiState.value.weather)
        assertEquals(25.0f, viewModel.uiState.value.weather!!.temperature)
        assertEquals(WeatherCondition.RAINY, viewModel.uiState.value.weather!!.condition)
    }

    // ==========================================
    // Timestamp tracking tests
    // ==========================================

    @Test
    fun `lastWeatherRefresh is updated after successful refresh`() = runTest(testDispatcher) {
        currentTime = 5000000L

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(5000000L, viewModel.lastWeatherRefresh)
    }

    @Test
    fun `lastHydroRefresh is updated after successful refresh`() = runTest(testDispatcher) {
        currentTime = 5000000L

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(5000000L, viewModel.lastHydroRefresh)
    }

    @Test
    fun `lastWeatherRefresh is updated on resume refresh`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        currentTime = 9000000L // Well past 5 min interval

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        advanceUntilIdle()

        assertEquals(9000000L, viewModel.lastWeatherRefresh)
    }
}
