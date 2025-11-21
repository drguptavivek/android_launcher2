package edu.aiims.surveylauncher.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telemetry_events")
data class TelemetryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val dataJson: String, // Store complex data as JSON string
    val timestamp: Long
)
