Gotcha — this is classic “lock task is on, but the allow-list isn’t set (or is wrong)” behaviour.

Once **security/kiosk (lock task)** kicks in, Android will **only** allow activities from packages in the **lock task allow-list** defined by your **Device Owner**. Everything else (even Settings, Calendar, ODK) will silently fail to launch or just flash and disappear.

Let me walk through what’s probably happening and how to debug/fix.

---

## 1. What’s really happening under the hood

When your launcher calls:

```kotlin
startLockTask()
```

Android checks:

> “Is the current app in the `lockTaskPackages` list for the active Device Owner?”

* If **only your launcher** is in that list, then:

  * Launcher can run.
  * Any attempt to `startActivity()` for **other packages** (ODK, Settings, Calendar) is blocked while lock task is active.

That’s exactly the “nothing launches after security kicks in” pattern.

So the root is almost always:
👉 **`setLockTaskPackages()` doesn’t include all the apps you want to run.**

---

## 2. What you *must* do as Device Owner

From your **Device Owner** admin (your DPC/launcher’s `DeviceAdminReceiver`), you need something like:

```kotlin
val dpm = context.getSystemService(DevicePolicyManager::class.java)
val admin = ComponentName(context, YourDeviceAdminReceiver::class.java)

val lockTaskPackages = arrayOf(
    context.packageName,             // your launcher
    "org.odk.collect.android",       // ODK
    "com.android.settings",          // Settings (if you really want)
    "com.android.calendar"           // Calendar (OEM package may differ!)
)

dpm.setLockTaskPackages(admin, lockTaskPackages)
```

⚠️ Gotchas:

1. **Must be called by the Device Owner admin** (if you call it from a non-DO app, it’s ignored).
2. Package names must be **exact**.

   * ODK is usually `org.odk.collect.android`.
   * Settings and Calendar vary by OEM (e.g., Samsung, Lenovo may have different package names).
3. Call this **before** you enter lock task (`startLockTask()`).

---

## 3. How to actually launch ODK / Settings / Calendar

From your launcher:

```kotlin
fun launchPackage(context: Context, pkg: String) {
    val pm = context.packageManager
    val intent = pm.getLaunchIntentForPackage(pkg)
    if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } else {
        // show “App not installed / no launchable activity”
    }
}
```

Example calls:

```kotlin
launchPackage(context, "org.odk.collect.android")
launchPackage(context, "com.android.settings")
launchPackage(context, "com.android.calendar")
```

If:

* `getLaunchIntentForPackage` returns **null** → package or main launcher activity not found.
* It returns non-null but **nothing appears** when lock task is on → **not in lock task allow-list**.

---

## 4. Quick debugging checklist (run via `adb`)

On one of the failing devices, plug into ADB and:

### A) Confirm you are *actually* Device Owner

```bash
adb shell dpm get-device-owner
```

You should see your package + admin receiver.
If you see nothing, or a different package → your app is not DO, and all `setLockTaskPackages` calls are ignored.

---

### B) Inspect current lock task allow-list

```bash
adb shell dumpsys device_policy | grep -A 5 'Lock task'
```

You should see something like:

```text
Lock task packages:
  User 0: com.your.launcher
          org.odk.collect.android
          com.android.settings
          com.android.calendar
```

If you see only your launcher → that’s the bug.
If you don’t see ODK / Settings / Calendar → update the `setLockTaskPackages` call.

---

### C) Confirm lock task mode is actually on

```bash
adb shell dumpsys activity activities | grep mLockTaskModeState
```

You’ll see states like `LOCK_TASK_MODE_PINNED` or `LOCK_TASK_MODE_LOCKED`.
If lock task is on and the packages are *not* in the list, Android will block them.

---

## 5. Other things that can block apps

Less common, but worth checking if allow-list looks correct:

1. **You called `setApplicationHidden()` / `setPackagesSuspended()`**
   – Make sure you didn’t accidentally hide/suspend ODK or system apps.

2. **User restrictions** like:

   ```kotlin
   dpm.addUserRestriction(admin, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
   dpm.addUserRestriction(admin, UserManager.DISALLOW_APPS_CONTROL)
   ```

   These don’t usually block launching installed apps, but aggressive policies on some OEM builds can behave oddly.

