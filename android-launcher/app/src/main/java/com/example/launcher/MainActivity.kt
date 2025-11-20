
package com.example.launcher

import android.Manifest
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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
import com.example.launcher.ui.pin.PinChangeScreen
import com.example.launcher.ui.home.AppDrawer
import com.example.launcher.util.KioskManager
import com.example.launcher.util.PinManager
import com.example.launcher.worker.TelemetryWorker
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import coil.request.ImageRequest
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
                // Theme selection (persisted)
                var colorTheme by remember { mutableStateOf(sessionManager.getTheme() ?: "deepBlue") }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = when (colorTheme) {
                                    "sunset" -> listOf(Color(0xFF43114B), Color(0xFF763462), Color(0xFFB05C5E))
                                    "forest" -> listOf(Color(0xFF0B2E26), Color(0xFF14593B), Color(0xFF2A7D4A))
                                    else -> listOf(Color(0xFF0F1F3A), Color(0xFF0A3342), Color(0xFF123B5A))
                                }
                            )
                        )
                ) {
                    val painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data("file:///android_asset/aiims_logo_currentcolor.svg")
                            .decoderFactory(SvgDecoder.Factory())
                            .build()
                    )
                    Image(
                        painter = painter,
                        contentDescription = "AIIMS Logo Backdrop",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxSize(0.7f)
                            .graphicsLayer { alpha = 0.16f },
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(Color(0xFF7FC8FF).copy(alpha = 0.22f))
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        tonalElevation = 12.dp,
                        shape = MaterialTheme.shapes.large,
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                    // Load initial state from session
                    var isDeviceRegistered by remember { mutableStateOf(sessionManager.isDeviceRegistered()) }
                    var isPinSet by remember { mutableStateOf(pinManager.isPinSet()) }
                    var currentUser by remember { mutableStateOf(sessionManager.getUser()) }
                    var isLoggedIn by remember { mutableStateOf(currentUser != null) }
                    var currentScreen by remember { mutableStateOf("HOME") } // HOME, SETTINGS, PIN_CHANGE
                    var colorTheme by remember { mutableStateOf(sessionManager.getTheme() ?: "deepBlue") }

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
                                },
                                onChangePin = {
                                    currentScreen = "PIN_CHANGE"
                                },
                                currentTheme = colorTheme,
                                onThemeChange = { theme ->
                                    colorTheme = theme
                                    sessionManager.saveTheme(theme)
                                },
                                onBackToHome = {
                                    kioskManager.stopKioskMode(this@MainActivity)
                                    currentScreen = "HOME"
                                }
                            )
                        } else if (currentScreen == "PIN_CHANGE") {
                            PinChangeScreen(
                                onComplete = { currentScreen = "SETTINGS" },
                                onCancel = { currentScreen = "SETTINGS" }
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

            // Fallback to the fixed allow-list when no valid policy is present
            if (allowed.isEmpty()) {
                allowed = KioskManager.DEFAULT_ALLOWED_PACKAGES
                android.util.Log.d("MainActivity", "Using fixed allow-list: $allowed")
            }

            // Automatically allow any installed edu.aiims.* apps
            val aiimsPackages = getAiimsPackages(this)
            if (aiimsPackages.isNotEmpty()) {
                allowed = (allowed + aiimsPackages).distinct()
                android.util.Log.d("MainActivity", "Added edu.aiims.* packages: $aiimsPackages")
            }

            // Always refresh the allow-list even if already pinned.
            kioskManager.applyLockTaskAllowList(allowed)

            // Enable kiosk mode and restrictions.
            // Important: setLockTaskPackages must include all whitelisted apps BEFORE startLockTask().
            try {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val lockTaskModeState = activityManager.lockTaskModeState
                
                if (lockTaskModeState == android.app.ActivityManager.LOCK_TASK_MODE_NONE) {
                    kioskManager.enableKioskMode(this, allowed)
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to enable kiosk mode", e)
            }
            
            kioskManager.setSystemRestrictions(true)
        }
    }

    private fun getAiimsPackages(context: Context): List<String> {
        return try {
            context.packageManager.getInstalledPackages(0)
                .map { it.packageName }
                .filter { it.startsWith("edu.aiims.") }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error collecting edu.aiims.* packages", e)
            emptyList()
        }
    }
}
