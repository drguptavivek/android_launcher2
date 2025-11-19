package com.example.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.example.launcher.ui.registration.RegistrationScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isLoggedIn by remember { mutableStateOf(false) }
                    var currentUser by remember { mutableStateOf<com.example.launcher.data.network.UserData?>(null) }

                    if (isLoggedIn && currentUser != null) {
                        // Show Launcher UI (Placeholder for now)
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.foundation.layout.Column(
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                            ) {
                                androidx.compose.material3.Card(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    androidx.compose.foundation.layout.Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                                    ) {
                                        androidx.compose.material3.Text(
                                            text = "User: ${currentUser?.username}",
                                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                                        )
                                        androidx.compose.material3.Text(
                                            text = "Device: ${android.os.Build.MODEL}",
                                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                                
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(32.dp))
                                
                                androidx.compose.material3.Text("Welcome to Launcher Home!")
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                                androidx.compose.material3.Button(onClick = { 
                                    // Trigger logout API call
                                    val userId = currentUser?.id ?: ""
                                    val deviceId = android.os.Build.MODEL
                                    
                                    // Launch coroutine to call logout
                                    // Note: In a real app, this should be in a ViewModel
                                    kotlinx.coroutines.GlobalScope.launch {
                                        try {
                                            val retrofit = retrofit2.Retrofit.Builder()
                                                .baseUrl("http://localhost:5173/")
                                                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                                                .build()
                                            val api = retrofit.create(com.example.launcher.data.network.ApiService::class.java)
                                            api.logout(com.example.launcher.data.network.LogoutRequest(userId, deviceId))
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }

                                    isLoggedIn = false 
                                    currentUser = null
                                }) {
                                    androidx.compose.material3.Text("Logout")
                                }
                            }
                        }
                    } else {
                        com.example.launcher.ui.login.LoginScreen(
                            onLoginSuccess = { user ->
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
