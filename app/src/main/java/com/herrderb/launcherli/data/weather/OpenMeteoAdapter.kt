package com.herrderb.launcherli.data.weather

import android.util.Log
import com.posthog.PostHog
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
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current_weather=true&hourly=temperature_2m,weather_code" +
                    "&daily=temperature_2m_max&timezone=auto&forecast_days=1"
            )
            PostHog.capture(event = "openmeteo_fetch")
            val connection = url.openConnection() as HttpURLConnection
            connection.useCaches = false
            connection.setRequestProperty("User-Agent", "Launcherli/1.0")
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode == 429) {
                connection.disconnect()
                return@withContext WeatherData(
                    temperature = 0f,
                    condition = WeatherCondition.CLEAR,
                    rateLimited = true
                )
            }
            if (responseCode >= 400) {
                Log.e("OpenMeteoAdapter", "HTTP $responseCode from Open-Meteo")
                connection.disconnect()
                return@withContext null
            }

            val json = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val temperature = extractJsonFloat(json, "temperature") ?: return@withContext null
            val weatherCode = extractJsonInt(json, "weathercode") ?: 0
            val condition = wmoCodeToCondition(weatherCode)

            // Hourly arrays run 00:00..23:00 local, so the array index == local hour.
            val currentHour = extractCurrentHour(json)

            // Extract +1h forecast condition from hourly.weather_code array
            val forecastCondition = currentHour?.let { extractHourlyWeatherCode(json, it + 1) }
                ?.let { wmoCodeToCondition(it) }

            // Day's max temperature, and whether the hour it occurs is still ahead.
            val maxTemperature = extractFirstArrayFloat(json, "temperature_2m_max")
            val hourlyTemps = extractFloatArray(json, "temperature_2m")
            val maxHour = hourlyTemps.indices.maxByOrNull { hourlyTemps[it] }
            val maxTempAhead = currentHour != null && maxHour != null && maxHour > currentHour

            WeatherData(
                temperature = temperature,
                condition = condition,
                forecastCondition = forecastCondition,
                maxTemperature = maxTemperature,
                maxTempAhead = maxTempAhead
            )
        } catch (e: Exception) {
            Log.e("OpenMeteoAdapter", "Failed to fetch weather", e)
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

    /** Local hour (0..23) from current_weather.time, e.g. "2026-06-24T14:00" -> 14. */
    private fun extractCurrentHour(json: String): Int? {
        val pattern = """"current_weather"\s*:\s*\{[^}]*?"time"\s*:\s*"([^"]+)"""".toRegex()
        val time = pattern.find(json)?.groupValues?.get(1) ?: return null
        return time.substringAfter('T', "").take(2).toIntOrNull()
    }

    /** All numbers of a JSON array property, e.g. hourly "temperature_2m". */
    private fun extractFloatArray(json: String, key: String): List<Float> {
        val pattern = """"$key"\s*:\s*\[([^\]]+)]""".toRegex()
        val match = pattern.find(json) ?: return emptyList()
        return match.groupValues[1].split(",").mapNotNull { it.trim().toFloatOrNull() }
    }

    /** First number of a JSON array property, e.g. daily "temperature_2m_max". */
    private fun extractFirstArrayFloat(json: String, key: String): Float? =
        extractFloatArray(json, key).firstOrNull()
}
