package com.example.launcher.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.launcher.LauncherApplication
import com.example.launcher.data.SessionManager
import com.example.launcher.data.db.TelemetryEntity
import com.example.launcher.data.network.ApiService
import com.example.launcher.data.network.TelemetryEvent
import com.example.launcher.data.network.TelemetryRequest
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class TelemetryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val sessionManager = SessionManager(context)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    override suspend fun doWork(): Result {
        val user = sessionManager.getUser() ?: return Result.success()
        val deviceId = sessionManager.getDeviceId() ?: android.os.Build.MODEL

        val events = mutableListOf<TelemetryEvent>()

        // 1. Collect Location
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val location: Location? = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    events.add(
                        TelemetryEvent(
                            type = "LOCATION",
                            data = mapOf(
                                "lat" to location.latitude,
                                "lng" to location.longitude,
                                "acc" to location.accuracy
                            ),
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Collect Usage Stats
        val usageStatsManager = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 60 * 24 // Last 24 hours

        val usageStatsList = usageStatsManager.queryUsageStats(
            android.app.usage.UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        if (usageStatsList != null && usageStatsList.isNotEmpty()) {
            val sortedStats = usageStatsList.sortedByDescending { it.totalTimeInForeground }
            val topApps = sortedStats.take(5).map { 
                mapOf(
                    "packageName" to it.packageName,
                    "totalTime" to it.totalTimeInForeground
                )
            }
            
            if (topApps.isNotEmpty()) {
                events.add(
                    TelemetryEvent(
                        type = "APP_USAGE",
                        data = topApps,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        // 3. Save to Local DB (Offline First)
        val application = applicationContext as LauncherApplication
        val database = application.database
        val gson = Gson()

        if (events.isNotEmpty()) {
            val entities = events.map { event ->
                TelemetryEntity(
                    type = event.type,
                    dataJson = gson.toJson(event.data),
                    timestamp = event.timestamp
                )
            }
            database.telemetryDao().insertAll(entities)
        }

        // 4. Sync with API (Send all unsent events)
        val allEvents = database.telemetryDao().getAllEvents()
        
        if (allEvents.isNotEmpty()) {
            val sessionManager = SessionManager(applicationContext)
            val user = sessionManager.getUser()
            
            if (user != null) {
                try {
                    val apiEvents = allEvents.map { entity ->
                        val data: Any = if (entity.dataJson.trim().startsWith("[")) {
                            gson.fromJson(entity.dataJson, List::class.java)
                        } else {
                            gson.fromJson(entity.dataJson, Map::class.java)
                        }

                        TelemetryEvent(
                            type = entity.type,
                            data = data,
                            timestamp = entity.timestamp
                        )
                    }

                    val retrofit = Retrofit.Builder()
                        .baseUrl("http://localhost:5173/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    
                    val api = retrofit.create(ApiService::class.java)
                    
                    val request = TelemetryRequest(
                        userId = user.id,
                        deviceId = sessionManager.getDeviceId() ?: android.os.Build.MODEL,
                        events = apiEvents
                    )
                    
                    val response = api.sendTelemetry(request)
                    
                    if (response.success) {
                        // Clear synced events from DB
                        database.telemetryDao().delete(allEvents)
                        Log.d("TelemetryWorker", "Successfully synced ${allEvents.size} events")
                    } else {
                        Log.e("TelemetryWorker", "Failed to sync telemetry: ${response.error}")
                    }
                } catch (e: Exception) {
                    Log.e("TelemetryWorker", "Error syncing telemetry", e)
                    // Keep events in DB for next retry
                }
            }
        }

        return Result.success()
    }
}
