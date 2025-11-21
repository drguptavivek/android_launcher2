# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android Launcher & Admin System combining a custom Android launcher with a SvelteKit admin dashboard for device management, user authentication, and telemetry collection with offline-first architecture.

- All plans when implementing a task will be documented in PLAN.md file giving high level breakdown of things to do in the feature to i=be implemented. No code, just descriptions.

- All notable changes to this project will be documented in CHANGELOG.md file

- All patterns that may cause repeat errors to be documented in ARTEFACTS.md 

## Architecture

### System Components
- **Admin Dashboard** (`admin-dashboard/`): SvelteKit 5 + TypeScript + SQLite/Drizzle ORM
- **Android Launcher** (`android-launcher/`): Kotlin + Jetpack Compose + Room + WorkManager
- **Communication**: REST API with offline-first telemetry sync

### Key Features
- Secure device registration via 5-digit temporary codes (10-min expiration)
- Role-based user authentication (admin/child)
- Offline telemetry collection (GPS, app usage) with background sync
- Real-time device and user management through admin interface

## Development Commands

### Admin Dashboard (SvelteKit)
```bash
cd admin-dashboard
npm install                    # Initial setup
npm run dev                    # Development server (localhost:5173)
npm run build                  # Production build
npm run preview                # Preview production build
npm run check                  # Type + Svelte diagnostics
npm run lint                   # ESLint
npm run test                   # Vitest unit tests
```

### Android Launcher
```bash
cd android-launcher
cd android-launcher
# Prefer a local Gradle cache to avoid permission/network issues: 
GRADLE_USER_HOME=$PWD/.gradle ./gradlew :app:assembleDebug.

./gradlew tasks                # List available tasks
./gradlew assembleDebug
./gradlew :app:assembleDebug   # Build debug APK
./gradlew :app:installDebug    # Install on connected device
./gradlew testDebugUnitTest    # Run unit tests
./gradlew connectedDebugAndroidTest # Instrumentation tests

```


### Common Commands
 - ~/Library/Android/sdk/platform-tools/adb
 - emulator

```bash

cd android-launcher
./gradlew tasks                # List available tasks
./gradlew :app:assembleDebug 

# Make sure no emulator is running
emulator -list-avds
# emulator -avd Medium_Phone_API_36.0 -netdelay none -netspeed full
adb devices       # optional, just to see what’s up
adb emu kill
emulator -avd Medium_Phone_API_36.0 -wipe-data -no-snapshot-load  -no-snapshot-save -no-boot-anim & 
adb wait-for-device
adb shell 'while [[ "$(getprop sys.boot_completed)" != "1" ]]; do sleep 1; done; echo "booted"'
# Reverse proxy for local API testing
adb reverse tcp:5173 tcp:5173 

cd android-launcher

# Installing ODK
adb install -r ODK-Collect-v2025.3.3.apk
adb shell dumpsys package org.odk.collect.android | grep -A 5 "android.intent.action.MAIN" | head -20
# Run ODK in KIOSK Mode adb shell am start -n org.odk.collect.android/.mainmenu.MainMenuActivity

#  - From repo root: 
cd android-launcher
# Prefer a local Gradle cache to avoid permission/network issues: 
GRADLE_USER_HOME=$PWD/.gradle ./gradlew :app:assembleDebug.

# If you need to force a clean rebuild: 
GRADLE_USER_HOME=$PWD/.gradle ./gradlew clean :app:assembleDebug.
# With the wrapper cached, subsequent runs are just 
./gradlew :app:assembleDebug 
# (or assembleRelease for a release build).
# ./gradlew :app:assembleRelease 

./gradlew tasks                # List available tasks
./gradlew :app:assembleDebug 
# Install Launcher
adb install -r app/build/outputs/apk/debug/app-debug.apk
 # Set Device Owner (factory reset device first!)
adb shell dpm set-device-owner edu.aiims.surveylauncher/.admin.LauncherAdminReceiver
adb shell cmd package set-home-activity edu.aiims.surveylauncher/.MainActivity  # set/reset HOME

# FORCE STOP
adb shell am force-stop edu.aiims.surveylauncher 
adb shell am start -n edu.aiims.surveylauncher/.MainActivity 


```


# 4. Checking Logs, stopping etc

```bash
adb logcat -t 100
adb shell am force-stop edu.aiims.surveylauncher && sleep 1 
adb shell am start -n edu.aiims.surveylauncher/.MainActivity && sleep 2 

#  AppDrawer’s discovery logs, showing the last ~15 lines where the drawer lists found launcher apps or allowed apps. 
# Handy for verifying the allow-list filtering after policy sync or debugging why an app isn’t appearing in the drawer without wading through full logs.
adb logcat -d | grep "AppDrawer.*Found\|AppDrawer.*  -" | tail -15
```
 


