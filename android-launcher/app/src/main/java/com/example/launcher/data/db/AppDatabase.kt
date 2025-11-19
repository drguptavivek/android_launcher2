package com.example.launcher.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TelemetryEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun telemetryDao(): TelemetryDao
}
