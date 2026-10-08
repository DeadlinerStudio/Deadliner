package com.aritxonly.deadliner.notification

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import com.aritxonly.deadliner.logging.AppLog
import com.aritxonly.deadliner.localutils.GlobalUtils

/** Every event uses the existing [time][Native][notification.*] TXT projection. */
internal object NotificationDiagnostics {
    fun event(name: String, vararg fields: Pair<String, Any?>) = runCatching { AppLog.event(
        "notification.$name", domain = "notification",
        context = fields.associate { it.first to (it.second?.toString() ?: "null") },
    ) }.getOrNull()

    fun failure(name: String, error: Throwable, vararg fields: Pair<String, Any?>) = runCatching { AppLog.event(
        "notification.$name", domain = "notification", level = AppLog.LogLevel.ERROR,
        context = fields.associate { it.first to (it.second?.toString() ?: "null") } + mapOf(
            "exception" to error.javaClass.name, "error" to error.message.orEmpty(),
            "stack" to error.stackTrace.take(8).joinToString(" | "),
        ),
    ) }.getOrNull()

    fun postingBlock(context: Context): String? {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return "post_permission_denied"
        if (!manager.areNotificationsEnabled()) return "app_notifications_disabled"
        if (manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE)
            return "channel_disabled"
        return null
    }

    fun snapshot(context: Context, run: String? = null) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val policy = NotificationPolicyPreferences.read(context)
        event("environment", "run" to run, "sdk" to Build.VERSION.SDK_INT,
            "brand" to Build.BRAND, "manufacturer" to Build.MANUFACTURER, "model" to Build.MODEL,
            "os" to Build.VERSION.RELEASE, "build" to Build.VERSION.INCREMENTAL,
            "postingBlock" to postingBlock(context), "channelImportance" to manager.getNotificationChannel(CHANNEL_ID)?.importance,
            "exactAlarm" to context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms(),
            "promotedAllowed" to if (Build.VERSION.SDK_INT >= 36) probe { manager.canPostPromotedNotifications() } else "unsupported_api",
            "islandSupported" to LiveUpdateNotificationAdapter.supportsIsland(), "islandMode" to IslandWhitelistBypass.mode(context),
            "taskRealtimeEnabled" to policy.taskRealtime, "taskNearbyEnabled" to policy.taskNearby,
            "taskDueEnabled" to policy.taskDue, "habitMode" to policy.habitMode, "taskWindowMinutes" to 60,
        )
    }

    fun built(context: Context, notification: Notification, uid: String, id: Int) {
        val manager = context.getSystemService(NotificationManager::class.java)
        event("live.built", "uid" to uid, "id" to id, "channel" to notification.channelId,
            "flags" to notification.flags, "ongoing" to (notification.flags and Notification.FLAG_ONGOING_EVENT != 0),
            "requestPromoted" to notification.extras.getBoolean(NotificationCompat.EXTRA_REQUEST_PROMOTED_ONGOING),
            "promotable" to if (Build.VERSION.SDK_INT >= 36) probe { notification.hasPromotableCharacteristics() } else "unsupported_api",
            "promotedAllowed" to if (Build.VERSION.SDK_INT >= 36) probe { manager.canPostPromotedNotifications() } else "unsupported_api",
            "islandPayload" to notification.extras.containsKey("miui.focus.param"),
            "islandPictures" to notification.extras.containsKey("miui.focus.pics"),
        )
    }

    private fun probe(value: () -> Any?): Any? = runCatching(value).getOrElse { "unavailable:${it.javaClass.simpleName}" }

    fun observe(context: Context, uid: String, id: Int, tag: String? = null) {
        val app = context.applicationContext
        Handler(Looper.getMainLooper()).postDelayed({
            runCatching {
                val posted = app.getSystemService(NotificationManager::class.java).activeNotifications
                    .firstOrNull { it.id == id && it.tag == tag }
                event("live.observed", "uid" to uid, "id" to id, "tag" to tag, "active" to (posted != null),
                    "flags" to posted?.notification?.flags,
                    "promoted" to if (Build.VERSION.SDK_INT >= 36) posted?.notification?.let {
                        it.flags and Notification.FLAG_PROMOTED_ONGOING != 0
                    } else "unsupported_api",
                )
            }.onFailure { failure("live.observe_failed", it, "uid" to uid, "id" to id) }
        }, 1_500L)
    }
}