3. **Wrong package for system apps**

   * Settings may be `com.android.settings` or OEM-specific.
   * Calendar might be `com.google.android.calendar` or an OEM one.
     Use:

   ```bash
   adb shell pm list packages | grep -i calendar
   adb shell pm list packages | grep -i settings
   ```

   Then use those package names in `setLockTaskPackages()`.

---

## 6. Minimal working pattern (what I’d suggest you implement)

1. On DO setup / app start (once):

   ```kotlin
   val allowed = arrayOf(
       context.packageName,
       "org.odk.collect.android",
       "com.android.settings",
       "com.google.android.calendar" // or OEM pkg
   )
   dpm.setLockTaskPackages(admin, allowed)
   ```

2. When entering secure mode (after PIN etc.):

   ```kotlin
   (context as? Activity)?.startLockTask()
   ```

3. All app launches go through `getLaunchIntentForPackage` as shown above.

---

> ✅ Lock task is ON
> ❌ `setLockTaskPackages` doesn’t include the apps you expect to launch.

Short answer: there *isn’t* a true “equivalent” to kiosk mode for hard lockdown — but there are a few **lighter-weight** patterns you can use depending on how strict you want to be.

I’ll group them by “how locked” you want the device.

---

## 1️⃣ Still Device Owner, but *no* lock task (softer lockdown)

You keep **Device Owner**, but **don’t call `startLockTask()`**. Instead you:

* Make your app the **HOME/launcher**.
* Use `DevicePolicyManager` to **restrict** but not hard-lock:

Examples:

* `dpm.addUserRestriction(admin, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)`
* `dpm.addUserRestriction(admin, UserManager.DISALLOW_ADD_USER)`
* Block factory reset, screen capture, USB file transfer, etc.
* Hide certain apps via:

  * `setApplicationHidden(...)`
  * `setPackagesSuspended(...)`

Outcome:

* User is mostly confined to whatever your launcher shows.
* They *can* still see system UI (status bar, recents), but you control what apps are visible/launchable.
* Easier to let them open Settings, Wi-Fi, etc., without whitelisting every system package in lock task.

👉 For SurveyLauncher:
You can have **multi-app survey tablet** just with:

* DO
* Your custom launcher
* User restrictions
  and skip lock task for now if it’s blocking your flows.

---

## 2️⃣ Custom launcher only (no DO) – mild control

If you’re ok with **less security**, you can:

* Just set your app as **HOME** (default launcher).
* Don’t become Device Owner.
* No lock task, no DPM restrictions.

You can still:

* Show only ODK + a few icons.
* Hide “Settings” shortcut from your own UI.

But:

* Student/field worker can still get to system UI via gestures, status bar, notification shade, etc.
* They can install apps, change settings, etc.

👉 Good for **demo / pilot** devices, not for national program-level lockdown.

---

## 3️⃣ System “Screen Pinning” (user-initiated app pinning)

Built-in Android feature (Settings → Security → Screen pinning):

* User opens app → recents → “Pin” app.
* Back+Recents to unpin.

Pros:

* Works without Device Owner.
* Quick way to keep people in a single app temporarily.

Cons:

* **Not centrally controllable**.
* Needs user action to pin/unpin.
* Easily bypassed by someone who knows the gesture.

👉 Fine for occasional “don’t touch anything” situations; not good for 1200-tablet field deployment.

---

## 4️⃣ Work Profile / Profile Owner (BYOD style)

If tablets are **shared with other uses**, you can:

* Use **Profile Owner** to create a **managed work profile**.
* Lock down only the “work” side: managed apps, data, policies.
* The personal side stays free.

But:

* You **cannot do real kiosk** with PO (no full device lock).
* User still controls device owner side.

👉 Good for BYOD/doctor phones, but not for your survey tablets where you want full control.

---

## 5️⃣ MDM / EMM solutions (HeadwindMDM, Android Management API, etc.)

Instead of hand-coding everything, you can:

* Use an **MDM** (HeadwindMDM, OEM solution, or Google Android Management API).
* Let the MDM handle:

  * DO provisioning
  * Kiosk mode
  * App install/update
  * Network, certs, restrictions
* Run **SurveyLauncher as one of the managed apps**, or even as the managed launcher.

You can choose:

