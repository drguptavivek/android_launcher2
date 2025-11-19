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
}
