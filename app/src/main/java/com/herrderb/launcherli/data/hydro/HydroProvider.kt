package com.herrderb.launcherli.data.hydro

import android.content.Context
import android.util.Log
import com.posthog.PostHog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

class HydroProvider(private val context: Context) {

    companion object {
        private const val GEOJSON_URL =
            "https://www.hydrodaten.admin.ch/web-hydro-maps/hydro_sensor_temperature.geojson"
        private const val STATION_DATA_URL =
            "https://www.hydrodaten.admin.ch/plots/temperature_7days/%s_temperature_7days_de.json"
        private const val STATION_BASE_URL =
            "https://www.hydrodaten.admin.ch/de/seen-und-fluesse/stationen-und-daten/"
        private const val CACHE_FILE = "hydro_stations.geojson"
        private const val CACHE_MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
    }

    suspend fun fetchNearestStation(latitude: Double, longitude: Double): HydroData? =
        withContext(Dispatchers.IO) {
            try {
                val station = findNearestStation(latitude, longitude) ?: return@withContext null
                val temperature = fetchCurrentTemperature(station.first) ?: return@withContext null

                HydroData(
                    stationKey = station.first,
                    stationLabel = station.second,
                    temperature = temperature,
                    url = "$STATION_BASE_URL${station.first}"
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    // (lat, lon) the station was resolved for, plus the (key, label). The nearest
    // station never changes for a fixed location, so we avoid re-reading and
    // re-parsing the whole GeoJSON on every refresh.
    @Volatile private var cachedStation: Triple<Double, Double, Pair<String, String>>? = null

    private fun findNearestStation(latitude: Double, longitude: Double): Pair<String, String>? {
        cachedStation?.let { (cachedLat, cachedLon, station) ->
            if (abs(latitude - cachedLat) < 0.01 && abs(longitude - cachedLon) < 0.01) {
                return station
            }
        }

        val json = getGeoJson() ?: return null
        val geoJson = JSONObject(json)
        val features = geoJson.getJSONArray("features")

        val userLv95 = wgs84ToLv95(latitude, longitude)

        var nearestKey: String? = null
        var nearestLabel: String? = null
        var nearestDist = Double.MAX_VALUE

        for (i in 0 until features.length()) {
            val feature = features.getJSONObject(i)
            val geometry = feature.getJSONObject("geometry")
            val coords = geometry.getJSONArray("coordinates")
            val easting = coords.getDouble(0)
            val northing = coords.getDouble(1)

            val dist = sqrt(
                (easting - userLv95.first).pow(2) +
                    (northing - userLv95.second).pow(2)
            )

            if (dist < nearestDist) {
                nearestDist = dist
                val props = feature.getJSONObject("properties")
                nearestKey = props.getString("key")
                nearestLabel = props.getString("label")
            }
        }

        return if (nearestKey != null) {
            val result = Pair(nearestKey, nearestLabel ?: nearestKey)
            cachedStation = Triple(latitude, longitude, result)
            result
        } else null
    }

    private fun fetchCurrentTemperature(stationKey: String): Double? {
        return try {
            val url = String.format(STATION_DATA_URL, stationKey)
            PostHog.capture(event = "hydro_fetch")
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.useCaches = false
            conn.setRequestProperty("User-Agent", "Launcherli/1.0")
            conn.setRequestProperty("Cache-Control", "no-cache")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val responseCode = conn.responseCode
            if (responseCode >= 400) {
                Log.e("HydroProvider", "HTTP $responseCode fetching temperature for station $stationKey")
                conn.disconnect()
                return null
            }

            val json = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val plotJson = JSONObject(json)
            val plot = plotJson.getJSONObject("plot")
            val data = plot.getJSONArray("data")

            // First trace contains the temperature values
            val trace = data.getJSONObject(0)
            val yValues = trace.getJSONArray("y")

            // Last non-null value is the most recent measurement
            var lastTemp: Double? = null
            for (i in yValues.length() - 1 downTo 0) {
                if (!yValues.isNull(i)) {
                    lastTemp = yValues.getDouble(i)
                    break
                }
            }
            lastTemp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getGeoJson(): String? {
        val cacheFile = File(context.cacheDir, CACHE_FILE)

        // Use cache if fresh enough
        if (cacheFile.exists() && System.currentTimeMillis() - cacheFile.lastModified() < CACHE_MAX_AGE_MS) {
            return cacheFile.readText()
        }

        // Fetch fresh data
        return try {
            val conn = URL(GEOJSON_URL).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Launcherli/1.0")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val responseCode = conn.responseCode
            if (responseCode >= 400) {
                Log.e("HydroProvider", "HTTP $responseCode fetching hydro GeoJSON")
                conn.disconnect()
                throw Exception("HTTP $responseCode")
            }

            val json = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            // Cache it
            cacheFile.writeText(json)
            json
        } catch (e: Exception) {
            // Fall back to stale cache if available
            if (cacheFile.exists()) cacheFile.readText() else null
        }
    }

    /**
     * Approximate WGS84 (lat/lon) to Swiss LV95 (E/N) conversion.
     * Based on swisstopo formulas.
     */
    private fun wgs84ToLv95(lat: Double, lon: Double): Pair<Double, Double> {
        val latSec = lat * 3600.0
        val lonSec = lon * 3600.0

        val latAux = (latSec - 169028.66) / 10000.0
        val lonAux = (lonSec - 26782.5) / 10000.0

        val easting = 2600072.37 +
            211455.93 * lonAux -
            10938.51 * lonAux * latAux -
            0.36 * lonAux * latAux.pow(2) -
            44.54 * lonAux.pow(3)

        val northing = 1200147.07 +
            308807.95 * latAux +
            3745.25 * lonAux.pow(2) +
            76.63 * latAux.pow(2) -
            194.56 * lonAux.pow(2) * latAux +
            119.79 * latAux.pow(3)

        return Pair(easting, northing)
    }
}
