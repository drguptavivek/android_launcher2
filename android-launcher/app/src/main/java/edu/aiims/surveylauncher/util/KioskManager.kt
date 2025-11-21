package edu.aiims.surveylauncher.util

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.UserManager
import edu.aiims.surveylauncher.MainActivity
import edu.aiims.surveylauncher.admin.LauncherAdminReceiver

class KioskManager(private val context: Context) {

    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(context, LauncherAdminReceiver::class.java)

    companion object {
        // Default multi-app kiosk allow-list used when no remote policy is present
        val DEFAULT_ALLOWED_PACKAGES = listOf(
            "org.odk.collect.android",
            "com.whatsapp",
            "com.whatsapp.w4b",
            "edu.vanderbilt.redcap",
            "com.android.settings",
            "com.google.android.settings",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.android.messaging",
            "com.google.android.apps.messaging",
            "com.google.android.gm",
            "org.mozilla.firefox",
            "com.android.chrome"
        )
    }

    fun isDeviceOwner(): Boolean {
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    fun applyLockTaskAllowList(allowedPackages: List<String>) {
        if (!isDeviceOwner()) return

        // Ensure the allow-list includes launcher + any policy packages before starting lock task
        val whitelist = (allowedPackages + context.packageName).toSet().toTypedArray()
        dpm.setLockTaskPackages(adminComponent, whitelist)
        android.util.Log.d("KioskManager", "Applied lock task allow-list: ${whitelist.joinToString()}")
    }

    fun enableKioskMode(activity: Activity, allowedPackages: List<String> = emptyList()) {
        if (!isDeviceOwner()) return

        applyLockTaskAllowList(allowedPackages)

        // Start lock task mode
        if (activity.getSystemService(Activity.ACTIVITY_SERVICE) != null) {
            activity.startLockTask()
        }
    }

    fun stopKioskMode(activity: Activity) {
        if (isDeviceOwner()) {
            activity.stopLockTask()
        }
    }

    fun setAllowedApps(packageNames: List<String>) {
        if (isDeviceOwner()) {
            // Always include our own package, use Set to avoid duplicates
            val allPackages = (packageNames + context.packageName).toSet().toTypedArray()
            dpm.setLockTaskPackages(adminComponent, allPackages)
        }
    }

    fun resetLockTaskPackages(allowed: List<String>) {
        if (isDeviceOwner()) {
            // First clear any existing whitelist
            dpm.setLockTaskPackages(adminComponent, arrayOf())
            // Then set the new list (include our own launcher)
            val all = (allowed + context.packageName).toSet().toTypedArray()
            dpm.setLockTaskPackages(adminComponent, all)
            android.util.Log.d("KioskManager", "LockTask whitelist reset: ${all.joinToString()}")
        }
    }

    fun lockDeviceNow() {
        if (isDeviceOwner()) {
            try {
                dpm.lockNow()
                android.util.Log.d("KioskManager", "Device locked via DPM")
            } catch (e: Exception) {
                android.util.Log.e("KioskManager", "Failed to lock device", e)
            }
        }
    }

    fun setSystemRestrictions(enable: Boolean) {
        if (isDeviceOwner()) {
            if (enable) {
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_ADD_USER)
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_ADJUST_VOLUME)
                
                // Force this app to be the default HOME app
                // Only apply if not already set to avoid restart loops
                if (!isDefaultHomeApp()) {
                    val intentFilter = IntentFilter(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        addCategory(Intent.CATEGORY_DEFAULT)
                    }
                    dpm.addPersistentPreferredActivity(
                        adminComponent, 
                        intentFilter, 
                        ComponentName(context.packageName, MainActivity::class.java.name)
                    )
                    android.util.Log.d("KioskManager", "Set as default home app")
                }
            } else {
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_ADD_USER)
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_ADJUST_VOLUME)
                
                // Clear default home app enforcement
                dpm.clearPackagePersistentPreferredActivities(adminComponent, context.packageName)
            }
        }
    }

    private fun isDefaultHomeApp(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        val currentHome = resolveInfo?.activityInfo?.packageName
        android.util.Log.d("KioskManager", "Current home app: $currentHome, My package: ${context.packageName}")
        return currentHome == context.packageName
    }
}
