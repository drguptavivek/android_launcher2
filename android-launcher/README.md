## AIIMS SurveyLauncher (Android)

Kotlin/Compose launcher app (`applicationId`: `edu.aiims.surveylauncher`) with multi-app kiosk, policy allow-listing, and PIN gating.

### Build & Install
- Requirements: Android SDK, Java 17+. Target SDK 36, min SDK 26.
- Build debug APK: `./gradlew :app:assembleDebug`
- Install to device/emulator: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- Optional: install ODK Collect bundle included at repo root: `adb install -r ODK-Collect-v2025.3.3.apk`

### Device Owner Provisioning (fresh device/emulator)
1) Factory reset / wipe data.
2) Install the launcher APK.
3) Set Device Owner:
   ```
   adb shell dpm set-device-owner edu.aiims.surveylauncher/edu.aiims.surveylauncher.admin.LauncherAdminReceiver
   ```
4) Set as HOME (if needed):
   ```
   adb shell cmd package set-home-activity edu.aiims.surveylauncher/edu.aiims.surveylauncher.MainActivity
   ```

### Kiosk & Allow-List
- Default allow-list when no policy is present: launcher itself, ODK, WhatsApp/Business, dialer/messaging variants (AOSP + Google), Gmail, Firefox, Chrome, REDCap (`edu.vanderbilt.redcap`), Settings (AOSP + Google), plus any installed `edu.aiims.*` apps.
- Policy sync (`Sync Policy Now` in Settings) merges server `allowedApps` + defaults + `edu.aiims.*` apps and applies lock-task packages before pinning.
- App drawer shows only allow-listed apps.

### Settings & UX
- Policy name shown in Device Information after sync (stored locally).
- Appearance: theme dropdown (Deep Blue, Sunset Glow, Forest) updates the gradient shell; persisted in preferences.
- PIN: dedicated Change PIN screen; PIN required for gated flows.
- Back to Home: button in Settings stops kiosk and returns to the launcher. Launching system Settings from the drawer also temporarily releases lock-task so Home works there.
- Backdrop: faint AIIMS logo from `app/src/main/assets/aiims_logo_currentcolor.svg` rendered via Coil SVG.

### Exiting / Recovery
- If stuck in Settings while kiosk is active: use the in-app “Back to Home” button. If the device is hard-pinned, use ADB: `adb shell am force-stop com.android.settings` then `adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME`. As a last resort on emulator: `adb emu kill` then restart with `-wipe-data` and re-provision.
