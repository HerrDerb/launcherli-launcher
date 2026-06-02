package com.herrderb.launcherli.data.weather


/**
 * Result of resolving the device location into a weather-source-specific context.
 *
*/
data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val inSwitzerland: Boolean,
    val cityName: String
)
