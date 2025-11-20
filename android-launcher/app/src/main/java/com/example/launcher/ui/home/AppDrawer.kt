package com.example.launcher.ui.home

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.launcher.data.SessionManager
import com.example.launcher.data.network.PolicyConfig
import com.example.launcher.util.KioskManager
import com.google.gson.Gson

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable?
)

@Composable
fun AppDrawer() {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    
    // Get installed apps (reload on each recomposition to catch policy changes)
    val installedApps = getInstalledApps(context)
    
    // Get allowed apps from policy (reload on each recomposition)
    val policyJson = sessionManager.getPolicy()
    val allowedApps = if (policyJson != null) {
        try {
            val gson = Gson()
            val policy = gson.fromJson(policyJson, PolicyConfig::class.java)
            android.util.Log.d("AppDrawer", "Policy loaded: ${policy.allowedApps.size} allowed apps")
            policy.allowedApps.ifEmpty {
                android.util.Log.d("AppDrawer", "Policy allow-list empty, using default kiosk allow-list")
                KioskManager.DEFAULT_ALLOWED_PACKAGES
            }
        } catch (e: Exception) {
            android.util.Log.e("AppDrawer", "Error parsing policy, using default allow-list", e)
            KioskManager.DEFAULT_ALLOWED_PACKAGES
        }
    } else {
        android.util.Log.d("AppDrawer", "No policy found, using default kiosk allow-list")
        KioskManager.DEFAULT_ALLOWED_PACKAGES
    }

    // Dynamically include any installed edu.aiims.* packages
    val aiimsPackages = remember { getAiimsPackages(context) }
    val effectiveAllowedApps = (allowedApps + aiimsPackages).distinct()
    
    // Filter apps based on policy
    val filteredApps = installedApps
        .filter { app -> effectiveAllowedApps.contains(app.packageName) }
        .also { filtered ->
            android.util.Log.d("AppDrawer", "Filtered ${filtered.size} apps from ${installedApps.size} installed")
        }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = if (allowedApps.isNotEmpty()) 
                "Allowed Apps (${filteredApps.size})" 
            else 
                "All Apps (${filteredApps.size})",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No apps available.\nWait for policy sync or contact admin.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps) { app ->
                    AppIcon(app = app, context = context)
                }
            }
        }
    }
}

@Composable
fun AppIcon(app: AppInfo, context: Context) {
    Column(
        modifier = Modifier
            .clickable {
                launchApp(context, app.packageName)
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App icon would go here (requires AndroidView for Drawable)
        Surface(
            modifier = Modifier.size(48.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = app.label.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2
        )
    }
}

fun getInstalledApps(context: Context): List<AppInfo> {
    val packageManager = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    
    android.util.Log.d("AppDrawer", "Querying for launcher apps")
    
    // Try with different flags to catch all apps
    val flags = PackageManager.MATCH_ALL or 
                PackageManager.MATCH_DISABLED_COMPONENTS or
                PackageManager.MATCH_UNINSTALLED_PACKAGES
    
    val apps = packageManager.queryIntentActivities(intent, flags)
        .map { resolveInfo ->
            android.util.Log.d("AppDrawer", "Found activity: ${resolveInfo.activityInfo.packageName}/${resolveInfo.activityInfo.name}")
            AppInfo(
                label = resolveInfo.loadLabel(packageManager).toString(),
                packageName = resolveInfo.activityInfo.packageName,
                icon = resolveInfo.loadIcon(packageManager)
            )
        }
        .toMutableList()
    
    // Manually add apps that might not appear in queryIntentActivities
    // Map of package name to (default label, activity class name)
    val manualApps = listOf(
        Triple("org.odk.collect.android", "ODK Collect", "org.odk.collect.android.activities.SplashScreenActivity")
    )
    
    android.util.Log.d("AppDrawer", "Checking ${manualApps.size} manual apps...")
    
    for ((packageName, defaultLabel, activityName) in manualApps) {
        try {
            android.util.Log.d("AppDrawer", "Checking if $packageName is installed...")
            
            // Check if package is installed
            val isInstalled = try {
                packageManager.getPackageInfo(packageName, 0)
                true
            } catch (e: Exception) {
                false
            }
            
            if (isInstalled && !apps.any { it.packageName == packageName }) {
                android.util.Log.d("AppDrawer", "Adding $packageName to app list")
                val appInfo = AppInfo(
                    label = try {
                        packageManager.getApplicationInfo(packageName, 0)
                            .loadLabel(packageManager).toString()
                    } catch (e: Exception) {
                        android.util.Log.e("AppDrawer", "Error getting label for $packageName", e)
                        defaultLabel
                    },
                    packageName = packageName,
                    icon = try {
                        packageManager.getApplicationIcon(packageName)
                    } catch (e: Exception) {
                        android.util.Log.e("AppDrawer", "Error getting icon for $packageName", e)
                        null
                    }
                )
                apps.add(appInfo)
                android.util.Log.d("AppDrawer", "Successfully added: $packageName")
            } else {
                if (!isInstalled) {
                    android.util.Log.d("AppDrawer", "$packageName is not installed")
                }
                if (apps.any { it.packageName == packageName }) {
                    android.util.Log.d("AppDrawer", "$packageName already in list")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppDrawer", "Error adding $packageName", e)
        }
    }
    
    val sortedApps = apps.sortedBy { it.label }
    
    android.util.Log.d("AppDrawer", "Found ${sortedApps.size} launcher apps:")
    sortedApps.forEach { app ->
        android.util.Log.d("AppDrawer", "  - ${app.label} (${app.packageName})")
    }
    
    return sortedApps
}

private fun getAiimsPackages(context: Context): List<String> {
    return try {
        context.packageManager.getInstalledPackages(0)
            .map { it.packageName }
            .filter { it.startsWith("edu.aiims.") }
    } catch (e: Exception) {
        android.util.Log.e("AppDrawer", "Error collecting edu.aiims.* packages", e)
        emptyList()
    }
}

fun launchApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (intent != null) {
        // Add FLAG_ACTIVITY_NEW_TASK for lock task mode compatibility
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            android.util.Log.d("AppDrawer", "Launching $packageName")
            context.startActivity(intent)
        } catch (e: SecurityException) {
            android.util.Log.e("AppDrawer", "Security exception launching $packageName", e)
            android.widget.Toast.makeText(
                context,
                "Cannot launch this app in Kiosk mode",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            android.util.Log.e("AppDrawer", "Failed to launch $packageName", e)
            android.widget.Toast.makeText(
                context,
                "Failed to launch app: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    } else {
        android.util.Log.e("AppDrawer", "No launch intent found for $packageName")
        android.widget.Toast.makeText(
            context,
            "App not found",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}
