feat: Implement multi-app kiosk mode with policy management and ODK Collect integration

## Summary
Implemented a complete device policy management system with multi-app kiosk mode support. The launcher now operates as a Device Owner, allowing administrators to control which apps users can access through a centralized policy system. Successfully integrated ODK Collect and resolved Android 11+ package visibility restrictions.

## Backend Changes

### Database Schema
- Added `policies` table for storing policy configurations (id, name, config JSON, createdAt)
- Added `device_policies` junction table for device-policy assignments (deviceId, policyId, assignedAt)
- Applied schema changes via Drizzle ORM

### API Endpoints
- **POST /api/policies** - Create new policies with allowedApps and systemToggles
- **GET /api/policies** - List all policies
- **POST /api/devices/:id/policy** - Assign policy to device
- **GET /api/sync/:deviceId** - Device endpoint for fetching assigned policy

### Admin Dashboard UI
- Created `/policies` page with policy management interface
- Added JSON editor for policy configuration
- Enhanced `/devices` page with policy assignment dropdown
- Added "Policy" column to devices table showing assigned policy

## Android Client Changes

### Device Owner & Kiosk Mode
- Created `LauncherAdminReceiver` extending `DeviceAdminReceiver`
- Added `device_admin.xml` with required device admin policies
- Implemented `KioskManager` utility class:
  - `isDeviceOwner()` - Check device owner status
  - `resetLockTaskPackages()` - Set app whitelist with duplicate prevention
  - `setSystemRestrictions()` - Apply system-level restrictions
  - `enableKioskMode()` / `stopKioskMode()` - Control lock task mode
- **Multi-app kiosk**: Whitelist apps without pinning launcher (users can switch between allowed apps)

### Policy Management
- Added `PolicyConfig` and `PolicyResponse` data classes to `ApiService`
- Implemented `syncPolicy()` API call in `ApiService`
- Extended `SessionManager` with `savePolicy()` and `getPolicy()` methods
- Updated `TelemetryWorker` to fetch and apply policies every 15 minutes
- Added "Sync Policy Now" button in Settings for manual policy refresh

### App Drawer & Filtering
- Created `AppDrawer` composable with app grid layout
- Implemented policy-based app filtering (only whitelisted apps shown)
- Added manual app addition for apps not appearing in standard queries (e.g., ODK Collect)
- Displays app count and "No apps available" message when appropriate

### Android 11+ Package Visibility
- Added `<queries>` element to AndroidManifest.xml:
  - Intent filter for LAUNCHER category apps
  - Explicit package declaration for org.odk.collect.android
- Resolved issue where `queryIntentActivities()` returned limited results on Android 11+

