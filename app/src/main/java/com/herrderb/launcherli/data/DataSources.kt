package com.herrderb.launcherli.data

/**
 * External data shown by the widgets, with the attribution shown in Settings.
 * Open-Meteo: https://open-meteo.com/en/terms (CC BY 4.0, attribution required).
 * FOEN: https://www.bafu.admin.ch/dam/de/sd-web/g7vjiKP5LJ11/liefer-nutzungsbedingungen-hydrologische-daten.pdf
 * (attribution recommended).
 */
internal data class DataSource(
    val purpose: String,
    val provider: String,
    val licence: String?,
    val url: String
) {
    val attribution: String
        get() = "$purpose: $provider" + (licence?.let { " ($it)" } ?: "")
}

internal val DATA_SOURCES = listOf(
    DataSource("Weather", "Open-Meteo.com", "CC BY 4.0", "https://open-meteo.com"),
    DataSource(
        "Water temperature",
        "Swiss Federal Office for the Environment (FOEN)",
        licence = null,
        url = "https://www.hydrodaten.admin.ch"
    )
)
