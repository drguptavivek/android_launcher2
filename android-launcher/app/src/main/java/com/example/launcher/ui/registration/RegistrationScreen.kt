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
fun RegistrationScreen(onRegistrationSuccess: () -> Unit) {
    var registrationCode by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { com.example.launcher.data.SessionManager(context) }

    // Using localhost with adb reverse tcp:5173 tcp:5173
    val baseUrl = "http://localhost:5173/" 

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Device Registration", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = registrationCode,
            onValueChange = { if (it.length <= 5) registrationCode = it.uppercase() },
            label = { Text("Enter 5-Digit Code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (status.isNotEmpty()) {
            Text(
                text = status,
                color = if (status.startsWith("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    status = ""
                    try {
                        val retrofit = Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                        
                        val api = retrofit.create(ApiService::class.java)
                        val response = api.registerDevice(
                            RegistrationRequest(
                                registrationCode = registrationCode,
                                model = android.os.Build.MODEL,
                                androidVersion = android.os.Build.VERSION.RELEASE
                            )
                        )
                        
                        if (response.status == "success") {
                            sessionManager.saveDeviceRegistration(
                                deviceId = response.deviceId,
                                description = response.description,
                                registeredAt = response.registeredAt
                            )
                            status = "Success: ${response.message}"
                            // Delay slightly to show success message before navigating
                            kotlinx.coroutines.delay(1000)
                            onRegistrationSuccess()
                        } else {
                            status = "Error: ${response.message}"
                        }
                    } catch (e: Exception) {
                        status = "Error: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading && registrationCode.length == 5,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(text = if (isLoading) "Registering..." else "Register Device")
        }
    }
}
