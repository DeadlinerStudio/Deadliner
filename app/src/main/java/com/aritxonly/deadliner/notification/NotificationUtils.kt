package com.aritxonly.deadliner.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.aritxonly.deadliner.R

const val CHANNEL_ID = "deadliner_alerts"
const val CHANNEL_DAILY_ID = "deadliner_daily_alerts"

/** Android-only notification channel registration; scheduling and lifecycle live in KMP. */
object NotificationUtil {
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            FocusRealtimeNotificationRenderer.CHANNEL_ID, context.getString(R.string.focus_live_notification), NotificationManager.IMPORTANCE_LOW))
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_deadline_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_deadline_desc)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DAILY_ID,
                context.getString(R.string.channel_daily_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_daily_desc)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
            },
        )
    }
}
