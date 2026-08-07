package com.example.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.VoiceReminderManager

class ReminderAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ALARM_TRIGGER = "com.example.ACTION_MEDICATION_REMINDER"
        const val EXTRA_MED_NAME = "med_name"
        const val EXTRA_DOSAGE = "dosage"
        const val EXTRA_TIME = "scheduled_time"

        fun scheduleExactAlarm(
            context: Context,
            medName: String,
            dosage: String,
            timeStr: String,
            triggerAtMillis: Long
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ACTION_ALARM_TRIGGER
                putExtra(EXTRA_MED_NAME, medName)
                putExtra(EXTRA_DOSAGE, dosage)
                putExtra(EXTRA_TIME, timeStr)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (medName + timeStr).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Restart smart background service on system reboot
            SmartReminderForegroundService.startService(context)
            return
        }

        if (action == ACTION_ALARM_TRIGGER) {
            val medName = intent.getStringExtra(EXTRA_MED_NAME) ?: "Medication"
            val dosage = intent.getStringExtra(EXTRA_DOSAGE) ?: "1 dose"
            val time = intent.getStringExtra(EXTRA_TIME) ?: ""

            val alertMsg = "Time to take $medName ($dosage) - Scheduled for $time"

            // Voice alert
            VoiceReminderManager.getInstance(context.applicationContext).speak(alertMsg)

            // High Priority System Notification
            showAlarmNotification(context, medName, alertMsg)
        }
    }

    private fun showAlarmNotification(context: Context, medName: String, alertText: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SmartReminderForegroundService.CHANNEL_ID)
            .setContentTitle("💊 CareSync Medication Alert: $medName")
            .setContentText(alertText)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .build()

        notificationManager.notify((medName + System.currentTimeMillis()).hashCode(), notification)
    }
}
