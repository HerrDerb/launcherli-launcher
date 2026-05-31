# Launcherli Launcher

A minimalist Android launcher built with Kotlin and Jetpack Compose. Clean, precise, no bloat.

## Features

- **Digital clock** with date and next alarm indicator
- **Weather widget** — MeteoSwiss (Switzerland) or Open-Meteo (international) with +1h forecast trend
- **Hydro widget** — nearest water temperature station via hydrodaten.admin.ch (Switzerland only)
- **Favorite apps** — text-only list
- **App drawer** — swipe left to open
- **Dark/Light/System theme**
- **No icons on home screen** — plain, typographic design
- **Settings** — text size, alignment, weather apps, drawer icons

## Screenshots

_Coming soon_

## Requirements

- Android 10+ (API 29)
- Location permission (for weather station selection)

## Build

```bash
./gradlew assembleDebug
```

## Architecture

- **Kotlin** + **Jetpack Compose** (Material 3)
- **DataStore** for preferences
- **MVVM** with `StateFlow`
- No third-party dependencies beyond AndroidX

## Data Sources

| Widget | Source | Region |
|--------|--------|--------|
| Weather (current) | [MeteoSwiss Open Data](https://data.geo.admin.ch) | Switzerland |
| Weather (current) | [Open-Meteo](https://open-meteo.com) | International |
| Weather (forecast) | [Open-Meteo](https://open-meteo.com) | Both |
| Hydro temperature | [hydrodaten.admin.ch](https://www.hydrodaten.admin.ch) | Switzerland |

## License

[MIT](LICENSE)
