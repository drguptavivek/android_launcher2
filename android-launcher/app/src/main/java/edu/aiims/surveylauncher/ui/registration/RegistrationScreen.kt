package edu.aiims.surveylauncher.ui.registration

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.aiims.surveylauncher.BuildConfig
import edu.aiims.surveylauncher.data.network.ApiService
import edu.aiims.surveylauncher.data.network.RegistrationRequest
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
    val sessionManager = remember { edu.aiims.surveylauncher.data.SessionManager(context) }

    // Using localhost with adb reverse tcp:5173 tcp:5173
    val baseUrl = "http://localhost:5173/" 

    val isDebugBypass = BuildConfig.DEBUG && registrationCode == "1234"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Device Registration",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = registrationCode,
                    onValueChange = { if (it.length <= 5) registrationCode = it.uppercase() },
                    label = { Text("Enter 5-Digit Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (BuildConfig.DEBUG) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Debug: enter 1234 to bypass server registration",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

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
                                if (isDebugBypass) {
                                    sessionManager.saveDeviceRegistration(
                                        deviceId = "DEBUG-${android.os.Build.MODEL}",
                                        description = "Debug device (${android.os.Build.MODEL})",
                                        registeredAt = System.currentTimeMillis().toString()
                                    )
                                    status = "Success: Debug registration applied"
                                    kotlinx.coroutines.delay(600)
                                    onRegistrationSuccess()
                                    return@launch
                                }

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
                    enabled = !isLoading && (registrationCode.length == 5 || isDebugBypass),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (isLoading) "Registering..." else "Register Device")
                }
            }
        }
    }
}
