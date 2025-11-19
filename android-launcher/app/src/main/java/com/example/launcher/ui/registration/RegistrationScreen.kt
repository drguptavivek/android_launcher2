package com.example.launcher.ui.registration

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.launcher.data.network.ApiService
import com.example.launcher.data.network.RegistrationRequest
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Composable
fun RegistrationScreen() {
    var status by remember { mutableStateOf("Not Registered") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Hardcoded for emulator to host machine
    val baseUrl = "http://10.0.2.2:5173/" 

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Device Registration", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Status: $status")
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    try {
                        val retrofit = Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                        
                        val api = retrofit.create(ApiService::class.java)
                        val response = api.registerDevice(
                            RegistrationRequest(
                                id = "android-device-" + System.currentTimeMillis(),
                                model = android.os.Build.MODEL,
                                androidVersion = android.os.Build.VERSION.RELEASE
                            )
                        )
                        status = "Success: ${response.message}"
                    } catch (e: Exception) {
                        status = "Error: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading
        ) {
            Text(text = if (isLoading) "Registering..." else "Register Device")
        }
    }
}