* **Fully managed, kiosk ON** → strict.
* **Fully managed, kiosk OFF** → your case: DO + restrictions + launcher, but no lock task.

👉 This is more ops-friendly at large scale, but adds one more moving part.

---

## 6️⃣ Accessibility / Parental-control style lockdown (hacky)

Some apps fake kiosk using:

* **Accessibility service** to intercept navigation.
* Overlays.
* Parental control APIs.

I’d **not** recommend this for you because:

* Less robust, more OEM-dependent.
* Ugly to maintain at your scale.
* You *already* have a DO-based design which is cleaner.

---

## What I’d do for your project *right now*

Given your problem (“whitelisted apps not launching once security kicks in”), and your need for **ODK + browser + extras**, I’d consider:

### Option A — “DO + Launcher + Restrictions, no lock task”

1. Stay as **Device Owner**.
2. Set SurveyLauncher as **HOME**.
3. Apply **user restrictions** + hide unwanted apps.
4. **Disable** `startLockTask()` completely *until* your allow-list is 100% correct.

This gives you:

* Strong policy control.
* Fewer surprises (Settings/Calendar/ODK will launch normally).
* Time to debug & perfect kiosk whitelisting.

### Option B — “Fix multi-app kiosk later”

Once everything is stable, re-introduce kiosk:

* DO + `setLockTaskPackages()` with **exact** package names for:

  * SurveyLauncher
  * ODK
  * Browser
  * Any system apps you truly need
* Then call `startLockTask()` after successful PIN, and verify via `dumpsys device_policy`.

---

If you tell me *how strict* you want the lockdown (e.g. “field staff should never reach Settings” vs “okay if supervisor can sometimes”), I can sketch a very concrete **mode matrix**:

* Mode 0: Demo (no DO, just launcher)
* Mode 1: DO + restrictions, no kiosk
* Mode 2: DO + multi-app kiosk (what we were designing)

and show you what to change in your code/policies for each.


Here’s a **minimal but realistic** Kotlin setup that does exactly what you asked:

> **Before ODK Collect, REDCap MyCap, or edu.aiims.hhlist can open, user must log in today.**
> If not logged in → redirect to login screen instead of launching the app.
> Also emits telemetry events you can later batch to your server.

I’ll give you:

1. `SessionManager` – daily login/session logic
2. `TelemetryClient` – stub where you plug your `/v1/events/batch`
3. `LoginActivity` – simple PIN login
4. `LauncherActivity` – HOME launcher that gates app launches
5. A manifest snippet so it works as launcher

You can plug this into your existing DO/kiosk project.

---

## 1️⃣ SessionManager – “have they logged in today?”

```kotlin
// SessionManager.kt
package edu.aiims.launcher.session

import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

enum class SessionKind { TEAM, SUPERVISOR }

data class SessionState(
    val isActive: Boolean,
    val kind: SessionKind?,
    val lastLoginEpochDay: Long, // LocalDate.toEpochDay()
    val expiresAtMillis: Long    // 0 = no explicit expiry
)

class SessionManager private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)

    companion object {
        @Volatile private var INSTANCE: SessionManager? = null

        fun get(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun getSession(): SessionState {
        val isActive = prefs.getBoolean("isActive", false)
        val kindStr  = prefs.getString("kind", null)
        val kind     = kindStr?.let { SessionKind.valueOf(it) }
        val lastDay  = prefs.getLong("lastLoginEpochDay", -1L)
        val exp      = prefs.getLong("expiresAtMillis", 0L)
        return SessionState(isActive, kind, lastDay, exp)
    }

    fun isSessionValidNow(): Boolean {
        val s = getSession()
        if (!s.isActive || s.kind == null) return false

        val nowMillis = System.currentTimeMillis()

        // daily check using device local date (you can swap to team timezone)
        val todayEpochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        if (s.lastLoginEpochDay < todayEpochDay) return false

        if (s.expiresAtMillis > 0 && nowMillis > s.expiresAtMillis) return false

        return true
    }

    /**
     * Start a new session valid for "hoursValid" hours, and locked to "today".
     * For a pure “once per day” rule you can set hoursValid = 24 or 0 (no time limit, just date).
     */
    fun startNewDailySession(kind: SessionKind, hoursValid: Int = 24) {
        val nowMillis = System.currentTimeMillis()
        val todayEpochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        val exp = if (hoursValid > 0) nowMillis + hoursValid * 60L * 60L * 1000L else 0L

        prefs.edit()
            .putBoolean("isActive", true)
            .putString("kind", kind.name)
            .putLong("lastLoginEpochDay", todayEpochDay)
            .putLong("expiresAtMillis", exp)
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .putBoolean("isActive", false)
            .remove("kind")
            .remove("lastLoginEpochDay")
            .remove("expiresAtMillis")
            .apply()
    }
}
```

