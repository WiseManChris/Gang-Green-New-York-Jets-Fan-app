package com.example.ganggreen.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ganggreen.data.DataRepository
import com.example.ganggreen.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settingsManager = SettingsManager(context)
                    val enabled = settingsManager.kickoffNotificationsFlow.first()
                    if (enabled) {
                        val repo = DataRepository()
                        val event = repo.getJetsEvent()
                        if (event != null && event.status?.type?.state == "pre") {
                            GameNotificationManager.scheduleKickoffAlarm(context, event)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
