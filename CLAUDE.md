# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android Launcher & Admin System combining a custom Android launcher with a SvelteKit admin dashboard for device management, user authentication, and telemetry collection with offline-first architecture.

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
./gradlew tasks                # List available tasks
./gradlew :app:assembleDebug   # Build debug APK
./gradlew :app:installDebug    # Install on connected device
./gradlew testDebugUnitTest    # Run unit tests
./gradlew connectedDebugAndroidTest # Instrumentation tests
adb reverse tcp:5173 tcp:5173  # Reverse proxy for local API testing
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