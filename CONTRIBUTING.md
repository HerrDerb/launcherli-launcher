# Contributing to Launcherli Launcher

Thanks for your interest in contributing! Here's how to get started.

## Getting Started

1. Fork the repository
2. Clone your fork locally
3. Open the project in Android Studio (Ladybug or newer)
4. Run `./gradlew assembleDebug` to verify the build works

## Development Setup

- **Kotlin** 2.0.21
- **Compose BOM** 2024.10.00
- **compileSdk** 35
- **minSdk** 29
- **Gradle** 8.9

## Making Changes

1. Create a feature branch from `main`
2. Make your changes
3. Ensure the build passes: `./gradlew assembleDebug`
4. Commit with a clear message describing what and why
5. Open a pull request

## Guidelines

- Keep it minimal — this launcher's philosophy is "less is more"
- No third-party network libraries (we use `HttpURLConnection`)
- No third-party UI libraries beyond AndroidX/Compose
- Follow existing code style (no heavy comments, clean Kotlin)
- Test on a real device if possible

## Reporting Issues

- Use GitHub Issues
- Include Android version, device model, and steps to reproduce
- Screenshots or screen recordings are welcome

## Code of Conduct

Be kind, be constructive. We're all here to build something clean and useful.
