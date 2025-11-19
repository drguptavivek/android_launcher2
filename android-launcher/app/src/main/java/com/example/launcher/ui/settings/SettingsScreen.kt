package com.example.launcher.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.launcher.data.network.UserData
import com.example.launcher.worker.TelemetryWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUser: UserData?,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(androidx.compose.material.icons.Icons.Default.ArrowBack, contentDescription = "Back")
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
        ) {
            // Device Info Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Device Information", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Model: ${android.os.Build.MODEL}")
                    Text("Server: http://localhost:5173/") // Hardcoded for now
                    Text("User ID: ${currentUser?.id}")
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

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Logout")
            }
        }
    }
}
