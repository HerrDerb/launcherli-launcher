package com.herrderb.launcherli.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pins the attribution shown in Settings: required by Open-Meteo (CC BY 4.0), recommended by FOEN. */
class DataSourcesTest {

    @Test
    fun attributesBothProviders() {
        assertEquals(
            listOf(
                "Weather: Open-Meteo.com (CC BY 4.0)",
                "Water temperature: Swiss Federal Office for the Environment (FOEN)"
            ),
            DATA_SOURCES.map { it.attribution }
        )
    }

    @Test
    fun everySourceLinksToItsProvider() {
        assertEquals(
            listOf("https://open-meteo.com", "https://www.hydrodaten.admin.ch"),
            DATA_SOURCES.map { it.url }
        )
    }
}
