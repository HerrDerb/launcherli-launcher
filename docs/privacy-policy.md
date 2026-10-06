---
title: Launcherli Launcher privacy policy
---

# Privacy policy

**Launcherli Launcher** (Android, package `com.herrderb.launcherli`)
Last updated: 6 October 2026

Launcherli is a minimal home screen app. It has no user accounts, no ads, and no analytics or tracking. This policy explains what data the app uses, where it goes, and how to remove it. The source code is public on [GitHub](https://github.com/HerrDerb/launcherli-launcher), so every statement here can be checked.

## Summary

| Data | Used for | Leaves the device? |
|---|---|---|
| Approximate location | Weather and water temperature widgets | Coordinates go to Open-Meteo, and to your device's geocoding service for the city name |
| Calendar link (optional) | Today and tomorrow appointment counts | The app downloads your calendar from the link you entered |
| Contacts (optional) | Contact search in the app drawer | No |
| List of installed apps | App drawer, favorites, "most used" | No |
| Settings and launch counts | Remembering your setup | Only through Android backup, if you have it enabled |

The developer does not receive any of this data. The app has no server of its own.

## Location

With your permission (approximate location only), the app reads the device's last known location while the launcher is open in the foreground, at most every 15 minutes. It does not request location in the background.

- **Weather:** the coordinates are sent to [Open-Meteo](https://open-meteo.com) to get the current weather and today's forecast.
- **City name:** the coordinates are passed to Android's built-in geocoding service to look up the city name shown in the weather widget. On many devices this service is provided by Google Play services, in which case Google processes the coordinates under its own privacy policy.
- **Water temperature (Switzerland only):** the nearest measuring station is calculated on the device, using a station list downloaded from [hydrodaten.admin.ch](https://www.hydrodaten.admin.ch) (Swiss Federal Office for the Environment). Only the station's ID is sent when fetching its temperature, never your coordinates.

The location is kept in memory only and is not saved. Without the location permission, the weather and water temperature widgets stay empty and everything else works.

## Calendar link (optional)

If you enter a calendar link (a public iCalendar `.ics` address from your calendar provider), the app downloads that calendar every 30 minutes while the launcher is in the foreground. It counts today's and tomorrow's appointments on the device, using only their start times and repeat rules. Other details in the file, such as titles, descriptions and attendees, are ignored and never stored.

The link itself is stored on the device encrypted (AES-256-GCM, with a key held in the Android Keystore). It is never shown again after saving. Your calendar provider receives the download requests under its own privacy policy.

## Contacts (optional)

Contact search is off by default. If you turn it on and grant the contacts permission, the app searches your contacts on the device while you type in the drawer search. Contact data is not stored or sent anywhere by the app. If you tap call, SMS or WhatsApp, the phone number is handed to the app you chose, which then handles it.

## Installed apps

To show the app drawer, the app reads the list of installed apps that can be launched (this needs Android's "query all packages" permission). It counts how often you open apps from the drawer, to fill the "most used" section. Both stay on the device.

## Storage and backup

Your settings are stored in the app's private storage on the device: favorites, launch counts, theme and layout options, and the encrypted calendar link. If Android backup is enabled on your device, Android may include these settings in your backup to your Google account. The calendar link stays encrypted in the backup. Its key never leaves the device, so after restoring on a new phone you need to enter the link again.

## Network requests

All requests go directly from your device to the services named above. Like any internet request, they reveal your IP address to the service. The app identifies itself with the user agent `Launcherli/1.0`. No identifiers about you or your device are added.

## Deleting your data

- Remove the calendar link in **Settings** ("Clear").
- Reset the "most used" list in **Settings** ("Reset usage counts").
- Clearing the app's storage in Android settings, or uninstalling the app, deletes all data the app has stored.
- You can revoke the location and contacts permissions at any time in Android settings.

## Children

The app is not directed at children and does not knowingly collect data from them.

## Changes

If this policy changes, the new version is published at this address with a new "Last updated" date. The full history is visible in the [GitHub repository](https://github.com/HerrDerb/launcherli-launcher/commits/main/docs/privacy-policy.md).

## Contact

Questions or requests about privacy: open an issue at [github.com/HerrDerb/launcherli-launcher/issues](https://github.com/HerrDerb/launcherli-launcher/issues).
