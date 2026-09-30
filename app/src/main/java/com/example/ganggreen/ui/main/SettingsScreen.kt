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

}
}
