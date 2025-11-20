# Device Policy Management Documentation

## Overview

The Android Launcher & Admin System implements a simplified policy management system where the Android app operates as a **Device Owner** in Kiosk mode. Administrators can define policies through the web dashboard to control which apps are allowed and enforce system restrictions.

## Key Features

- **Device Owner Mode**: The launcher runs as a Device Owner, enabling full control over the device
- **Kiosk Mode**: Locks the device to the launcher and whitelisted apps
- **Local PIN Protection**: Users set a local PIN after login to unlock apps
- **Policy Sync**: Devices periodically fetch their assigned policy from the server
- **Allowed Apps Whitelist**: Admins control which apps can be launched

## Backend API Documentation

### 1. List All Policies

**Endpoint:** `GET /api/policies`

**Purpose:** Retrieve all policies created by administrators

**Response (200 OK):**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "School Mode",
    "config": "{\"allowedApps\":[\"com.android.settings\",\"com.example.launcher\"],\"systemToggles\":{\"wifi\":true,\"bluetooth\":false}}",
    "createdAt": "2024-01-01T10:00:00.000Z"
  }
]
```

### 2. Create Policy

**Endpoint:** `POST /api/policies`

**Purpose:** Create a new policy with allowed apps and system toggles

**Request Body:**
```json
{
  "name": "School Mode",
  "config": {
    "allowedApps": [
      "com.android.settings",
      "com.example.launcher",
      "com.android.calculator2",
      "org.odk.collect.android"
    ],
    "systemToggles": {
      "wifi": true,
      "bluetooth": false
    }
  }
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "School Mode",
  "config": { ... },
  "createdAt": "2024-01-01T10:00:00.000Z"
}
```

### 3. Assign Policy to Device

**Endpoint:** `POST /api/devices/:id/policy`

**Purpose:** Assign a policy to a specific device

**Request Body:**
```json
{
  "policyId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response (200 OK):**
```json
{
  "success": true
}
```

### 4. Sync Policy (Device Endpoint)

**Endpoint:** `GET /api/sync/:deviceId`

**Purpose:** Called by Android devices to fetch their assigned policy

**Response (200 OK):**
```json
{
  "config": "{\"allowedApps\":[\"com.android.settings\",\"com.example.launcher\"],\"systemToggles\":{\"wifi\":true,\"bluetooth\":false}}",
  "updatedAt": "2024-01-01T10:00:00.000Z"
}
```

**Response (404 Not Found):**
```json
{
  "error": "No policy assigned"
}
```

## Database Schema

### Policies Table
```sql
CREATE TABLE policies (
  id TEXT PRIMARY KEY,                    -- UUID
  name TEXT NOT NULL,                     -- Policy name (e.g., "School Mode")
  config TEXT NOT NULL,                   -- JSON string with allowedApps and systemToggles
  createdAt TIMESTAMP NOT NULL            -- When policy was created
);
```

### Device Policies Table
```sql
CREATE TABLE device_policies (
  deviceId INTEGER NOT NULL,              -- Foreign key to devices.id
  policyId TEXT NOT NULL,                 -- Foreign key to policies.id
  assignedAt TIMESTAMP NOT NULL,          -- When policy was assigned
  FOREIGN KEY (deviceId) REFERENCES devices(id),
  FOREIGN KEY (policyId) REFERENCES policies(id)
);
```

## Android Device Owner Setup

### Prerequisites

The Android app must be set as the **Device Owner** to enable Kiosk mode and policy enforcement.

### Development Setup

1. **Factory Reset** the device or emulator (or use a fresh AVD)
2. **Do NOT** complete the initial setup wizard
3. **Run the following ADB command**:
   ```bash
   adb shell dpm set-device-owner com.example.launcher/.admin.LauncherAdminReceiver
   ```
4. **Verify** Device Owner status:
   ```bash
   adb shell dumpsys device_policy | grep -A 5 "Device Owner"
   ```

### Production Setup (Headwind MDM)

For production deployments, use a QR code provisioning method:

1. Generate a QR code with the following JSON:
   ```json
   {
     "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.example.launcher/.admin.LauncherAdminReceiver",
     "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://your-server.com/app-release.apk",
     "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": false,
     "android.app.extra.PROVISIONING_WIFI_SSID": "YourWiFiSSID",
     "android.app.extra.PROVISIONING_WIFI_PASSWORD": "YourWiFiPassword"
   }
   ```
2. During device setup, scan the QR code
3. The device will automatically download and install the app as Device Owner

## Android Implementation

### 1. Device Admin Receiver

**File:** `LauncherAdminReceiver.kt`

```kotlin
class LauncherAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Device Admin Enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Device Admin Disabled", Toast.LENGTH_SHORT).show()
    }
}
```

### 2. Kiosk Manager

**File:** `KioskManager.kt`

```kotlin
class KioskManager(private val context: Context) {
    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(context, LauncherAdminReceiver::class.java)

    fun isDeviceOwner(): Boolean {
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    fun enableKioskMode(activity: Activity) {
        if (isDeviceOwner()) {
            val packages = arrayOf(context.packageName)
            dpm.setLockTaskPackages(adminComponent, packages)
            activity.startLockTask()
        }
    }

    fun setAllowedApps(packageNames: List<String>) {
        if (isDeviceOwner()) {
            val allPackages = (packageNames + context.packageName).toTypedArray()
            dpm.setLockTaskPackages(adminComponent, allPackages)
        }
    }

    fun setSystemRestrictions(enable: Boolean) {
        if (isDeviceOwner()) {
            if (enable) {
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
                // ... more restrictions
            }
        }
    }
}
```

### 3. PIN Manager

**File:** `PinManager.kt`

Uses `EncryptedSharedPreferences` to securely store the user's PIN locally.

```kotlin
class PinManager(context: Context) {
    private val sharedPreferences: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "secure_pin_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun isPinSet(): Boolean = sharedPreferences.contains(KEY_PIN)
    fun setPin(pin: String) { sharedPreferences.edit().putString(KEY_PIN, pin).apply() }
    fun validatePin(pin: String): Boolean = sharedPreferences.getString(KEY_PIN, null) == pin
    fun clearPin() { sharedPreferences.edit().remove(KEY_PIN).apply() }
}
```

### 4. Policy Sync Flow

1. **TelemetryWorker** (runs every 15 minutes) calls `GET /api/sync/:deviceId`
2. **Parse** the JSON config to extract `allowedApps` list
3. **Update** `KioskManager.setAllowedApps(allowedApps)`
4. **Store** policy locally (optional, for offline access)

## User Flow

### First-Time Setup

1. **Device Registration**: User enters 5-digit code from admin dashboard
2. **Login**: User logs in with their credentials
3. **PIN Setup**: User creates a 4-6 digit PIN (stored locally)
4. **Kiosk Mode Activated**: Device enters lock task mode

### Daily Usage

1. **App Launch**: User taps an app icon
2. **PIN Prompt**: `PinLockScreen` appears
3. **Validation**: User enters PIN
4. **Launch**: If PIN is correct and app is in `allowedApps`, app launches

### Policy Updates

1. **Admin** assigns a new policy to the device via dashboard
2. **Device** syncs policy during next `TelemetryWorker` run (every 15 min)
3. **KioskManager** updates the allowed apps list
4. **User** sees updated app drawer on next launch

## Security Considerations

### Device Owner Privileges

- **Full Control**: Device Owner has unrestricted access to device management APIs
- **Cannot be Uninstalled**: Users cannot remove the app without factory reset
- **Persistent**: Survives app updates and device reboots

### PIN Security

- **Local Only**: PIN is never sent to the server
- **Encrypted**: Stored using Android Keystore and EncryptedSharedPreferences
- **Reset**: User must re-login to reset PIN (clears local data)

### Policy Enforcement

- **Lock Task Mode**: Prevents users from exiting the launcher
- **System Restrictions**: Disables Safe Mode, Factory Reset, etc.
- **App Whitelist**: Only approved apps can be launched

## Testing Checklist

### Backend
- [ ] Create a policy with 2-3 allowed apps
- [ ] Assign policy to a test device
- [ ] Verify `/api/sync/:deviceId` returns correct config

### Android
- [ ] Set app as Device Owner using `adb` command
- [ ] Register device and login
- [ ] Set PIN (4-6 digits)
- [ ] Verify Kiosk mode activates
- [ ] Verify only allowed apps appear in drawer
- [ ] Test PIN unlock flow
- [ ] Verify policy sync updates allowed apps

## Troubleshooting

### "Not a Device Owner" Error
- Ensure device is factory reset or fresh AVD
- Run `adb shell dpm set-device-owner` command
- Verify with `adb shell dumpsys device_policy`

### PIN Not Working
- Check `EncryptedSharedPreferences` initialization
- Verify `androidx.security:security-crypto` dependency
- Clear app data and re-login to reset PIN

### Apps Not Filtering
- Verify policy JSON format in database
- Check `TelemetryWorker` logs for sync errors
- Ensure `KioskManager.setAllowedApps()` is called after sync

---

## Technical Learnings & Key Insights

### 1. Android 11+ Package Visibility Restrictions

**Problem:** On Android 11 and above, apps cannot see other installed apps by default due to package visibility restrictions. `PackageManager.queryIntentActivities()` returns an empty or limited list.

**Solution:** Add a `<queries>` element to `AndroidManifest.xml`:

```xml
<queries>
    <!-- Allow launcher to query all apps with LAUNCHER intent -->
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
    
    <!-- Explicitly declare specific packages if needed -->
    <package android:name="org.odk.collect.android" />
</queries>
```

**Key Points:**
- Without `<queries>`, the launcher cannot see installed apps
- The `<intent>` filter allows querying all apps with launcher activities
- Specific packages can be declared with `<package>` elements
- This is required for Android API 30+ (Android 11+)

### 2. Lock Task Mode vs. Multi-App Kiosk

**Single-App Kiosk (Lock Task Mode):**
```kotlin
// This "pins" the launcher and prevents navigation
activity.startLockTask()
```

**Multi-App Kiosk (Recommended):**
```kotlin
// Set whitelist but DON'T call startLockTask()
dpm.setLockTaskPackages(adminComponent, allowedPackages)
// Users can freely navigate between whitelisted apps
```

**Key Differences:**
- **Single-App**: Calls `startLockTask()` - app is "pinned", user cannot leave
- **Multi-App**: Only sets whitelist - users can switch between allowed apps
- **System Restrictions**: Apply in both modes via `addUserRestriction()`

**Our Implementation:**
- We use **multi-app kiosk** mode
- `MainActivity.onResume()` sets the whitelist but does NOT call `startLockTask()`
- Users can freely navigate between whitelisted apps
- System restrictions (no factory reset, no safe boot, etc.) are still enforced

### 3. Lock Task Package Whitelist Management

**Problem:** `setLockTaskPackages()` throws "duplicate element" error if the same package appears twice.

**Solution:** Use a Set to remove duplicates:

```kotlin
fun resetLockTaskPackages(allowed: List<String>) {
    if (isDeviceOwner()) {
        // Clear existing whitelist first
        dpm.setLockTaskPackages(adminComponent, arrayOf())
        
        // Add launcher + allowed apps, removing duplicates
        val all = (allowed + context.packageName).toSet().toTypedArray()
        dpm.setLockTaskPackages(adminComponent, all)
    }
}
```

**Key Points:**
- Always include the launcher's own package in the whitelist
- Use `.toSet()` to remove duplicates before converting to array
- Clear the whitelist first (`arrayOf()`) then set the new list
- This prevents "IllegalArgumentException: duplicate element" errors

### 4. ODK Collect Integration Challenges

**Problem:** ODK Collect doesn't appear in `queryIntentActivities()` results, even with `<queries>` element.

**Root Causes:**
1. `getLaunchIntentForPackage()` returns `null` for ODK Collect
2. The main launcher activity might not be exported or have the correct intent filters
3. ODK Collect uses a splash screen activity as the entry point

**Solution:** Manually add ODK Collect to the app list:

```kotlin
val manualApps = listOf(
    Triple("org.odk.collect.android", "ODK Collect", "org.odk.collect.android.activities.SplashScreenActivity")
)

for ((packageName, defaultLabel, activityName) in manualApps) {
    val isInstalled = try {
        packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: Exception) { false }
    
    if (isInstalled && !apps.any { it.packageName == packageName }) {
        apps.add(AppInfo(
            label = packageManager.getApplicationInfo(packageName, 0).loadLabel(packageManager).toString(),
            packageName = packageName,
            icon = packageManager.getApplicationIcon(packageName)
        ))
    }
}
```

**Key Points:**
- Use `getPackageInfo()` to check if app is installed (more reliable than `getLaunchIntentForPackage()`)
- Store the activity class name for manual intent creation
- ODK Collect's entry point is `SplashScreenActivity`, not `MainMenuActivity`
- This approach works for any app that doesn't appear in standard queries

### 5. Device Owner Setup Requirements

**Critical Requirements:**
1. **Factory Reset**: Device must be factory reset OR use a fresh AVD
2. **No Accounts**: Cannot set device owner if any accounts exist on the device
3. **Before First Launch**: Must set device owner BEFORE launching the app for the first time
4. **ADB Command**: `adb shell dpm set-device-owner com.example.launcher/.admin.LauncherAdminReceiver`

**Common Errors:**
- "Not allowed to set device owner because there are already some accounts" → Factory reset required
- "Not a device owner" → Run the `dpm set-device-owner` command
- Cannot uninstall device owner app → Must factory reset to remove

**Production Deployment:**
- Use QR code provisioning for zero-touch deployment
- Include WiFi credentials, APK download URL, and device admin component in QR code
- Device automatically downloads and sets up as device owner during initial setup

### 6. Policy Sync Architecture

**Sync Trigger Points:**
1. **Periodic Sync**: `TelemetryWorker` runs every 15 minutes
2. **Manual Sync**: "Sync Policy Now" button in Settings
3. **App Resume**: `MainActivity.onResume()` applies cached policy

**Sync Flow:**
```
1. Fetch policy from server (GET /api/sync/:deviceId)
2. Save policy JSON to SessionManager (SharedPreferences)
3. Parse JSON to extract allowedApps list
4. Call KioskManager.resetLockTaskPackages(allowedApps)
5. AppDrawer reads policy and filters app list
```

**Key Design Decisions:**
- Policy is cached locally for offline access
- Whitelist is updated on every `onResume()` to ensure consistency
- `resetLockTaskPackages()` clears and re-sets to avoid stale state
- AppDrawer re-reads policy on each recomposition (no caching)

### 7. System Restrictions Best Practices

**Recommended Restrictions:**
```kotlin
dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_ADD_USER)
dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_ADJUST_VOLUME)
```

**Restrictions to Avoid:**
- `DISALLOW_INSTALL_APPS` - Prevents installing updates
- `DISALLOW_UNINSTALL_APPS` - Too restrictive for testing
- `DISALLOW_MODIFY_ACCOUNTS` - Can break Google services

**Testing Tip:**
- Apply restrictions gradually
- Test each restriction individually
- Some restrictions require device reboot to take effect

### 8. App Drawer Filtering Logic

**Challenge:** Balance between showing all apps and filtering by policy.

**Our Approach:**
```kotlin
val allowedApps = sessionManager.getPolicy()?.allowedApps ?: emptyList()

val filteredApps = if (allowedApps.isNotEmpty()) {
    installedApps.filter { app -> allowedApps.contains(app.packageName) }
} else {
    installedApps // Show all if no policy (fallback)
}
```

**Key Points:**
- If no policy exists, show all apps (better UX for testing)
- Filter is applied on every recomposition (no caching)
- Policy is read from SharedPreferences (fast, no network call)
- Filtering happens in the UI layer, not in the query

### 9. Performance Considerations

**App Query Optimization:**
- `queryIntentActivities()` is slow (~100-200ms for 30+ apps)
- Call it once and cache results in a `remember {}` block
- Only re-query when policy changes or app is installed/uninstalled

**Policy Sync Optimization:**
- Cache policy locally to avoid network calls on every app launch
- Use WorkManager for background sync (battery-efficient)
- Only update whitelist if policy has changed (compare timestamps)

**UI Responsiveness:**
- Load app icons asynchronously
- Use `LazyVerticalGrid` for efficient scrolling
- Filter apps in background thread if list is very large

### 10. Debugging Tips

**Enable Verbose Logging:**
```kotlin
android.util.Log.d("AppDrawer", "Found ${apps.size} apps")
android.util.Log.d("KioskManager", "Whitelist: ${packages.joinToString()}")
```

**Check Device Owner Status:**
```bash
adb shell dumpsys device_policy | grep "Device Owner"
```

**View Lock Task Packages:**
```bash
adb shell dumpsys activity activities | grep "lockTaskPackages"
```

**Monitor Policy Sync:**
```bash
adb logcat | grep "TelemetryWorker\|Policy"
```

**Test App Launch:**
```bash
adb shell am start -n org.odk.collect.android/.activities.SplashScreenActivity
```

---

## Implementation Status

### Completed
- [x] Backend: Database schema (policies, device_policies)
- [x] Backend: API endpoints (policies, assign, sync)
- [x] Backend: Policy management UI
- [x] Android: DeviceAdminReceiver & device_admin.xml
- [x] Android: KioskManager helper class
- [x] Android: PinManager with EncryptedSharedPreferences
- [x] Android: PinSetupScreen & PinLockScreen UI
- [x] Android: MainActivity integration
- [x] Android: Policy sync in TelemetryWorker
- [x] Android: Multi-app kiosk mode (whitelist without lock task pinning)
- [x] Android: App drawer filtering based on policy
- [x] Android: Manual "Sync Policy Now" button in Settings
- [x] Android: Package visibility queries for Android 11+
- [x] Android: ODK Collect manual integration
- [x] Testing: End-to-end policy enforcement

### Known Limitations
- Some system apps (Settings, Play Store) cannot run in lock task mode due to Android security restrictions
- ODK Collect requires manual addition to app list (doesn't appear in standard queries)
- Device owner setup requires factory reset (cannot be done on devices with existing accounts)
- Policy sync interval is 15 minutes (can be manually triggered from Settings)
