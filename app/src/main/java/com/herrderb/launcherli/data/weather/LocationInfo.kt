package com.herrderb.launcherli.data.weather

/**
 * Result of resolving the device location into a weather-source-specific context.
 *
 * - [SwissLocation]: device is in Switzerland → use MeteoSwiss + hydro stations.
 * - [InternationalLocation]: device is outside Switzerland → use Open-Meteo with reverse-geocoded city.
 * - [UnavailableLocation]: no usable location (no permission, no provider, etc.).
 */
sealed interface LocationInfo {
    val latitude: Double
    val longitude: Double
}

data class SwissLocation(
    override val latitude: Double,
    override val longitude: Double,
    val nearestStationId: String,
    val nearestStationName: String
) : LocationInfo

data class InternationalLocation(
    override val latitude: Double,
    override val longitude: Double,
    val cityName: String
) : LocationInfo

data object UnavailableLocation : LocationInfo {
    override val latitude = 0.0
    override val longitude = 0.0
}
