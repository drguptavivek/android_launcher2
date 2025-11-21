package edu.aiims.surveylauncher

import android.app.Application
import androidx.room.Room
import edu.aiims.surveylauncher.data.db.AppDatabase

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
