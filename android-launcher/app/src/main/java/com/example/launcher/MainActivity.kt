
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
import com.example.launcher.ui.registration.RegistrationScreen
import com.example.launcher.ui.login.LoginScreen
import com.example.launcher.ui.settings.SettingsScreen
import com.example.launcher.ui.pin.PinSetupScreen
import com.example.launcher.ui.pin.PinLockScreen
import com.example.launcher.ui.home.AppDrawer
import com.example.launcher.util.KioskManager
import com.example.launcher.util.PinManager
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
        val kioskManager = KioskManager(this)
        val pinManager = PinManager(this)
        
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
                    var isDeviceRegistered by remember { mutableStateOf(sessionManager.isDeviceRegistered()) }
                    var isPinSet by remember { mutableStateOf(pinManager.isPinSet()) }
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

                    if (!isDeviceRegistered) {
                        RegistrationScreen(
                            onRegistrationSuccess = {
                                isDeviceRegistered = true
                            }
                        )
                    } else if (!isPinSet && isLoggedIn) {
                        // Show PIN setup after first login
                        PinSetupScreen(
                            onPinSet = {
                                isPinSet = true
                            }
                        )
                    } else if (isLoggedIn && currentUser != null) {
                        if (currentScreen == "SETTINGS") {
                            SettingsScreen(
                                currentUser = currentUser,
                                onBack = { currentScreen = "HOME" },
                                onLogout = {
                                    // Trigger logout API call
                                    val userId = currentUser?.id ?: ""
                                    val deviceId = sessionManager.getDeviceId() ?: android.os.Build.MODEL
                                    
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
                                },
                                onReset = {
                                    sessionManager.clearUser()
                                    sessionManager.clearDeviceRegistration()
                                    isLoggedIn = false
                                    currentUser = null
                                    isDeviceRegistered = false
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
                                                    text = "${sessionManager.getDeviceDescription() ?: android.os.Build.MODEL} • Login: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(currentUser?.loginTime ?: 0))}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        },
                                        actions = {
                                            IconButton(onClick = { currentScreen = "SETTINGS" }) {
                                                Icon(Icons.Filled.Settings, contentDescription = "Settings")
                                            }
                                        }
                                    )
                                }
                            ) { padding ->
                                Box(
                                    modifier = Modifier
                                        .padding(padding)
                                        .fillMaxSize()
                                ) {
                                    AppDrawer()
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

    override fun onResume() {
        super.onResume()
        
        // Set up lock task whitelist if device is Device Owner
        val kioskManager = KioskManager(this)
        if (kioskManager.isDeviceOwner()) {
            // Load policy and set allowed apps
            val sessionManager = SessionManager(this)
            val policyJson = sessionManager.getPolicy()
            var allowed = emptyList<String>()
            
            if (policyJson != null) {
                try {
                    val gson = com.google.gson.Gson()
                    val policy = gson.fromJson(policyJson, com.example.launcher.data.network.PolicyConfig::class.java)
                    allowed = policy.allowedApps
                    android.util.Log.d("MainActivity", "Policy whitelist: $allowed")
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error parsing policy", e)
                }
            }
            
            // Reset whitelist with the latest allowed apps
            kioskManager.resetLockTaskPackages(allowed)
            
            // NOTE: Kiosk mode is NOT automatically enabled here
            // User can enable it manually from Settings
            // kioskManager.enableKioskMode(this)
            kioskManager.setSystemRestrictions(true)
        }
    }
}
