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

## [0.2.0] - 2025-11-19

### Added
- **Backend (SvelteKit)**:
    - User Management:
        - Added `users` table to database schema with username, password hash, role, and timestamps.
        - Created User Management UI (`/users`) for creating and deleting users.
        - Implemented user creation with role selection (child, parent, admin).
    - Authentication API:
        - Login endpoint (`/api/auth/login`) with credential validation.
        - Logout endpoint (`/api/auth/logout`) for session termination.
    - Telemetry System:
        - Added `telemetry` table to track user events (LOGIN, LOGOUT).
        - Records userId, deviceId, event type, UTC timestamp, and additional data.
        - Login and logout events automatically logged with device information.
    - Enhanced Admin Dashboard:
        - New sidebar navigation layout with Tailwind CSS.
        - Dashboard overview page with statistics.
        - Upgraded device listing to responsive table with status badges.
- **Android Client**:
    - Login Screen:
        - Created `LoginScreen` composable with username/password fields.
        - Integrated with backend login API.
        - Displays login status and error messages.
    - User Session Management:
        - Stores logged-in user data (id, username, role).
        - Displays user info and device name after login.
        - Logout functionality with telemetry tracking.
    - Telemetry Integration:
        - Sends device ID (Build.MODEL) with login requests.
        - Calls logout API on user logout action.

### Fixed
- **Backend**:
    - Fixed Svelte compilation error with invalid onclick handler syntax.
    - Corrected telemetry table column names (`created_at` vs `createdAt`).
    - Fixed login API to properly record telemetry events.
- **Android**:
    - Added missing imports for Compose runtime and coroutines.
    - Updated API service to include deviceId in login requests.

### Changed
- **UI/UX**:
    - Replaced default SvelteKit welcome page with custom dashboard.
    - Enhanced device listing with modern card-based design.
    - Added user avatar placeholders in user management UI.
