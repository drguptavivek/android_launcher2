package edu.aiims.surveylauncher.data

import android.content.Context
import android.content.SharedPreferences
import edu.aiims.surveylauncher.data.network.UserData
import com.google.gson.Gson

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_USER = "user"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_DESCRIPTION = "device_description"
        private const val KEY_REGISTRATION_TIMESTAMP = "registration_timestamp"
        private const val KEY_IS_REGISTERED = "is_registered"
        private const val KEY_POLICY = "policy_config"
        private const val KEY_THEME = "theme"
        private const val KEY_POLICY_NAME = "policy_name"
    }

    fun saveUser(user: UserData) {
        val json = gson.toJson(user)
        prefs.edit().putString(KEY_USER, json).apply()
    }

    fun getUser(): UserData? {
        val json = prefs.getString(KEY_USER, null)
        return if (json != null) {
            gson.fromJson(json, UserData::class.java)
        } else {
            null
        }
    }

    fun clearUser() {
        prefs.edit().remove(KEY_USER).apply()
    }

    fun saveDeviceId(deviceId: String) {
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
    }

    fun getDeviceId(): String? {
        return prefs.getString(KEY_DEVICE_ID, null)
    }

    fun saveDeviceRegistration(deviceId: String, description: String, registeredAt: String) {
        prefs.edit()
            .putString(KEY_DEVICE_ID, deviceId)
            .putString(KEY_DEVICE_DESCRIPTION, description)
            .putString(KEY_REGISTRATION_TIMESTAMP, registeredAt)
            .putBoolean(KEY_IS_REGISTERED, true)
            .apply()
    }

    fun isDeviceRegistered(): Boolean {
        return prefs.getBoolean(KEY_IS_REGISTERED, false)
    }

    fun getDeviceDescription(): String? {
        return prefs.getString(KEY_DEVICE_DESCRIPTION, null)
    }

    fun clearDeviceRegistration() {
        prefs.edit()
            .remove(KEY_DEVICE_ID)
            .remove(KEY_DEVICE_DESCRIPTION)
            .remove(KEY_REGISTRATION_TIMESTAMP)
            .remove(KEY_IS_REGISTERED)
            .apply()
    }

    fun savePolicy(policyJson: String) {
        prefs.edit().putString(KEY_POLICY, policyJson).apply()
    }

    fun getPolicy(): String? {
        return prefs.getString(KEY_POLICY, null)
    }

    fun saveTheme(themeKey: String) {
        prefs.edit().putString(KEY_THEME, themeKey).apply()
    }

    fun getTheme(): String? {
        return prefs.getString(KEY_THEME, null)
    }

    fun savePolicyName(name: String) {
        prefs.edit().putString(KEY_POLICY_NAME, name).apply()
    }

    fun getPolicyName(): String? {
        return prefs.getString(KEY_POLICY_NAME, null)
    }
}
