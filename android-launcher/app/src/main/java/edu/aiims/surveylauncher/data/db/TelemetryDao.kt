package edu.aiims.surveylauncher.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TelemetryDao {
    @Insert
    suspend fun insert(event: TelemetryEntity)

    @Insert
    suspend fun insertAll(events: List<TelemetryEntity>)

    @Query("SELECT * FROM telemetry_events ORDER BY timestamp ASC")
    suspend fun getAllEvents(): List<TelemetryEntity>

    @Delete
    suspend fun delete(events: List<TelemetryEntity>)
    
    @Query("DELETE FROM telemetry_events")
    suspend fun clearAll()
}
