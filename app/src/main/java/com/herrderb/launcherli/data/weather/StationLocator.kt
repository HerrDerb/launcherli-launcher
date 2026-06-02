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
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Resolves the device location into a [LocationInfo] suitable for picking a weather source.
 *
 * - For Switzerland, finds the nearest MeteoSwiss SwissMetNet station.
 * - For other countries, reverse-geocodes the city name for use with Open-Meteo.
 */
class StationLocator(private val context: Context) {

    private data class Station(val id: String, val lat: Double, val lon: Double, val name: String)

    suspend fun getLocation(): LocationInfo = withContext(Dispatchers.IO) {
        val coords = getLastKnownLocation() ?: return@withContext UnavailableLocation
        val (lat, lon) = coords

        val (inSwitzerland, cityName) = resolveAddress(lat, lon)

        if (inSwitzerland) {
            val nearest = fetchStationList()?.minByOrNull { haversineDistance(lat, lon, it.lat, it.lon) }
            SwissLocation(
                latitude = lat,
                longitude = lon,
                nearestStationId = nearest?.id ?: DEFAULT_STATION_ID,
                nearestStationName = nearest?.name ?: DEFAULT_STATION_ID.uppercase()
            )
        } else {
            InternationalLocation(
                latitude = lat,
                longitude = lon,
                cityName = cityName.orEmpty()
            )
        }
    }

    private fun resolveAddress(lat: Double, lon: Double): Pair<Boolean, String?> {
        return try {
            @Suppress("DEPRECATION")
            val addr = Geocoder(context).getFromLocation(lat, lon, 1)?.firstOrNull()
            val inSwitzerland = addr?.countryCode == "CH"
            val city = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea
            Pair(inSwitzerland, if (!inSwitzerland) city else null)
        } catch (_: Exception) {
            Pair(lat in 45.8..47.8 && lon in 5.9..10.5, null)
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

    @Volatile private var stationListCache: List<Station>? = null

    private fun fetchStationList(): List<Station>? {
        stationListCache?.let { return it }
        return synchronized(this) {
            stationListCache?.let { return it }
            try {
                val url = URL("https://data.geo.admin.ch/api/stac/v1/collections/ch.meteoschweiz.ogd-smn/items?limit=200")
                val connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "Launcherli/1.0")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val json = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                parseStations(json).also { stationListCache = it }
            } catch (_: Exception) {
                null
            }
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
            val title = """"title"\s*:\s*"([^"]+)"""".toRegex().find(block)?.groupValues?.get(1)

            stations.add(Station(id = id, lat = lat, lon = lon, name = title ?: id.uppercase()))
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

    companion object {
        private const val DEFAULT_STATION_ID = "sma"
    }
}
