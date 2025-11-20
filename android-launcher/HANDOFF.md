## Handoff – AIIMS SurveyLauncher (Android)

Status Summary
- App ID: `edu.aiims.surveylauncher`; main activity `com.example.launcher.MainActivity`; DO receiver `com.example.launcher.admin.LauncherAdminReceiver`.
- Kiosk: default allow-list includes launcher, ODK, WhatsApp/Business, dialer/messaging (AOSP+Google), Gmail, Firefox, Chrome, REDCap, Settings (AOSP/Google), plus any installed `edu.aiims.*` apps. App drawer shows only allow-listed apps.
- Policy sync: `Settings -> Sync Policy Now` merges server allow-list + defaults + `edu.aiims.*`, applies `setLockTaskPackages`, and saves policy name for display.
- Appearance: gradient theme selector (Deep Blue/Sunset/Forest) persists; backdrop uses AIIMS SVG tinted light blue.
- PIN: separate Change PIN screen; PIN stored via `PinManager`.
- Settings escape: “Back to Home” button stops lock task and returns home; launching system Settings from the drawer also unpins first.

Build/Install
```
cd android-launcher
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Provisioning (fresh device/emulator)
```
adb shell dpm set-device-owner edu.aiims.surveylauncher/com.example.launcher.admin.LauncherAdminReceiver
adb shell cmd package set-home-activity edu.aiims.surveylauncher/com.example.launcher.MainActivity
```

Exiting/Recovery
- In-app: use “Back to Home” in Settings to unpin.
- ADB: `adb shell am force-stop com.android.settings` then `adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME`. If DO is stale/old, wipe emulator and re-provision (non-test DO cannot be removed).

Notes/Todos
- Permissions: GRANT_USAGE_ACCESS still prompted if missing.
- Package namespaces remain `com.example.launcher.*` in code; DO component uses that namespace even though appId changed. Refactor if you want fully aligned packages.***
