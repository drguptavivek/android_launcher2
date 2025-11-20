## Technical Implementation Notes

### Core Identifiers
- `applicationId`: `edu.aiims.surveylauncher`
- Main activity: `com.example.launcher.MainActivity`
- Device owner admin: `com.example.launcher.admin.LauncherAdminReceiver`

### Kiosk / Lock Task
- On `onResume`, if Device Owner:
  - Load policy JSON from prefs; parse `PolicyConfig`.
  - Effective allow-list = `KioskManager.DEFAULT_ALLOWED_PACKAGES` + `policy.allowedApps` + installed `edu.aiims.*` packages.
  - Apply via `DevicePolicyManager.setLockTaskPackages(admin, whitelist)` then start lock task if not already active.
  - System restrictions applied via `KioskManager.setSystemRestrictions(true)` (safe boot, factory reset, add user, physical media, volume).
- Default allow-list (when no policy):
  - Launcher, ODK Collect, WhatsApp/Business, dialer/messaging (AOSP+Google), Gmail, Firefox, Chrome, REDCap, Settings (AOSP/Google), plus any installed `edu.aiims.*`.

### App Drawer Filtering
- Reads policy from prefs; falls back to default allow-list.
- Filters installed launcher apps to allowed packages only; no “show all” fallback.
- Adds known packages (e.g., ODK) if not discoverable via launcher queries.
- Launch handlers add `FLAG_ACTIVITY_NEW_TASK`; launching Settings temporarily stops lock task first.

### Policy Sync
- Settings “Sync Policy Now” calls `/api/sync/{deviceId}`.
- Stores JSON to prefs; parsed `allowedApps` merged with defaults and `edu.aiims.*`; reapplies `setLockTaskPackages`.
- Policy name persisted (from config or response) and shown in Settings.

### Themes & UI
- Theme selector (Deep Blue, Sunset Glow, Forest) persists to prefs; gradient applied in `MainActivity`.
- Backdrop: AIIMS SVG (`app/src/main/assets/aiims_logo_currentcolor.svg`) rendered via Coil SVG, tinted light blue with low alpha behind glass container.
- Settings is scrollable; includes Change PIN button (separate screen) and Back to Home (stops lock task and returns).

### Pin Management
- `PinManager` uses EncryptedSharedPreferences with MasterKey AES256_GCM.
- PIN setup, lock, and change screens use 4-digit entry UI (dots + keypad for change).

### Escape / Recovery
- In-app: Back to Home, or launching system Settings via drawer (stops lock task first).
- ADB recovery if stuck: `adb shell am force-stop com.android.settings` then `adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME`.
- If DO is stale/incorrect, wipe emulator/data and re-provision (non-test DO cannot be removed via shell).
