# Launcherli Launcher

A minimalist Android launcher built with Kotlin and Jetpack Compose. Clean, precise, no bloat.

## Features

- **Digital clock** with date and next-alarm indicator; tap to open the system alarm app
- **Weather widget** — current conditions + temperature from Open-Meteo, with a +1h forecast trend arrow; tap opens MeteoSwiss
- **Hydro widget** — nearest water-temperature station via hydrodaten.admin.ch (Switzerland only)
- **Calendar counts** — today/tomorrow appointment counts from any public iCalendar (`.ics`) link; tap opens the provider's app when recognized (e.g. Proton Calendar)
- **Favorite apps** — text-only list with drag-to-reorder and swipe-to-remove
- **App drawer** — swipe left to open, with search and a **Most used** section
- **Dark / Light / System theme**
- **No icons on home screen** — plain, typographic design
- **Settings** — text size, alignment, station labels, drawer icons, most-used apps, calendar link

## Privacy

- The calendar link is stored **encrypted at rest** (AES-256-GCM, key held in the Android Keystore) and never shown again after saving.
- Network refreshes (weather, hydro, calendar) run **only while the launcher is in the foreground** — no background polling.
- No analytics, no third-party dependencies beyond AndroidX.

## Screenshots

_Coming soon_

## Requirements

- Android 10+ (API 29)
- Location permission — for weather/hydro station selection
- Internet — for weather, hydro, and calendar data

## Build

```bash
./gradlew assembleDebug
```

## Architecture

- **Kotlin** + **Jetpack Compose** (Material 3)
- **DataStore** for preferences (calendar link encrypted via Android Keystore)
- **MVVM** with `StateFlow`; periodic refreshes gated to the foreground via `repeatOnLifecycle`
- Pluggable weather sources behind a `WeatherAdapter` interface + registry
- No third-party dependencies beyond AndroidX

## Data Sources

| Widget | Source | Region |
|--------|--------|--------|
| Weather (current + forecast) | [Open-Meteo](https://open-meteo.com) | International |
| Hydro temperature | [hydrodaten.admin.ch](https://www.hydrodaten.admin.ch) | Switzerland |
| Calendar counts | Any public iCalendar (`.ics`) link | Any provider |

## License

[MIT](LICENSE)
