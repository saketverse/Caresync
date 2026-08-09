package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.VoiceReminderManager

class SmartReminderForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "caresync_reminder_safeguard"
        const val CHANNEL_NAME = "CareSync Reminder Safeguard & Smart Alarms"
        const val NOTIFICATION_ID = 8842

        const val ACTION_START_SERVICE = "com.example.service.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.service.STOP_SERVICE"
        const val ACTION_TRIGGER_VOICE_ALERT = "com.example.service.TRIGGER_VOICE_ALERT"

        const val EXTRA_ALERT_TEXT = "extra_alert_text"
        const val EXTRA_MED_NAME = "extra_med_name"
        const val EXTRA_DOSAGE = "extra_dosage"

        fun startService(context: Context) {
            try {
                val intent = Intent(context, SmartReminderForegroundService::class.java).apply {
                    action = ACTION_START_SERVICE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, SmartReminderForegroundService::class.java).apply {
                    action = ACTION_STOP_SERVICE
                }
                context.stopService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun triggerVoiceAlert(context: Context, text: String) {
            try {
                val intent = Intent(context, SmartReminderForegroundService::class.java).apply {
                    action = ACTION_TRIGGER_VOICE_ALERT
                    putExtra(EXTRA_ALERT_TEXT, text)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_SERVICE

        when (action) {
            ACTION_STOP_SERVICE -> {
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_VOICE_ALERT -> {
                val text = intent?.getStringExtra(EXTRA_ALERT_TEXT)
                if (!text.isNullOrBlank()) {
                    VoiceReminderManager.getInstance(applicationContext).speak(text)
                }
            }
            else -> {
                // Keep foreground service active
            }
        }

        try {
            val notification = buildForegroundNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildForegroundNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CareSync Smart Reminders Active")
            .setContentText("Background service active - Safeguarding medication alerts from battery optimization.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(contentIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Keeps smart medication reminders and voice alerts active through Android Doze & battery optimization"
                setSound(null, null)
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
