package com.example.launcher

import android.app.Application
import androidx.room.Room
import com.example.launcher.data.db.AppDatabase

class LauncherApplication : Application() {
    lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "launcher-db"
        ).build()
    }
}
