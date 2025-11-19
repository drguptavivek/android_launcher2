package com.example.launcher.data

import android.content.Context
import android.content.SharedPreferences
import com.example.launcher.data.network.UserData
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
}
