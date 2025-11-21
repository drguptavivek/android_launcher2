package edu.aiims.surveylauncher.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import edu.aiims.surveylauncher.data.network.UserData
import edu.aiims.surveylauncher.data.network.ApiService
import edu.aiims.surveylauncher.data.network.PolicyConfig
import edu.aiims.surveylauncher.worker.TelemetryWorker
import edu.aiims.surveylauncher.util.KioskManager
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUser: UserData?,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onReset: () -> Unit,
    onChangePin: () -> Unit,
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { edu.aiims.surveylauncher.data.SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()
    var syncStatus by remember { mutableStateOf("") }
    var isSyncing by remember { mutableStateOf(false) }
    var policyName by remember { mutableStateOf(sessionManager.getPolicyName() ?: extractPolicyName(sessionManager.getPolicy()) ?: "Default (local)") }
    var selectedTheme by remember { mutableStateOf(currentTheme) }
    var themeMenuExpanded by remember { mutableStateOf(false) }
    val themeOptions = listOf(
        "deepBlue" to "Deep Blue",
        "sunset" to "Sunset Glow",
        "forest" to "Forest"
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Device Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Device Information", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Name: ${sessionManager.getDeviceDescription() ?: "Unknown"}")
                    Text("Model: ${android.os.Build.MODEL}")
                    Text("Device ID: ${sessionManager.getDeviceId() ?: "Not Registered"}")
                    Text("Server: http://localhost:5173/")
                    Text("User: ${currentUser?.username ?: "Not Logged In"}")
                    Text("Policy: ${policyName ?: "Default (local)"}")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Permissions Section
            Text("Permissions", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            
            val hasUsageStats = remember {
                val appOps = context.getSystemService(android.content.Context.APP_OPS_SERVICE) as android.app.AppOpsManager
                val mode = appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
                mode == android.app.AppOpsManager.MODE_ALLOWED
            }

            if (!hasUsageStats) {
                Button(
                    onClick = {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Grant Usage Access")
                }
            } else {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Usage Access Granted ✅")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Appearance Section
            Text("Appearance", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = themeMenuExpanded,
                onExpandedChange = { themeMenuExpanded = !themeMenuExpanded }
            ) {
                OutlinedTextField(
                    value = themeOptions.find { it.first == selectedTheme }?.second ?: "Deep Blue",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Accent Theme") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = themeMenuExpanded,
                    onDismissRequest = { themeMenuExpanded = false }
                ) {
                    themeOptions.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                selectedTheme = key
                                themeMenuExpanded = false
                                onThemeChange(key)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // PIN Section
            Text("PIN", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onChangePin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change PIN")
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Back to Home / release kiosk
            Button(
                onClick = onBackToHome,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Home")
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Actions Section
            Text("Actions", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val workRequest = OneTimeWorkRequestBuilder<TelemetryWorker>().build()
                    WorkManager.getInstance(context).enqueue(workRequest)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send Telemetry Now")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    isSyncing = true
                    syncStatus = "Syncing..."
                    coroutineScope.launch {
                        try {
                            val deviceId = sessionManager.getDeviceId() ?: "unknown"
                            val retrofit = Retrofit.Builder()
                                .baseUrl("http://localhost:5173/")
                                .addConverterFactory(GsonConverterFactory.create())
                                .build()
                            
                            val api = retrofit.create(ApiService::class.java)
                            val policyResponse = api.syncPolicy(deviceId)
                            
                            // Save policy
                            sessionManager.savePolicy(policyResponse.config)
                            
                            // Parse and apply
                            val gson = Gson()
                            val policy = gson.fromJson(policyResponse.config, PolicyConfig::class.java)
                            val resolvedName = policy.name ?: policyResponse.name ?: "Default (local)"
                            policyName = resolvedName
                            sessionManager.savePolicyName(resolvedName)
                            
                            // Update KioskManager - merge defaults + policy + aiims packages, then apply
                            val kioskManager = KioskManager(context)
                            if (kioskManager.isDeviceOwner()) {
                                val merged = (KioskManager.DEFAULT_ALLOWED_PACKAGES + policy.allowedApps + getAiimsPackages(context)).distinct()
                                kioskManager.applyLockTaskAllowList(merged)
                            }
                            
                            syncStatus = "✅ Synced ${policy.allowedApps.size} apps"
                            isSyncing = false
                        } catch (e: Exception) {
                            syncStatus = "❌ Error: ${e.message}"
                            isSyncing = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSyncing
            ) {
                Text(if (isSyncing) "Syncing..." else "Sync Policy Now")
            }

            if (syncStatus.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = syncStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (syncStatus.startsWith("✅")) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Logout")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Reset Registration (Debug)")
            }
        }
    }
}

private fun extractPolicyName(policyJson: String?): String? {
    if (policyJson.isNullOrBlank()) return null
    return try {
        val gson = Gson()
        val policy = gson.fromJson(policyJson, PolicyConfig::class.java)
        policy.name
    } catch (_: Exception) {
        null
    }
}

private fun getAiimsPackages(context: android.content.Context): List<String> {
    return try {
        context.packageManager.getInstalledPackages(0)
            .map { it.packageName }
            .filter { it.startsWith("edu.aiims.") }
    } catch (e: Exception) {
        android.util.Log.e("SettingsScreen", "Error collecting edu.aiims.* packages", e)
        emptyList()
    }
}
