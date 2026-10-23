package com.example.ganggreen.data.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.ganggreen.MainActivity
import com.example.ganggreen.data.DataRepository
import com.example.ganggreen.data.model.Event

object GameNotificationManager {
    const val CHANNEL_ID = "gang_green_game_alerts"
    const val CHANNEL_NAME = "Kickoff & Game Alerts"
    const val PREFS_NAME = "gang_green_game_notifications"
    const val PREF_LAST_NOTIFIED_STATE = "last_notified_state_"
    const val NOTIFICATION_ID_KICKOFF = 1001
    const val NOTIFICATION_ID_LIVE = 1002

    fun createNotificationChannel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Real-time alerts when New York Jets games kickoff or start"
                    enableLights(true)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 200, 300)
                }
                notificationManager?.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun scheduleKickoffAlarm(context: Context, event: Event) {
        val comp = event.competitions.firstOrNull() ?: return
        val dateStr = comp.date.ifEmpty { event.date }
        val kickoffTimeMs = DataRepository().parseGameDateToMillis(dateStr)
        val nowMs = System.currentTimeMillis()

        if (kickoffTimeMs <= nowMs) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, KickoffReceiver::class.java).apply {
            putExtra("event_id", event.id)
            putExtra("event_name", event.name)
            val home = comp.competitors.find { it.homeAway == "home" }?.team?.displayName ?: "Home"
            val away = comp.competitors.find { it.homeAway == "away" }?.team?.displayName ?: "Away"
            putExtra("home_team", home)
            putExtra("away_team", away)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID_KICKOFF,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, kickoffTimeMs, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, kickoffTimeMs, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showGameAlert(context: Context, title: String, message: String, notificationId: Int = NOTIFICATION_ID_KICKOFF) {
        try {
            createNotificationChannel(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_tab", "live")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setSound(defaultSoundUri)
                .setVibrate(longArrayOf(0, 300, 200, 300))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun checkAndNotifyGameStatus(context: Context, event: Event) {
        try {
            val state = event.status?.type?.state ?: event.competitions.firstOrNull()?.status?.type?.state ?: return
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val key = PREF_LAST_NOTIFIED_STATE + event.id
            val lastState = prefs.getString(key, null)

            if (state == "in" && lastState != "in") {
                prefs.edit().putString(key, "in").apply()
                val comp = event.competitions.firstOrNull()
                val away = comp?.competitors?.find { it.homeAway == "away" }?.team?.displayName ?: "Away"
                val home = comp?.competitors?.find { it.homeAway == "home" }?.team?.displayName ?: "Home"
                val title = "🚨 GAME IS LIVE!"
                val message = "$away at $home has started! Tap to view live play-by-play and scores."
                showGameAlert(context, title, message, NOTIFICATION_ID_LIVE)
            } else if (state == "post" && lastState != "post") {
                prefs.edit().putString(key, "post").apply()
            } else if (lastState == null && state == "pre") {
                prefs.edit().putString(key, "pre").apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
