
package com.example.launcher

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.launcher.data.SessionManager
import com.example.launcher.data.network.ApiService
import com.example.launcher.data.network.LogoutRequest
import com.example.launcher.data.network.UserData
import com.example.launcher.ui.login.LoginScreen
import com.example.launcher.worker.TelemetryWorker
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager(this)
        
        // Request permissions
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        requestPermissions(permissions, 0)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Load initial state from session
                    var currentUser by remember { mutableStateOf(sessionManager.getUser()) }
                    var isLoggedIn by remember { mutableStateOf(currentUser != null) }

                    // Effect to schedule worker when logged in
                    LaunchedEffect(isLoggedIn) {
                        if (isLoggedIn) {
                            val workRequest = PeriodicWorkRequestBuilder<TelemetryWorker>(
                                15, TimeUnit.MINUTES
                            ).build()
                            
                            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                                "telemetry_work",
                                ExistingPeriodicWorkPolicy.UPDATE,
                                workRequest
                            )
                        } else {
                            WorkManager.getInstance(applicationContext).cancelUniqueWork("telemetry_work")
                        }
                    }

                    if (isLoggedIn && currentUser != null) {
                        // Show Launcher UI (Placeholder for now)
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Card(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "User: ${currentUser?.username}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Device: ${android.os.Build.MODEL}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(32.dp))
                                
                                // Usage Stats Permission Check
                                val hasUsageStats = remember {
                                    val appOps = getSystemService(android.content.Context.APP_OPS_SERVICE) as android.app.AppOpsManager
                                    val mode = appOps.checkOpNoThrow(
                                        android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                                        android.os.Process.myUid(),
                                        packageName
                                    )
                                    mode == android.app.AppOpsManager.MODE_ALLOWED
                                }

                                if (!hasUsageStats) {
                                    Button(onClick = {
                                        startActivity(android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                    }) {
                                        Text("Grant Usage Access")
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                Text("Welcome to Launcher Home!")
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Manual Telemetry Trigger (For Testing)
                                Button(onClick = {
                                    val workRequest = androidx.work.OneTimeWorkRequestBuilder<TelemetryWorker>().build()
                                    WorkManager.getInstance(applicationContext).enqueue(workRequest)
                                }) {
                                    Text("Send Telemetry Now")
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))

                                Button(onClick = { 
                                    // Trigger logout API call
                                    val userId = currentUser?.id ?: ""
                                    val deviceId = android.os.Build.MODEL
                                    
                                    // Launch coroutine to call logout
                                    GlobalScope.launch {
                                        try {
                                            val retrofit = Retrofit.Builder()
                                                .baseUrl("http://localhost:5173/")
                                                .addConverterFactory(GsonConverterFactory.create())
                                                .build()
                                            val api = retrofit.create(ApiService::class.java)
                                            api.logout(LogoutRequest(userId, deviceId))
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }

                                    // Clear session
                                    sessionManager.clearUser()
                                    isLoggedIn = false 
                                    currentUser = null
                                }) {
                                    Text("Logout")
                                }
                            }
                        }
                    } else {
                        LoginScreen(
                            onLoginSuccess = { user ->
                                sessionManager.saveUser(user)
                                currentUser = user
                                isLoggedIn = true
                            }
                        )
                    }
                }
            }
        }
    }
}
