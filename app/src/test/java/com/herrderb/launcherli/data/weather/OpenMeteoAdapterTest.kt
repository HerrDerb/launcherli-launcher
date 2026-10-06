package com.herrderb.launcherli.data.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMeteoAdapterTest {

    private val adapter = OpenMeteoAdapter()

    /** Shaped like a real response: unit objects precede the value objects. */
    private fun response(
        time: String = "2026-10-06T14:00",
        weathercode: Int = 61,
        hourlyCodes: List<Int> = List(24) { if (it == 15) 71 else 0 },
        hourlyTemps: List<Double> = List(24) { if (it == 16) 19.0 else 10.0 }
    ) = """
        {"current_weather_units":{"time":"iso8601","temperature":"°C","weathercode":"wmo code"},
         "current_weather":{"time":"$time","temperature":12.5,"weathercode":$weathercode},
         "hourly_units":{"temperature_2m":"°C","weather_code":"wmo code"},
         "hourly":{"temperature_2m":[${hourlyTemps.joinToString(",")}],"weather_code":[${hourlyCodes.joinToString(",")}]},
         "daily":{"temperature_2m_max":[18.4]}}
    """.trimIndent()

    @Test
    fun parsesCurrentConditions() {
        val data = adapter.parse(response())!!
        assertEquals(12.5f, data.temperature)
        assertEquals(WeatherCondition.RAINY, data.condition)
        assertEquals(18.4f, data.maxTemperature)
    }

    @Test
    fun forecastIsTheNextHour() {
        assertEquals(WeatherCondition.SNOWY, adapter.parse(response())!!.forecastCondition)
    }

    @Test
    fun maxTempAheadWhenPeakHourIsLater() {
        assertTrue(adapter.parse(response(time = "2026-10-06T14:00"))!!.maxTempAhead)
        assertFalse(adapter.parse(response(time = "2026-10-06T17:00"))!!.maxTempAhead)
    }

    @Test
    fun noForecastAtLastHourOfDay() {
        assertNull(adapter.parse(response(time = "2026-10-06T23:00"))!!.forecastCondition)
    }

    @Test
    fun wmoCodesMapToConditions() {
        mapOf(
            0 to WeatherCondition.CLEAR, 3 to WeatherCondition.CLOUDY, 45 to WeatherCondition.CLOUDY,
            55 to WeatherCondition.RAINY, 75 to WeatherCondition.SNOWY, 86 to WeatherCondition.SNOWY,
            95 to WeatherCondition.RAINY, 1234 to WeatherCondition.CLOUDY
        ).forEach { (code, expected) ->
            assertEquals("code $code", expected, adapter.parse(response(weathercode = code))!!.condition)
        }
    }

    @Test
    fun missingTemperatureYieldsNull() {
        assertNull(adapter.parse("""{"current_weather":{"weathercode":0}}"""))
    }
}
