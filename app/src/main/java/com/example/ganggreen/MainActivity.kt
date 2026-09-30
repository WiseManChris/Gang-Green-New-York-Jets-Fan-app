package com.example.ganggreen

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ganggreen.theme.GangGreenTheme

@androidx.compose.material3.ExperimentalMaterial3Api
class MainActivity : ComponentActivity() {
  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val settingsManager = com.example.ganggreen.data.SettingsManager(this)
    setContent {
      val windowSizeClass = calculateWindowSizeClass(this)
      val viewModel: com.example.ganggreen.ui.main.MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
      val appTheme by settingsManager.themeFlow.collectAsState(initial = com.example.ganggreen.data.AppTheme.HOME)
      
      GangGreenTheme(appTheme = appTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation(windowSizeClass, viewModel, settingsManager)
        }
      }
    }
  }
}