---

## 2️⃣ TelemetryClient – stub for your `/v1/events/batch`

This just logs locally; you can later buffer and POST to your server.

```kotlin
// TelemetryClient.kt
package edu.aiims.launcher.telemetry

import android.content.Context
import android.util.Log
import edu.aiims.launcher.session.SessionKind

class TelemetryClient private constructor(context: Context) {

    companion object {
        @Volatile private var INSTANCE: TelemetryClient? = null

        fun get(context: Context): TelemetryClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TelemetryClient(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val tag = "Telemetry"

    fun logLoginSuccess(kind: SessionKind) {
        Log.d(tag, "login_success kind=$kind")
        // TODO: enqueue event for /v1/events/batch
    }

    fun logLoginFailure(reason: String) {
        Log.d(tag, "login_failure reason=$reason")
    }

    fun logAppLaunch(pkg: String, kind: SessionKind) {
        Log.d(tag, "app_launch pkg=$pkg session=$kind")
    }

    fun logAppLaunchDenied(pkg: String, reason: String) {
        Log.d(tag, "app_launch_denied pkg=$pkg reason=$reason")
    }
}
```

---

## 3️⃣ LoginActivity – simple PIN gate

This is where you verify the team/supervisor PIN (you can wire it to your Argon2 verification later).

```kotlin
// LoginActivity.kt
package edu.aiims.launcher.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import edu.aiims.launcher.R
import edu.aiims.launcher.session.SessionKind
import edu.aiims.launcher.session.SessionManager
import edu.aiims.launcher.telemetry.TelemetryClient

class LoginActivity : Activity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var telemetry: TelemetryClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sessionManager = SessionManager.get(this)
        telemetry = TelemetryClient.get(this)

        val pinInput = findViewById<EditText>(R.id.input_pin)
        val teamRadio = findViewById<RadioButton>(R.id.radio_team)
        val supervisorRadio = findViewById<RadioButton>(R.id.radio_supervisor)
        val btnLogin = findViewById<Button>(R.id.btn_login)

        btnLogin.setOnClickListener {
            val pin = pinInput.text.toString().trim()
            val kind = if (supervisorRadio.isChecked) SessionKind.SUPERVISOR else SessionKind.TEAM

            if (pin.isEmpty()) {
                Toast.makeText(this, "Enter PIN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (verifyPin(kind, pin)) {
                sessionManager.startNewDailySession(
                    kind = kind,
                    hoursValid = if (kind == SessionKind.SUPERVISOR) 12 else 24
                )
                telemetry.logLoginSuccess(kind)
                setResult(RESULT_OK)
                finish()
            } else {
                telemetry.logLoginFailure("mismatch")
                Toast.makeText(this, "Invalid PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * TODO: Replace with your real PIN verification:
     * - Offline verify against Argon2 hash from policy, OR
     * - Optional online /v1/pin/verify.
     */
    private fun verifyPin(kind: SessionKind, pin: String): Boolean {
        // Dummy example: 1111 for TEAM, 9999 for SUPERVISOR
        return when (kind) {
            SessionKind.TEAM -> pin == "1111"
            SessionKind.SUPERVISOR -> pin == "9999"
        }
    }
}
```

Simple layout example (`res/layout/activity_login.xml`):

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:orientation="vertical"
    android:padding="24dp"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <RadioGroup
        android:id="@+id/radio_group"
        android:orientation="horizontal"
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <RadioButton
            android:id="@+id/radio_team"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:checked="true"
            android:text="Team" />

        <RadioButton
            android:id="@+id/radio_supervisor"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Supervisor" />
    </RadioGroup>

    <EditText
        android:id="@+id/input_pin"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="numberPassword"
        android:hint="Enter PIN" />

    <Button
        android:id="@+id/btn_login"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Login" />
