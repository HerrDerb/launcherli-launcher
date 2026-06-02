package com.herrderb.launcherli.data.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Open-Meteo adapter — free, no API key required.
 * Uses latitude/longitude from WeatherConfig.
 * WMO weather codes: https://open-meteo.com/en/docs
 */
class OpenMeteoAdapter : WeatherAdapter {

    override val id = "openmeteo"
    override val displayName = "Open-Meteo"

    override suspend fun fetchWeather(config: WeatherConfig): WeatherData? = withContext(Dispatchers.IO) {
        try {
            val lat = config.latitude
            val lon = config.longitude
            if (lat == 0.0 && lon == 0.0) return@withContext null

            val url = URL(
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true&hourly=weather_code&forecast_hours=2"
            )
            val connection = url.openConnection() as HttpURLConnection
            connection.useCaches = false
            connection.setRequestProperty("User-Agent", "Launcherli/1.0")
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val json = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val temperature = extractJsonFloat(json, "temperature") ?: return@withContext null
            val weatherCode = extractJsonInt(json, "weathercode") ?: 0
            val condition = wmoCodeToCondition(weatherCode)

            // Extract +1h forecast condition from hourly.weather_code array
            val forecastCondition = extractHourlyWeatherCode(json, 1)?.let { wmoCodeToCondition(it) }

            WeatherData(temperature = temperature, condition = condition, forecastCondition = forecastCondition)
        } catch (e: Exception) {
            null
        }
    }

    private fun wmoCodeToCondition(code: Int): WeatherCondition = when (code) {
        0, 1 -> WeatherCondition.CLEAR           // Clear sky, mainly clear
        2, 3 -> WeatherCondition.CLOUDY           // Partly cloudy, overcast
        45, 48 -> WeatherCondition.CLOUDY         // Fog
        51, 53, 55 -> WeatherCondition.RAINY      // Drizzle
        56, 57 -> WeatherCondition.RAINY          // Freezing drizzle
        61, 63, 65 -> WeatherCondition.RAINY      // Rain
        66, 67 -> WeatherCondition.RAINY          // Freezing rain
        71, 73, 75 -> WeatherCondition.SNOWY      // Snow fall
        77 -> WeatherCondition.SNOWY              // Snow grains
        80, 81, 82 -> WeatherCondition.RAINY      // Rain showers
        85, 86 -> WeatherCondition.SNOWY          // Snow showers
        95, 96, 99 -> WeatherCondition.RAINY      // Thunderstorm
        else -> WeatherCondition.CLOUDY
    }

    private fun extractJsonFloat(json: String, key: String): Float? {
        val pattern = """"$key"\s*:\s*([-\d.]+)""".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toFloatOrNull()
    }

    private fun extractJsonInt(json: String, key: String): Int? {
        val pattern = """"$key"\s*:\s*(\d+)""".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractHourlyWeatherCode(json: String, index: Int): Int? {
        val pattern = """"weather_code"\s*:\s*\[([^\]]+)]""".toRegex()
        val match = pattern.find(json) ?: return null
        val values = match.groupValues[1].split(",").map { it.trim() }
        return values.getOrNull(index)?.toIntOrNull()
    }
}
