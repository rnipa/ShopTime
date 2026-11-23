package com.shoptimemaster.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object ShopNotificationManager {
    const val CHANNEL_TRACKING = "stm_tracking"
    const val CHANNEL_ALERTS = "stm_alerts"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_TRACKING, "Active Shop Tracking", NotificationManager.IMPORTANCE_LOW)
            )
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ALERTS, "Shop Reminders & Geofence Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    fun showGeofencePrompt(context: Context, isArrival: Boolean) {
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val title = if (isArrival) "আপনি কি দোকানে পৌঁছেছেন?" else "দোকান থেকে প্রস্থান শনাক্ত"
        val body = if (isArrival) {
            "দোকানের জিওফেন্সে প্রবেশ করেছেন। ট্র্যাকিং শুরু করবেন?"
        } else {
            "দোকানের সীমানা ত্যাগ করেছেন। সেশন বন্ধ করবেন?"
        }
        val notif = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()
        nm.notify(if (isArrival) 2001 else 2002, notif)
    }

    fun showMissingSessionWarning(context: Context) {
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("সেশন নিশ্চিতকরণ প্রয়োজন")
            .setContentText("পূর্বের সেশনের প্রস্থানের সময় নিশ্চিত করুন।")
            .setAutoCancel(true)
            .build()
        nm.notify(2003, notif)
    }
}
