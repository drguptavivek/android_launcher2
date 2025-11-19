
package com.example.launcher

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import com.example.launcher.ui.settings.SettingsScreen
import com.example.launcher.worker.TelemetryWorker
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
                    var currentScreen by remember { mutableStateOf("HOME") } // HOME, SETTINGS

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
                        if (currentScreen == "SETTINGS") {
                            SettingsScreen(
                                currentUser = currentUser,
                                onBack = { currentScreen = "HOME" },
                                onLogout = {
                                    // Trigger logout API call
                                    val userId = currentUser?.id ?: ""
                                    val deviceId = android.os.Build.MODEL
                                    
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

                                    sessionManager.clearUser()
                                    isLoggedIn = false 
                                    currentUser = null
                                    currentScreen = "HOME"
                                }
                            )
                        } else {
                            // HOME SCREEN
                            Scaffold(
                                topBar = {
                                    @OptIn(ExperimentalMaterial3Api::class)
                                    TopAppBar(
                                        title = {
                                            Column {
                                                Text(
                                                    text = currentUser?.username ?: "",
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    text = "${android.os.Build.MODEL} • Login: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(currentUser?.loginTime ?: 0))}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        },
                                        actions = {
                                            IconButton(onClick = { currentScreen = "SETTINGS" }) {
                                                Icon(androidx.compose.material.icons.Icons.Default.Settings, contentDescription = "Settings")
                                            }
                                        }
                                    )
                                }
                            ) { padding ->
                                Box(
                                    modifier = Modifier
                                        .padding(padding)
                                        .fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Welcome to Launcher Home!")
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
