package com.example.haven.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.haven.MainActivity
import com.example.haven.R

object HavenNotificationManager {
    const val CHANNEL_EMERGENCY = "haven_sos_alerts"
    const val CHANNEL_FAMILY_UPDATES = "haven_family_updates"

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. High-priority SOS Alert Channel
            val sosChannel = NotificationChannel(
                CHANNEL_EMERGENCY,
                "Family SOS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical emergency alerts and panic triggers from family members"
                enableLights(true)
                enableVibration(true)
            }

            // 2. Regular Family Updates Channel
            val updatesChannel = NotificationChannel(
                CHANNEL_FAMILY_UPDATES,
                "Family Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Chat messages, schedule reminders, and place check-ins"
            }

            notificationManager.createNotificationChannel(sosChannel)
            notificationManager.createNotificationChannel(updatesChannel)
        }
    }

    fun showEmergencyAlert(context: Context, senderName: String, lat: Double, lng: Double) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_EMERGENCY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🚨 EMERGENCY: $senderName triggered Family SOS!")
            .setContentText("Coordinates: %.4f° N, %.4f° W".format(lat, lng))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(999, notification)
    }

    fun showFamilyUpdate(context: Context, title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_FAMILY_UPDATES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
