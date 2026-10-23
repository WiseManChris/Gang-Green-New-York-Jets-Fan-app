package com.example.ganggreen.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ganggreen.data.AppTheme
import com.example.ganggreen.data.SettingsManager
import kotlinx.coroutines.launch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.ganggreen.theme.JetsGreenLight
import com.example.ganggreen.theme.StatusRedText
import com.example.ganggreen.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsManager: SettingsManager) {
    val coroutineScope = rememberCoroutineScope()
    val currentTheme by settingsManager.themeFlow.collectAsState(initial = AppTheme.HOME)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }

        // Theme Selection
        item {
            Text("App Theme", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
                    AppTheme.values().forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch { settingsManager.setTheme(theme) }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val displayName = when (theme) {
                                AppTheme.HOME -> "Home (Gotham Green)"
                                AppTheme.ALTERNATE -> "Alternate (Stealth Black)"
                                AppTheme.CLASSIC -> "Classic (Kelly Green)"
                                AppTheme.AFL -> "AFL (Titans Navy/Gold)"
                                AppTheme.RIVALRY -> "Rivalry (Gotham City Football)"
                                AppTheme.SYSTEM -> "System (Material You)"
                            }
                            Text(displayName, style = MaterialTheme.typography.bodyLarge)
                            RadioButton(
                                selected = (theme == currentTheme),
                                onClick = { coroutineScope.launch { settingsManager.setTheme(theme) } }
                            )
                        }
                        if (theme != AppTheme.SYSTEM) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))
                        }
                    }
                }
            }
        }

        // Notifications Section
        item {
            val context = LocalContext.current
            val kickoffNotificationsEnabled by settingsManager.kickoffNotificationsFlow.collectAsState(initial = true)
            var hasNotificationPermission by remember {
                mutableStateOf(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    } else true
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                hasNotificationPermission = isGranted
                if (isGranted) {
                    coroutineScope.launch { settingsManager.setKickoffNotifications(true) }
                }
            }

            Text("Notifications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text("Kickoff & Game Start Alerts", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) 
                                    "Notification permission required to receive game alerts" 
                                else 
                                    "Get notified the moment New York Jets games kickoff or start",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) StatusRedText else TextSecondary
                            )
                        }
                        Switch(
                            checked = kickoffNotificationsEnabled && hasNotificationPermission,
                            onCheckedChange = { isChecked ->
                                if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    coroutineScope.launch { settingsManager.setKickoffNotifications(isChecked) }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = JetsGreenLight
                            )
                        )
                    }
                }
            }
        }
    }
}
