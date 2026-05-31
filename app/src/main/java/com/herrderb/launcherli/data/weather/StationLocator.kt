package com.herrderb.launcherli.data.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val isInSwitzerland: Boolean,
    val nearestStationId: String?
)

/**
 * Finds the nearest MeteoSwiss SwissMetNet station based on device location.
 * Also determines whether the device is in Switzerland.
 */
class StationLocator(private val context: Context) {

    data class Station(val id: String, val lat: Double, val lon: Double, val name: String)

    /**
     * Get location info: coordinates, whether in Switzerland, and nearest station if applicable.
     */
    suspend fun getLocationResult(): LocationResult = withContext(Dispatchers.IO) {
        val location = getLastKnownLocation()
            ?: return@withContext LocationResult(0.0, 0.0, false, null)

        val inSwitzerland = checkIfInSwitzerland(location.first, location.second)

        val stationId = if (inSwitzerland) {
            val stations = fetchStationList()
            stations?.minByOrNull { station ->
                haversineDistance(location.first, location.second, station.lat, station.lon)
            }?.id ?: "sma"
        } else null

        LocationResult(
            latitude = location.first,
            longitude = location.second,
            isInSwitzerland = inSwitzerland,
            nearestStationId = stationId
        )
    }

    @Deprecated("Use getLocationResult() instead")
    suspend fun getNearestStationId(): String {
        val result = getLocationResult()
        return result.nearestStationId ?: "sma"
    }

    private fun checkIfInSwitzerland(lat: Double, lon: Double): Boolean {
        return try {
            @Suppress("DEPRECATION")
            val addresses = Geocoder(context).getFromLocation(lat, lon, 1)
            addresses?.firstOrNull()?.countryCode == "CH"
        } catch (_: Exception) {
            // Fallback to bounding box
            lat in 45.8..47.8 && lon in 5.9..10.5
        }
    }

    private fun getLastKnownLocation(): Pair<Double, Double>? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val providers = listOf(
            LocationManager.FUSED_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        )

        for (provider in providers) {
            try {
                @Suppress("DEPRECATION")
                val loc = locationManager.getLastKnownLocation(provider)
                if (loc != null) return loc.latitude to loc.longitude
            } catch (_: Exception) { }
        }
        return null
    }

    private fun fetchStationList(): List<Station>? {
        return try {
            val url = URL("https://data.geo.admin.ch/api/stac/v1/collections/ch.meteoschweiz.ogd-smn/items?limit=200")
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Launcherli/1.0")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val json = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            parseStations(json)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseStations(json: String): List<Station> {
        val stations = mutableListOf<Station>()
        val featureBlocks = json.split(""""type":"Feature"""")
        for (block in featureBlocks) {
            val idMatch = """"id"\s*:\s*"([^"]+)"""".toRegex().find(block) ?: continue
            val coordMatch = """"coordinates"\s*:\s*\[\s*([\d.]+)\s*,\s*([\d.]+)\s*\]""".toRegex().find(block) ?: continue

            val id = idMatch.groupValues[1]
            val lon = coordMatch.groupValues[1].toDoubleOrNull() ?: continue
            val lat = coordMatch.groupValues[2].toDoubleOrNull() ?: continue

            stations.add(Station(id = id, lat = lat, lon = lon, name = id.uppercase()))
        }
        return stations
    }

    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * asin(sqrt(a))
    }
}
