package com.example.ganggreen.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

enum class AppTheme {
    HOME, ALTERNATE, CLASSIC, AFL, RIVALRY, SYSTEM
}

class SettingsManager(private val context: Context) {
    companion object {
        val THEME_KEY = stringPreferencesKey("app_theme")
    }

    val themeFlow: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        val themeName = prefs[THEME_KEY] ?: AppTheme.HOME.name
        AppTheme.valueOf(themeName)
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = theme.name
        }
    }
}
