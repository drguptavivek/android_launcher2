package com.example.launcher.util

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.UserManager
import com.example.launcher.admin.LauncherAdminReceiver

class KioskManager(private val context: Context) {

    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(context, LauncherAdminReceiver::class.java)

    fun isDeviceOwner(): Boolean {
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    fun enableKioskMode(activity: Activity) {
        if (isDeviceOwner()) {
            // Set this app as the lock task package
            val packages = arrayOf(context.packageName)
            dpm.setLockTaskPackages(adminComponent, packages)
            
            // Start lock task mode
            if (activity.getSystemService(Activity.ACTIVITY_SERVICE) != null) {
                activity.startLockTask()
            }
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

    fun setSystemRestrictions(enable: Boolean) {
        if (isDeviceOwner()) {
            if (enable) {
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_ADD_USER)
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_ADJUST_VOLUME)
            } else {
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_ADD_USER)
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_ADJUST_VOLUME)
            }
        }
    }
}
