package com.herrderb.launcherli.data.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * MeteoSwiss adapter using SwissMetNet open data for current conditions
 * and Open-Meteo MeteoSwiss ICON for +1h forecast.
 */
class MeteoSwissAdapter : WeatherAdapter {

    override val id = "meteoswiss"
    override val displayName = "MeteoSwiss"

    override suspend fun fetchWeather(config: WeatherConfig): WeatherData? = withContext(Dispatchers.IO) {
        try {
            val station = config.stationId.ifBlank { "sma" }.lowercase()
            val url = URL("https://data.geo.admin.ch/ch.meteoschweiz.ogd-smn/$station/ogd-smn_${station}_t_now.csv")
            val connection = url.openConnection() as HttpURLConnection
            connection.useCaches = false
            connection.setRequestProperty("User-Agent", "Launcherli/1.0")
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val csv = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val lines = csv.trim().lines()
            if (lines.size < 2) return@withContext null

            val header = lines[0].split(";")
            // Pick last row that has a non-empty temperature value
            val tempIdx = header.indexOf("tre200s0")
            val cloudIdx = header.indexOf("wcc006s0")
            val rainIdx = header.indexOf("rre150z0")
            if (tempIdx < 0) return@withContext null

            val lastLine = lines.asReversed().drop(0).firstOrNull { row ->
                val cols = row.split(";")
                cols.getOrNull(tempIdx)?.toFloatOrNull() != null
            }?.split(";") ?: return@withContext null

            val temperature = lastLine.getOrNull(tempIdx)?.toFloatOrNull() ?: return@withContext null
            val cloudCover = lastLine.getOrNull(cloudIdx)?.toFloatOrNull() ?: 0f
            val rain = lastLine.getOrNull(rainIdx)?.toFloatOrNull() ?: 0f

            val condition = when {
                rain > 0.5f && temperature < 1f -> WeatherCondition.SNOWY
                rain > 0.5f -> WeatherCondition.RAINY
                cloudCover >= 6f -> WeatherCondition.CLOUDY
                else -> WeatherCondition.CLEAR
            }

            // Fetch +1h forecast from Open-Meteo MeteoSwiss ICON
            val forecastCondition = fetchForecastCondition(config)

            WeatherData(temperature = temperature, condition = condition, forecastCondition = forecastCondition)
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchForecastCondition(config: WeatherConfig): WeatherCondition? {
        return try {
            val lat = config.latitude
            val lon = config.longitude
            if (lat == 0.0 && lon == 0.0) return null

            val url = URL(
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&hourly=weather_code&forecast_hours=2&models=icon_seamless"
            )
            val connection = url.openConnection() as HttpURLConnection
            connection.useCaches = false
            connection.setRequestProperty("User-Agent", "Launcherli/1.0")
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val json = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val pattern = """"weather_code"\s*:\s*\[([^\]]+)]""".toRegex()
            val match = pattern.find(json) ?: return null
            val values = match.groupValues[1].split(",").map { it.trim() }
            val code = values.getOrNull(1)?.toIntOrNull() ?: return null
            wmoCodeToCondition(code)
        } catch (e: Exception) {
            null
        }
    }

    private fun wmoCodeToCondition(code: Int): WeatherCondition = when (code) {
        0, 1 -> WeatherCondition.CLEAR
        2, 3 -> WeatherCondition.CLOUDY
        45, 48 -> WeatherCondition.CLOUDY
        51, 53, 55 -> WeatherCondition.RAINY
        56, 57 -> WeatherCondition.RAINY
        61, 63, 65 -> WeatherCondition.RAINY
        66, 67 -> WeatherCondition.RAINY
        71, 73, 75 -> WeatherCondition.SNOWY
        77 -> WeatherCondition.SNOWY
        80, 81, 82 -> WeatherCondition.RAINY
        85, 86 -> WeatherCondition.SNOWY
        95, 96, 99 -> WeatherCondition.RAINY
        else -> WeatherCondition.CLOUDY
    }
}
