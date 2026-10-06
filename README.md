# Launcherli Launcher

A minimalist Android launcher built with Kotlin and Jetpack Compose. Clean, precise, no bloat.

## Features

- **Digital clock** with date and next-alarm indicator; tap to open the system alarm app
- **Weather widget** — current conditions + temperature from Open-Meteo, with a +1h forecast trend arrow; tap opens MeteoSwiss
- **Hydro widget** — nearest water-temperature station via hydrodaten.admin.ch (Switzerland only)
- **Calendar counts** — today/tomorrow appointment counts from any public iCalendar (`.ics`) link; tap opens the provider's app when recognized (e.g. Proton Calendar)
- **Favorite apps** — text-only list with drag-to-reorder and swipe-to-remove
- **App drawer** — swipe left to open, with search and a **Most used** section (long-press an entry → Clear to drop it); includes work-profile apps (labelled by the system, e.g. "Work Gmail")
- **Contact search**: optional; matching contacts show up in the drawer search with call, SMS, WhatsApp and contact card shortcuts
- **Dark / Light / System theme**
- **No icons on home screen** — plain, typographic design
- **Settings** — text size, alignment, station labels, drawer icons, most-used apps, contact search, calendar link

## Privacy

- The calendar link is stored **encrypted at rest** (AES-256-GCM, key held in the Android Keystore) and never shown again after saving.
- Network refreshes (weather, hydro, calendar) run **only while the launcher is in the foreground** — no background polling.
- Contacts are read on-device only when contact search is enabled; the launcher never sends them anywhere.
- No analytics, no tracking, no third-party runtime dependencies beyond AndroidX.

## Screenshots

_Coming soon_

## Requirements

- Android 10+ (API 29)
- Location permission — for weather/hydro station selection
- Internet — for weather, hydro, and calendar data
- Contacts permission (optional): for contact search in the drawer

## Build

```bash
./gradlew assembleDebug        # build
./gradlew testDebugUnitTest    # JVM unit tests
./gradlew deploy               # install + launch on a connected device
```

## Architecture

- **Kotlin** + **Jetpack Compose** (Material 3)
- **DataStore** for preferences (calendar link encrypted via Android Keystore)
- **MVVM** with `StateFlow`; periodic refreshes gated to the foreground via `repeatOnLifecycle`
- Pluggable weather sources behind a `WeatherAdapter` interface + registry
- JVM unit tests (JUnit 4) for the pure logic: ICS parsing, weather parsing, usage counts, coordinates
- No third-party runtime dependencies beyond AndroidX

## Data Sources

| Widget | Source | Region |
|--------|--------|--------|
| Weather (current + forecast) | [Open-Meteo](https://open-meteo.com) | International |
| Hydro temperature | [hydrodaten.admin.ch](https://www.hydrodaten.admin.ch) | Switzerland |
| Calendar counts | Any public iCalendar (`.ics`) link | Any provider |

## License

[MIT](LICENSE)