### Database Operations
```bash
cd admin-dashboard
npx drizzle-kit generate       # Create migration files
npx drizzle-kit push          # Apply schema changes
```

## Database Schema

### Core Tables
- `devices`: Registered devices with model, Android version, description
- `deviceRegistrationTokens`: Temporary 5-digit registration codes with expiration
- `users`: User accounts with roles (admin/child) and Base64 passwords
- `telemetry`: Events (LOGIN, APP_OPEN, LOCATION) with JSON data payload

## API Endpoints

### Device Registration
- `POST /api/devices/register/generate-code` - Create registration code with description
- `POST /api/devices/register` - Register device using valid code

### Authentication
- `POST /api/auth/login` - User login with device association
- `POST /api/auth/logout` - User logout with telemetry tracking

### Telemetry
- `POST /api/telemetry` - Batch telemetry submission (offline sync)

## Code Architecture

### Admin Dashboard Structure
- `src/routes/api/` - API endpoints grouped by feature
- `src/lib/server/db/` - Database schema and connection (Drizzle ORM)
- `src/routes/[feature]/` - Dashboard pages with server-side data loading
- Modal-based UI patterns for device registration flows

### Android Architecture
- MVVM pattern with Repository for data management
- `data/` - Network (Retrofit), Database (Room), Session management
- `ui/` - Compose screens ending with `Screen` suffix
- `worker/` - WorkManager background tasks (TelemetryWorker)
- Offline-first telemetry with 15-minute sync intervals

## Key Implementation Details

### Device Registration Flow
1. Admin generates 5-digit code via dashboard modal
2. Android app registers device using code within 10 minutes
3. Backend validates code and creates device record
4. Automatic cleanup of expired tokens

### Telemetry System
- Android stores events locally in Room database
- WorkManager syncs every 15 minutes when network available
- Batch processing reduces API calls
- JSON payload supports flexible event data structures

### Authentication System
- Simple username/password with Base64 encoding (upgrade to bcrypt for production)
- Session management with device association
- Role-based access control for admin functions

## Development Patterns

### File Organization
- SvelteKit: kebab-case route files, feature-based grouping
- Android: UpperCamelCase classes, lowerCamelCase functions, SCREAMING_SNAKE_CASE constants
- Database: snake_case columns, centralized schema definitions

### Testing Approach
- Web: Vitest for unit tests, Playwright for component tests
- Android: JUnit for unit tests, Espresso for UI tests
- Type checking: `npm run check` for TypeScript/Svelte validation

## Environment Setup

### Local Development
- Backend runs on port 5173 (Vite dev server)
- Android target SDK 36, minimum SDK 26
- SQLite database file (`sqlite.db`) committed for development
- Use `adb reverse` for Android→localhost communication during development

### Build Requirements
- Node.js for SvelteKit development
- Android SDK with NDK for Android builds
- Java 17+ for Android development




# Svelte-MCP
You are able to use the Svelte MCP server, where you have access to comprehensive Svelte 5 and SvelteKit documentation. Here's how to use the available tools effectively:

## Available MCP Tools:

### 1. list-sections

Use this FIRST to discover all available documentation sections. Returns a structured list with titles, use_cases, and paths.
When asked about Svelte or SvelteKit topics, ALWAYS use this tool at the start of the chat to find relevant sections.

### 2. get-documentation

Retrieves full documentation content for specific sections. Accepts single or multiple sections.
After calling the list-sections tool, you MUST analyze the returned documentation sections (especially the use_cases field) and then use the get-documentation tool to fetch ALL documentation sections that are relevant for the user's task.

### 3. svelte-autofixer

Analyzes Svelte code and returns issues and suggestions.
You MUST use this tool whenever writing Svelte code before sending it to the user. Keep calling it until no issues or suggestions are returned.

### 4. playground-link

Generates a Svelte Playground link with the provided code.
After completing the code, ask the user if they want a playground link. Only call this tool after user confirmation and NEVER if code was written to files in their project.



# svelte-llm MCP server
When connected to the svelte-llm MCP server, you have access to comprehensive Svelte 5 and SvelteKit documentation. Here's how to use the available tools effectively:

## Available MCP Tools:

### 1. list_sections
Use this FIRST to discover all available documentation sections. Returns a structured list with titles and paths.
When asked about Svelte or SvelteKit topics, ALWAYS use this tool at the start of the chat to find relevant sections.

### 2. get_documentation
Retrieves full documentation content for specific sections. Accepts single or multiple sections.
After calling the list_sections tool, you MUST analyze the returned documentation sections and then use the get_documentation tool to fetch ALL documentation sections that are relevant for the users task.
