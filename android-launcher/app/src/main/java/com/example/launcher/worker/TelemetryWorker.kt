package com.example.launcher.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.launcher.data.SessionManager
import com.example.launcher.data.network.ApiService
import com.example.launcher.data.network.TelemetryEvent
import com.example.launcher.data.network.TelemetryRequest
import com.google.android.gms.location.LocationServices
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

        // 2. Collect Usage Stats (Placeholder for now)
        // ...

        // 3. Send to API
        if (events.isNotEmpty()) {
            try {
                // Note: In a real app, use DI for Retrofit
                val retrofit = Retrofit.Builder()
                    .baseUrl("http://localhost:5173/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                
                val api = retrofit.create(ApiService::class.java)
                
                api.sendTelemetry(
                    TelemetryRequest(
                        userId = user.id,
                        deviceId = deviceId,
                        events = events
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                return Result.retry()
            }
        }

        return Result.success()
    }
}
