# Changelog

All notable changes to this project will be documented in this file.

## [0.8.0] - 2025-11-20

### Changed
- **Android Launcher**:
    - Renamed app namespace and components to `edu.aiims.surveylauncher` (matches `applicationId` and DO/home components).
    - Applied a cohesive dark/glassy Material3 theme with brighter headings for readability across registration, login, PIN, settings, and app drawer.

### Added
- **Android Launcher**:
    - Debug-only registration bypass: entering code `1234` in debug builds skips the server call and stores a local debug device registration for faster testing.

## [0.7.0] - 2025-11-20

### Added
- **Android Launcher**:
    - Default multi-app kiosk allow-list when no remote policy is present (launcher, ODK, WhatsApp/Business, dialer/messaging variants, Gmail, Firefox, Chrome, REDCap, Settings); auto-allow any installed `edu.aiims.*` apps.
    - Launcher app grid now strictly filters to the allow-list (no show-all fallback).

### Changed
- **Android Launcher**:
    - Apply lock task packages before entering kiosk mode to honor the allow-list.
    - Whitelisted Settings (AOSP + Google) for supervised access under kiosk.

## [0.6.0] - 2025-11-19

### Changed
- **Backend (SvelteKit)**:
    - Changed `devices` table primary key from UUID string to Auto-Increment Integer for shorter, more readable Device IDs (e.g., "1", "1001").
    - Updated registration API to return the generated integer ID.
- **Android Client**:
    - Updated `RegistrationScreen` to handle the new integer Device ID format.
    - Added a "Reset Registration (Debug)" button in Settings to clear local registration data for testing.

### Added
- **Backend**:
    - Added `seed.ts` script to seed the database with a default admin user (`admin` / `password`).

## [0.5.0] - 2025-11-19

### Added
- **Backend (SvelteKit)**:
    - Implemented a new secure device registration flow using temporary 5-digit codes.
    - Added a `/api/devices/register/generate-code` endpoint to create time-sensitive registration codes.
    - Added the ability for administrators to add a descriptive name to devices upon registration.
    - Implemented an automatic cleanup mechanism to purge expired registration tokens.

### Changed
- **Backend (SvelteKit)**:
    - The `/api/devices/register` endpoint now requires a valid 5-digit code.
- **UI/UX (Admin Dashboard)**:
    - Replaced the "Add Device" button with a modal-based registration flow.
    - The new modal displays the registration code and an expiration countdown timer.

### Fixed
- **Admin Dashboard**:
    - Resolved multiple Svelte 5 compilation errors related to props (`$props()`).
    - Addressed accessibility warnings in the Modal component.
    - Resolved a TypeScript error by installing type definitions for `better-sqlite3`.

## [0.4.0] - 2025-11-19

### Added
- **Android Client**:
    - Implemented offline‑first telemetry storage using Room SQLite.
    - Added `TelemetryEntity`, `TelemetryDao`, and `AppDatabase`.
    - Updated `TelemetryWorker` to save events locally and sync when network is available.
    - Fixed JSON deserialization to handle both object and array payloads.
- **Backend (SvelteKit)**:
    - No code changes required; existing `/api/telemetry` endpoint now receives batched events after sync.

### Fixed
- Resolved `JsonSyntaxException` caused by mismatched JSON structures.
- Improved logging for telemetry sync success/failure.

### Changed
- Updated documentation and README to reflect offline telemetry capabilities.


## [0.3.0] - 2025-11-19

### Added
- **Backend (SvelteKit)**:
    - Telemetry API:
        - Generic `/api/telemetry` endpoint for batch event ingestion.
        - Supports `LOCATION` and `APP_USAGE` event types.
    - Telemetry Dashboard:
        - New `/telemetry` page to view real-time device events.
        - Displays event type, user/device, timestamp, and raw data.
- **Android Client**:
    - Background Data Collection:
        - Implemented `TelemetryWorker` using WorkManager (runs every 15 min).
        - Collects GPS Location (Latitude, Longitude, Accuracy).
        - Collects App Usage Stats (Top 5 apps by usage time).
    - Permissions & Privacy:
        - Added `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`.
        - Added `PACKAGE_USAGE_STATS` permission logic.
        - UI prompts user to grant Usage Access in system settings.
    - Session Management:
        - Created `SessionManager` to persist user session and device ID.
        - Auto-schedules telemetry worker upon login.

### Changed
- **Android**:
    - `MainActivity` now checks for Usage Stats permission and shows a "Grant" button if missing.
    - Added "Send Telemetry Now" button for easier testing.
- **Backend**:
    - Replaced default SvelteKit welcome page with custom dashboard.
    - Enhanced device listing with modern card-based design.
    - Added user avatar placeholders in user management UI.

### Fixed
- **Backend**:
    - Fixed Svelte compilation error with invalid onclick handler syntax.
    - Corrected telemetry table column names (`created_at` vs `createdAt`).
    - Fixed login API to properly record telemetry events.
- **Android**:
    - Added missing imports for Compose runtime and coroutines.
    - Updated API service to include deviceId in login requests.


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
