package com.herrderb.launcherli.data.weather

/**
 * Registry of available weather adapters.
 * Add new adapters here to make them selectable in settings.
 */
open class WeatherAdapterRegistry {

    private val adapters = mutableMapOf<String, WeatherAdapter>()

    init {
        register(MeteoSwissAdapter())
        register(OpenMeteoAdapter())
    }

    fun register(adapter: WeatherAdapter) {
        adapters[adapter.id] = adapter
    }

    open fun getAdapter(id: String): WeatherAdapter? = adapters[id]

    fun getDefault(): WeatherAdapter = adapters.values.first()

    fun allAdapters(): List<WeatherAdapter> = adapters.values.toList()

    companion object : WeatherAdapterRegistry()
}
