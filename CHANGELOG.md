# Changelog

All notable changes to this project will be documented in this file.

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