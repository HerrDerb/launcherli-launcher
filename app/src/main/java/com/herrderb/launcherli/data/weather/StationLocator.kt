package com.herrderb.launcherli.data.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Resolves the device location into a [LocationInfo]: coordinates for Open-Meteo,
 * a reverse-geocoded city name for the weather label, and whether the device is in
 * Switzerland (which gates the hydro widget).
 */
class StationLocator(private val context: Context) {

    // Last resolved location. Reverse-geocoding is comparatively expensive, so we
    // reuse the previous result while the device hasn't meaningfully moved (~1 km),
    // and fall back to it if no fresh fix is available.
    @Volatile private var cached: LocationInfo? = null

    suspend fun getLocation(): LocationInfo? = withContext(Dispatchers.IO) {
        val coords = getLastKnownLocation() ?: return@withContext cached
        val (lat, lon) = coords

        cached?.let { previous ->
            if (abs(lat - previous.latitude) < 0.01 && abs(lon - previous.longitude) < 0.01) {
                return@withContext previous
            }
        }

        val (inSwitzerland, cityName) = resolveAddress(lat, lon)
        LocationInfo(
            latitude = lat,
            longitude = lon,
            inSwitzerland = inSwitzerland,
            cityName = cityName.orEmpty()
        ).also { cached = it }
    }

    private fun resolveAddress(lat: Double, lon: Double): Pair<Boolean, String?> {
        return try {
            @Suppress("DEPRECATION")
            val addr = Geocoder(context).getFromLocation(lat, lon, 1)?.firstOrNull()
            val inSwitzerland = addr?.countryCode == "CH"
            val city = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea
            Pair(inSwitzerland, city)
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
}
