package com.example.ganggreen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.ganggreen.data.notification.GameNotificationManager
import com.example.ganggreen.theme.GangGreenTheme

class MainActivity : ComponentActivity() {
  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    GameNotificationManager.createNotificationChannel(this)

    val initialTab = intent.getStringExtra("open_tab")
    val settingsManager = com.example.ganggreen.data.SettingsManager(this)

    setContent {
      val windowSizeClass = calculateWindowSizeClass(this)
      val viewModel: com.example.ganggreen.ui.main.MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
      val appTheme by settingsManager.themeFlow.collectAsState(initial = com.example.ganggreen.data.AppTheme.HOME)

      val notificationPermissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission()
      ) { /* Permission result handled */ }

      LaunchedEffect(Unit) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                  notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
              }
          }
      }

      GangGreenTheme(appTheme = appTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation(windowSizeClass, viewModel, settingsManager, initialTab)
        }
      }
    }
  }
}
