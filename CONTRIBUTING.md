# Contributing to Launcherli Launcher

Thanks for your interest in contributing! Here's how to get started.

## Getting Started

1. Fork the repository
2. Clone your fork locally
3. Open the project in a recent Android Studio that supports AGP 9.3
4. Run `./gradlew assembleDebug testDebugUnitTest` to verify the build works

## Development Setup

- **JDK** 17
- **Kotlin** 2.3.21
- **Android Gradle Plugin** 9.3.1
- **Gradle** 9.6.1
- **Compose BOM** 2026.05.01
- **compileSdk** 36, **targetSdk** 35, **minSdk** 29

Exact versions live in `build.gradle`, `app/build.gradle` and the Gradle wrapper; Dependabot keeps them current.

## Making Changes

1. Create a feature branch from `main`
2. Make your changes
3. Add or update unit tests under `app/src/test` for logic changes
4. Ensure build and tests pass: `./gradlew assembleDebug testDebugUnitTest`
5. Commit with a clear message describing what and why
6. Open a pull request

## Guidelines

- Keep it minimal — this launcher's philosophy is "less is more"
- No third-party network libraries (we use `HttpURLConnection`)
- No third-party UI libraries beyond AndroidX/Compose
- No analytics or tracking SDKs
- Follow existing code style (no heavy comments, clean Kotlin)
- Test on a real device if possible

## Reporting Issues

- Use GitHub Issues
- Include Android version, device model, and steps to reproduce
- Screenshots or screen recordings are welcome

## Code of Conduct

Be kind, be constructive. We're all here to build something clean and useful.
