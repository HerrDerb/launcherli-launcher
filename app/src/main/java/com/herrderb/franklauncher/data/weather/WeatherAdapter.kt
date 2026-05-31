package com.herrderb.franklauncher.data.weather

/**
 * Weather data model returned by any weather adapter.
 */
data class WeatherData(
    val temperature: Float,
    val condition: WeatherCondition
)

enum class WeatherCondition {
    CLEAR, CLOUDY, RAINY, SNOWY
}

/**
 * Adapter interface for weather data sources.
 * Implement this to add new weather providers.
 */
interface WeatherAdapter {
    val id: String
    val displayName: String

    /**
     * Fetch current weather data. Returns null on failure.
     */
    suspend fun fetchWeather(config: WeatherConfig): WeatherData?
}

/**
 * Configuration for a weather adapter (e.g. station ID, location, API key).
 */
data class WeatherConfig(
    val stationId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val extras: Map<String, String> = emptyMap()
)