</LinearLayout>
```

---

## 4️⃣ LauncherActivity – gate all app launches

This acts as the HOME launcher. It checks session validity on **resume** and redirects to Login if needed.
Buttons for ODK, MyCap, edu.aiims.hhlist all go through the same `launchAllowedApp` function.

```kotlin
// LauncherActivity.kt
package edu.aiims.launcher.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import edu.aiims.launcher.R
import edu.aiims.launcher.session.SessionKind
import edu.aiims.launcher.session.SessionManager
import edu.aiims.launcher.telemetry.TelemetryClient

class LauncherActivity : Activity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var telemetry: TelemetryClient

    // TODO: confirm these package names with `adb shell pm list packages`
    private val PKG_ODK = "org.odk.collect.android"
    private val PKG_MYCAP = "edu.aiims.mycap"        // <-- put real MyCap package here
    private val PKG_HHLIST = "edu.aiims.hhlist"      // your app

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        sessionManager = SessionManager.get(this)
        telemetry = TelemetryClient.get(this)

        val btnOdk   = findViewById<Button>(R.id.btn_odk)
        val btnMyCap = findViewById<Button>(R.id.btn_mycap)
        val btnHH    = findViewById<Button>(R.id.btn_hhlist)
        val btnRelogin = findViewById<Button>(R.id.btn_relogin)

        btnOdk.setOnClickListener { launchAllowedApp(PKG_ODK) }
        btnMyCap.setOnClickListener { launchAllowedApp(PKG_MYCAP) }
        btnHH.setOnClickListener { launchAllowedApp(PKG_HHLIST) }

        btnRelogin.setOnClickListener {
            // allow manual re-login (e.g. supervisor override)
            openLogin()
        }
    }

    override fun onResume() {
        super.onResume()

        if (!sessionManager.isSessionValidNow()) {
            openLogin()
        }
    }

    private fun openLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    private fun launchAllowedApp(pkg: String) {
        val session = sessionManager.getSession()
        if (!sessionManager.isSessionValidNow() || session.kind == null) {
            telemetry.logAppLaunchDenied(pkg, "no_active_session")
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            openLogin()
            return
        }

        val pm: PackageManager = packageManager
        val launchIntent = pm.getLaunchIntentForPackage(pkg)

        if (launchIntent == null) {
            telemetry.logAppLaunchDenied(pkg, "no_launch_intent")
            Toast.makeText(this, "App not installed", Toast.LENGTH_SHORT).show()
            return
        }

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(launchIntent)
            telemetry.logAppLaunch(pkg, session.kind)
        } catch (e: Exception) {
            telemetry.logAppLaunchDenied(pkg, "start_failed:${e.javaClass.simpleName}")
            Toast.makeText(this, "Unable to start app", Toast.LENGTH_SHORT).show()
        }
    }
}
```

Example launcher layout (`res/layout/activity_launcher.xml`):

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:orientation="vertical"
    android:padding="24dp"
    android:gravity="center_horizontal"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <Button
        android:id="@+id/btn_odk"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Open ODK Collect" />

    <Button
        android:id="@+id/btn_mycap"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Open REDCap MyCap" />

    <Button
        android:id="@+id/btn_hhlist"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Open Household List" />

    <Button
        android:id="@+id/btn_relogin"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Relogin / Change user" />
</LinearLayout>
```

---

## 5️⃣ Manifest snippet – make this the HOME launcher

You probably already have this, but just to be explicit:

```xml
<!-- AndroidManifest.xml -->
<application
    ...>

    <!-- Your launcher -->
    <activity
        android:name=".ui.LauncherActivity"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.HOME" />
            <category android:name="android.intent.category.LAUNCHER" />
            <category android:name="android.intent.category.DEFAULT" />
        </intent-filter>
    </activity>

    <!-- Login activity (no launcher icon) -->
    <activity
        android:name=".ui.LoginActivity"
        android:exported="false" />
</application>
```

If you’re already Device Owner, Android will use this as the home/launcher after provisioning.

---

## How this meets your requirement

* **Daily login enforced**
  `SessionManager.isSessionValidNow()` returns `false` if:

  * No session
  * Last login not today
  * Supervisor session expired

* **No app can launch before login**
  All app buttons call `launchAllowedApp()`. If no active session → you go to `LoginActivity` instead of launching ODK/MyCap/HHList.

