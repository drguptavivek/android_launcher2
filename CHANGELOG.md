# Changelog

All notable changes to this project will be documented in this file.

## [0.1.0] - 2025-11-19

### Added
- **Backend (SvelteKit)**:
    - Initial project setup with SvelteKit and SQLite (Drizzle ORM).
    - Device Registration API endpoint (`/api/devices/register`).
    - Admin Dashboard to view registered devices.
- **Android Client**:
    - Basic Launcher setup with Jetpack Compose.
    - Device Registration UI (`RegistrationScreen`).
    - Retrofit API client for backend communication.
    - Support for Android SDK 36 (Preview).
- **Integration**:
    - Verified end-to-end device registration flow.
    - Configured `adb reverse` for local emulator connectivity.

### Fixed
- **Android Build**:
    - Resolved build failures by updating to Gradle 9.2.1 and AGP 8.13.1.
    - Fixed missing Android resources (strings, themes).
    - Enabled `android.useAndroidX=true`.
    - Suppressed `UnstableApiUsage` warnings in `settings.gradle.kts`.
- **Connectivity**:
    - Enabled `android:usesCleartextTraffic="true"` to allow local HTTP connections.
    - Updated API client to use `localhost` instead of `10.0.2.2` for better compatibility with `adb reverse`.
