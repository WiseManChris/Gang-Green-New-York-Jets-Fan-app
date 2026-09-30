package com.example.ganggreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Settings
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ganggreen.theme.*
import com.example.ganggreen.ui.main.LiveScreen
import com.example.ganggreen.ui.main.NewsScreen
import com.example.ganggreen.ui.main.RosterScreen
import com.example.ganggreen.ui.main.InjuryScreen
import com.example.ganggreen.ui.main.SettingsScreen
import com.example.ganggreen.ui.main.LiveChatScreen

sealed class Screen(val route: String, val label: String, val icon: @Composable () -> Unit) {
    object News : Screen("news", "News", { Icon(Icons.Filled.Article, contentDescription = "News") })
    object Roster : Screen("roster", "Roster", { Icon(Icons.Filled.Group, contentDescription = "Roster") })
    object Injury : Screen("injury", "Injury", { Icon(Icons.Filled.LocalHospital, contentDescription = "Injury") })
    object Live : Screen("live", "Live", { Icon(Icons.Filled.SportsFootball, contentDescription = "Live") })
    object Settings : Screen("settings", "Settings", { Icon(Icons.Filled.Settings, contentDescription = "Settings") })
    object LiveChat : Screen("live_chat", "Chat", { Icon(Icons.Filled.Chat, contentDescription = "Chat") })
}

val items = listOf(
    Screen.News,
    Screen.Roster,
    Screen.Injury,
    Screen.Live,
    Screen.LiveChat,
    Screen.Settings
)

@Composable
fun MainNavigation(windowSizeClass: WindowSizeClass, viewModel: com.example.ganggreen.ui.main.MainViewModel, settingsManager: com.example.ganggreen.data.SettingsManager) {
    val navController = rememberNavController()
    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    Scaffold(
        bottomBar = {
            if (isCompact) {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = screen.icon,
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = JetsGreenLight,
                                unselectedIconColor = TextSecondary,
                                selectedTextColor = JetsGreenLight,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = DarkGreenSurface
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val currentTheme by settingsManager.themeFlow.collectAsStateWithLifecycle(initialValue = com.example.ganggreen.data.AppTheme.HOME)
        ThemeBackground(currentTheme)
        
        Row(modifier = Modifier.padding(innerPadding)) {
            if (!isCompact) {
                NavigationRail {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    items.forEach { screen ->
                        NavigationRailItem(
                            icon = screen.icon,
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
            NavHost(navController, startDestination = Screen.News.route, modifier = Modifier.weight(1f)) {
                composable(Screen.News.route) { NewsScreen(viewModel, settingsManager) }
                composable(Screen.Roster.route) { RosterScreen(viewModel) }
                composable(Screen.Injury.route) { InjuryScreen(viewModel) }
                composable(Screen.Live.route) { LiveScreen(viewModel) }
                composable(Screen.Settings.route) { SettingsScreen(settingsManager) }
                composable(Screen.LiveChat.route) { LiveChatScreen(viewModel) }
            }
        }
    }
}

@Composable
fun ThemeBackground(theme: com.example.ganggreen.data.AppTheme) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        val imageUrl = when (theme) {
            com.example.ganggreen.data.AppTheme.HOME, com.example.ganggreen.data.AppTheme.SYSTEM -> 
                "https://upload.wikimedia.org/wikipedia/en/thumb/6/6b/New_York_Jets_logo.svg/1200px-New_York_Jets_logo.svg.png"
            com.example.ganggreen.data.AppTheme.CLASSIC -> 
                "https://upload.wikimedia.org/wikipedia/en/thumb/e/e4/New_York_Jets_logo_1978-1997.svg/1200px-New_York_Jets_logo_1978-1997.svg.png"
            com.example.ganggreen.data.AppTheme.AFL -> 
                "https://upload.wikimedia.org/wikipedia/en/thumb/e/e8/Titans_of_New_York_logo.svg/1200px-Titans_of_New_York_logo.svg.png"
            com.example.ganggreen.data.AppTheme.ALTERNATE, com.example.ganggreen.data.AppTheme.RIVALRY -> 
                "https://upload.wikimedia.org/wikipedia/en/thumb/b/be/New_York_Jets_1963_logo.svg/1200px-New_York_Jets_1963_logo.svg.png"
        }
        
        coil.compose.AsyncImage(
            model = imageUrl,
            contentDescription = "Theme Watermark",
            modifier = Modifier.fillMaxSize(0.6f).alpha(0.08f), // Big and faint
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            colorFilter = if (theme == com.example.ganggreen.data.AppTheme.SYSTEM) {
                androidx.compose.ui.graphics.ColorFilter.tint(androidx.compose.ui.graphics.Color.Black)
            } else {
                null
            }
        )
    }
}