* **Telemetry integrated**
  You have hooks at:

  * `logLoginSuccess / logLoginFailure`
  * `logAppLaunch / logAppLaunchDenied`

You can now wire `TelemetryClient` to your actual `/v1/events/batch` implementation.

---

If you want next, I can:

* Replace the dummy `verifyPin()` with a real offline verifier using your Argon2 PHC from policy JSON, or
* Add the local queue + background worker that flushes telemetry to your SvelteKit backend with exponential backoff.

Short answer:

* **On a normal Android device:** yes, users *can* often sneak into ODK via Settings/Play Store/Recents.
* **On your Device Owner fleet, configured properly:** you can make it so **they effectively cannot**, because those other launch paths don’t exist anymore.

Let’s break it down in the context of the code I just gave you.

---

## 1. With *only* the launcher logic I gave

If you **only** implement:

* `LauncherActivity` as HOME, and
* `launchAllowedApp()` that checks the session before opening ODK/MyCap/HHList,

then:

* From **your launcher buttons** → ODK **cannot** open without PIN (good ✅).
* But Android **still has other ways** to start ODK:

  * Settings → Apps → ODK → **Open**
  * Google Play Store → My apps → ODK → **Open**
  * Another launcher (if installed)
  * A notification or deep link from some other app

So on a generic device: **yes, they could bypass your PIN** via those paths.

That’s why for a *secure* deployment we always combine:

> **Business rule in the launcher** **+** **Device Owner restrictions & app hiding**

---

## 2. With Device Owner properly configured (what you actually want)

On your SurveyLauncher tablets you will be:

* **Device Owner**
* **Your launcher = only visible launcher**
* **Play Store & Settings either hidden or heavily restricted**

In that setup, you lock down the alternative launch paths:

### 🔒 Block / hide Play Store

As DO, you can:

* Completely remove Play Store from the user’s view:

  * Factory image without Play Store **or**
  * `setApplicationHidden(admin, "com.android.vending", true)`
    (package name may vary, but typically `com.android.vending`)

Result:

> There is **no Play Store icon**, no “Open” shortcut → user cannot launch ODK from Play Store.

---

### 🔒 Control Settings access

Options (pick how strict you want to be):

1. **Hard lock:** Hide Settings completely for field team

   * Don’t whitelist `com.android.settings` in kiosk
   * Use user restrictions (e.g. `DISALLOW_SETTINGS`, or granular `DISALLOW_CONFIG_WIFI` etc. on supported builds)
   * Provide only a **supervisor-only “Settings” button** *inside your launcher* that:

     * asks for supervisor PIN
     * then fires a `startActivity(Intent(Settings.ACTION_SETTINGS))`

   In everyday team use, there’s **no Settings icon & no way** to tap “Open ODK” from Settings.

2. **Soft access:** Limited settings but *no app-open paths*

   * You allow a restricted “Settings”-like screen via your own UI
   * But you don’t give them the generic “Apps → ODK → Open” path.

Either way, the idea is:

> The **only** visible/usable way to start ODK is your launcher button → which is guarded by the session/PIN check.

---

### 🔒 One launcher, no escape

* Only your launcher is installed/visible as HOME.
* No third-party launchers available.
* Recents / system UI are limited by:

  * **Lock Task mode** (when you enable kiosk again), or
  * At least DO restrictions + your UI design.

So users cannot switch to some other launcher and open ODK from there.

---

## 3. So, practically: can they open ODK without PIN?

If you:

1. Keep **SurveyLauncher as the only visible launcher**,
2. Hide/disable **Play Store** for field users,
3. Hide or strictly gate **Settings** behind your own supervisor PIN UI,
4. Route **all ODK/MyCap/HHList launches** through `launchAllowedApp()` (session check),

then **for all practical purposes**:

> ❌ No, users cannot launch ODK without doing a PIN login first.

Any theoretical path (Settings “Open”, Play Store “Open”, another launcher) is simply **not reachable** in your DO setup.

---

If you tell me:

* *“I want supervisors to be able to open full Settings sometimes, but team members never should”*

I can sketch a concrete flow:

* Supervisor PIN → temporary “admin mode” in launcher → enable Settings button for 10–15 minutes → revert.