### ODK Collect Integration
- Manually added ODK Collect to app list (doesn't appear in standard queries)
- Used `getPackageInfo()` instead of `getLaunchIntentForPackage()` for reliability
- Configured correct entry point: `org.odk.collect.android.activities.SplashScreenActivity`

### PIN Management
- Added `androidx.security:security-crypto` dependency
- Created `PinManager` using `EncryptedSharedPreferences` for secure local PIN storage
- Implemented `PinSetupScreen` and `PinLockScreen` composables
- Integrated PIN flow into `MainActivity`

### MainActivity Updates
- Added `onResume()` lifecycle method:
  - Loads policy from SessionManager
  - Calls `resetLockTaskPackages()` with allowed apps
  - Applies system restrictions
  - **Does NOT** call `startLockTask()` to allow multi-app navigation
- Integrated PIN setup and lock screens
- Added AppDrawer to home screen

## Documentation

### New Documentation Files
- **docs/device-policy.md** - Comprehensive policy management documentation:
  - Backend API reference
  - Database schema
  - Android implementation guide
  - Device owner setup instructions
  - User flows and security considerations
  - **Technical Learnings section** with 10 key insights:
    1. Android 11+ package visibility restrictions
    2. Lock task mode vs multi-app kiosk
    3. Whitelist management and duplicate handling
    4. ODK Collect integration challenges
    5. Device owner setup requirements
    6. Policy sync architecture
    7. System restrictions best practices
    8. App drawer filtering logic
    9. Performance considerations
    10. Debugging tips
- **docs/SETUP_GUIDE.md** - Step-by-step setup and testing guide

### Updated Documentation
- **PLAN.md** - Updated Phase 4 completion status
- **CLAUDE.md** - Added device owner setup commands and troubleshooting notes

## Key Technical Decisions

### Multi-App Kiosk vs Single-App Lock Task
- Chose **multi-app kiosk** approach: set whitelist but don't call `startLockTask()`
- Users can freely navigate between whitelisted apps
- System restrictions still enforced (no factory reset, safe boot, etc.)
- Better UX than pinned single-app mode

### Policy Sync Strategy
- Periodic sync every 15 minutes via `TelemetryWorker`
- Manual sync available in Settings
- Policy cached locally in SharedPreferences for offline access
- Whitelist updated on every `MainActivity.onResume()` for consistency

### Duplicate Prevention
- `resetLockTaskPackages()` uses `.toSet()` to remove duplicates
- Clears existing whitelist before setting new one
- Prevents "IllegalArgumentException: duplicate element" errors

### Package Visibility Solution
- Added `<queries>` element for Android 11+ compatibility
- Manual app addition fallback for apps not appearing in queries
- Use `getPackageInfo()` instead of `getLaunchIntentForPackage()` for reliability

## Testing Notes

### Device Owner Setup
1. Factory reset device or use fresh AVD
2. Install launcher APK
3. Run: `adb shell dpm set-device-owner com.example.launcher/.admin.LauncherAdminReceiver`
4. Set up port forwarding: `adb reverse tcp:5173 tcp:5173`

### Policy Testing
1. Create policy via admin dashboard with allowed apps
2. Assign policy to device
3. Use "Sync Policy Now" in Settings or wait 15 minutes
4. Verify only whitelisted apps appear in app drawer
5. Confirm apps launch successfully

### Known Limitations
- Some system apps (Settings, Play Store) cannot run in lock task mode
- ODK Collect requires manual addition to app list
- Device owner setup requires factory reset
- Policy sync interval is 15 minutes (manual trigger available)

## Files Changed

### Modified (12 files)
- CLAUDE.md
- PLAN.md
- admin-dashboard/src/lib/server/db/schema.ts
- admin-dashboard/src/routes/devices/+page.server.ts
- admin-dashboard/src/routes/devices/+page.svelte
- android-launcher/app/build.gradle.kts
- android-launcher/app/src/main/AndroidManifest.xml
- android-launcher/app/src/main/java/com/example/launcher/MainActivity.kt
- android-launcher/app/src/main/java/com/example/launcher/data/SessionManager.kt
- android-launcher/app/src/main/java/com/example/launcher/data/network/ApiService.kt
- android-launcher/app/src/main/java/com/example/launcher/ui/settings/SettingsScreen.kt
- android-launcher/app/src/main/java/com/example/launcher/worker/TelemetryWorker.kt

### New Files
- admin-dashboard/src/routes/api/devices/[id]/policy/+server.ts
- admin-dashboard/src/routes/api/policies/+server.ts
- admin-dashboard/src/routes/api/sync/[deviceId]/+server.ts
- admin-dashboard/src/routes/policies/+page.server.ts
- admin-dashboard/src/routes/policies/+page.svelte
- android-launcher/app/src/main/java/com/example/launcher/admin/LauncherAdminReceiver.kt
- android-launcher/app/src/main/java/com/example/launcher/ui/home/AppDrawer.kt
- android-launcher/app/src/main/java/com/example/launcher/ui/pin/PinLockScreen.kt
- android-launcher/app/src/main/java/com/example/launcher/ui/pin/PinSetupScreen.kt
- android-launcher/app/src/main/java/com/example/launcher/util/KioskManager.kt
- android-launcher/app/src/main/java/com/example/launcher/util/PinManager.kt
- android-launcher/app/src/main/res/xml/device_admin.xml
- docs/device-policy.md
- docs/SETUP_GUIDE.md

## Statistics
- 12 files modified
- 393 insertions(+), 55 deletions(-)
- 15 new files created
- 2 new documentation files

## Breaking Changes
None - all changes are additive

## Migration Notes
1. Run database migration: `npx drizzle-kit push`
2. Factory reset test devices to set as device owner
3. Reinstall launcher app and set device owner before first launch
4. Create and assign policies via admin dashboard
