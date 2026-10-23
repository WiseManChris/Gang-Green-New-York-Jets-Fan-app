package com.example.ganggreen.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ganggreen.data.SettingsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class KickoffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settingsManager = SettingsManager(context)
        val notificationsEnabled = runBlocking {
            try {
                settingsManager.kickoffNotificationsFlow.first()
            } catch (e: Exception) {
                true
            }
        }

        if (!notificationsEnabled) return

        val homeTeam = intent.getStringExtra("home_team") ?: "New York Jets"
        val awayTeam = intent.getStringExtra("away_team") ?: "Opponent"
        val title = "🏈 KICKOFF!"
        val message = "$awayTeam at $homeTeam is kicking off right now! Tap to follow live."

        GameNotificationManager.showGameAlert(
            context,
            title,
            message,
            GameNotificationManager.NOTIFICATION_ID_KICKOFF
        )
    }
}
