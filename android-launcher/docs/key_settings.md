## Kiosk, Home, and App Filtering – Technical Notes

### Package IDs and Components
- `applicationId`: `edu.aiims.surveylauncher`
- Main activity: `com.example.launcher.MainActivity`
- Device admin receiver (DO): `com.example.launcher.admin.LauncherAdminReceiver`
- Home intent filter on `MainActivity`; Device Owner required for kiosk.

### Device Owner / Kiosk Flow
1) Provision DO on a wiped device/emulator:
   ```
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   adb shell dpm set-device-owner edu.aiims.surveylauncher/com.example.launcher.admin.LauncherAdminReceiver
   adb shell cmd package set-home-activity edu.aiims.surveylauncher/com.example.launcher.MainActivity
   ```
2) On resume (MainActivity), for DO devices:
   - Load policy JSON (if present), else defaults.
   - Allow-list = `DEFAULT_ALLOWED_PACKAGES` + policy.allowedApps + any installed `edu.aiims.*`.
   - Apply allow-list via `DevicePolicyManager.setLockTaskPackages` then enter lock task if not already pinned.
   - System restrictions applied via `KioskManager.setSystemRestrictions(true)`.

### Default Allow-List (when no policy)
- Launcher itself.
- ODK Collect `org.odk.collect.android`.
- WhatsApp `com.whatsapp`, WhatsApp Business `com.whatsapp.w4b`.
- Dialer: `com.android.dialer`, `com.google.android.dialer`.
- Messaging: `com.android.messaging`, `com.google.android.apps.messaging`.
- Gmail `com.google.android.gm`.
- Browsers: `org.mozilla.firefox`, `com.android.chrome`.
- REDCap `edu.vanderbilt.redcap`.
- Settings: `com.android.settings`, `com.google.android.settings`.
- Any installed `edu.aiims.*` packages (auto-added).

### App Drawer Filtering
- Uses policy allow-list if present; falls back to default allow-list.
- Displays only packages in the effective allow-list (no “show all” fallback).
- Manually ensures key apps (like ODK) are included even if not discovered via launcher intent query.

### Policy Sync
- `Settings -> Sync Policy Now` calls `/api/sync/{deviceId}`.
- JSON is stored locally; allow-list merged with defaults + `edu.aiims.*`; applied immediately to `setLockTaskPackages`.
- Policy name (from JSON or response) persisted and shown under Device Information.

### Settings / Exiting Behavior
- “Back to Home” button in Settings stops lock task and returns to launcher.
- Launching system Settings via drawer temporarily stops lock task before opening, so Home works there.
- PIN change has its own screen; other PIN-protected flows use `PinManager` storage.

### Appearance
- Theme dropdown in Settings selects gradient theme (Deep Blue / Sunset Glow / Forest) and persists to prefs.
- Background: glass container over gradient with faint AIIMS SVG backdrop (tinted light blue).

### Recovery / If Kiosk Locks You In
- Preferred: in-app “Back to Home” or launching Settings via drawer (auto-unpins).
- ADB options on emulator: kill Settings `adb shell am force-stop com.android.settings`; start Home; if Device Owner is incorrect/stale, wipe emulator and re-provision (DO cannot be removed if not test admin).

### Home Activity Setting
- After DO provisioning, ensure HOME points to the launcher:
  ```
  adb shell cmd package set-home-activity edu.aiims.surveylauncher/com.example.launcher.MainActivity
  ```
