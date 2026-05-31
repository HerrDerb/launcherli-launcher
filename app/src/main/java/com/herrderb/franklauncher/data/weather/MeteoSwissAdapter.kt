package com.herrderb.franklauncher.data.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * MeteoSwiss adapter using SwissMetNet open data.
 * Fetches hourly "now" CSV for a given station ID.
 * Default station: "sma" (Zürich / Fluntern).
 */
class MeteoSwissAdapter : WeatherAdapter {

    override val id = "meteoswiss"
    override val displayName = "MeteoSwiss"

    override suspend fun fetchWeather(config: WeatherConfig): WeatherData? = withContext(Dispatchers.IO) {
        try {
            val station = config.stationId.ifBlank { "sma" }.lowercase()
            val url = URL("https://data.geo.admin.ch/ch.meteoschweiz.ogd-smn/$station/ogd-smn_${station}_h_now.csv")
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "FrankLauncher/1.0")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val csv = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val lines = csv.trim().lines()
            if (lines.size < 2) return@withContext null

            val header = lines[0].split(";")
            val lastLine = lines.last().split(";")

            val tempIdx = header.indexOf("tre200h0")
            val cloudIdx = header.indexOf("wcc006h0")
            val rainIdx = header.indexOf("rre150h0")

            val temperature = lastLine.getOrNull(tempIdx)?.toFloatOrNull() ?: return@withContext null
            val cloudCover = lastLine.getOrNull(cloudIdx)?.toFloatOrNull() ?: 0f
            val rain = lastLine.getOrNull(rainIdx)?.toFloatOrNull() ?: 0f

            val condition = when {
                rain > 0.5f && temperature < 1f -> WeatherCondition.SNOWY
                rain > 0.5f -> WeatherCondition.RAINY
                cloudCover >= 6f -> WeatherCondition.CLOUDY
                else -> WeatherCondition.CLEAR
            }

            WeatherData(temperature = temperature, condition = condition)
        } catch (e: Exception) {
            null
        }
    }
}
