package com.aritxonly.deadliner.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aritxonly.deadliner.DeadlineDetailActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.deadliner.platform.NotificationMetadata
import com.deadliner.platform.NotificationMetadataEntry
import com.deadliner.platform.NotificationKind
import com.google.gson.Gson
import java.time.Instant

/** OS registrations are a persisted diff ledger; business timing comes exclusively from KMP. */
internal class AndroidNotificationMetadataScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val prefs = context.getSharedPreferences("notification_metadata_schedule", Context.MODE_PRIVATE)
    private val gson = Gson()
    private fun entries(): List<NotificationMetadataEntry> = runCatching {
        gson.fromJson(prefs.getString("entries", "[]"), Array<NotificationMetadataEntry>::class.java).toList()
    }.getOrDefault(emptyList())

    @Synchronized fun apply(metadata: NotificationMetadata) {
        val activeTags = metadata.realtime.drop(1).map { "realtime:${it.key}" }.toSet()
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.activeNotifications.filter { it.tag?.startsWith("realtime:") == true && it.tag !in activeTags }
            .forEach { manager.cancel(it.tag, it.id) }
        val posted = runCatching {
            gson.fromJson(prefs.getString("posted", "[]"), Array<NotificationMetadataEntry>::class.java).toList()
        }.getOrDefault(emptyList())
        val keepPosted = posted.filter { metadata.activeVersions[it.ownerUid] == it.version && it.kind in metadata.ordinaryKinds }
        (posted - keepPosted.toSet()).forEach { context.getSystemService(NotificationManager::class.java).cancel("metadata:${it.key}", 1) }
        prefs.edit().putString("posted", gson.toJson(keepPosted)).apply()
        val previous = entries().associateBy { it.key }
        val desired = metadata.alarms.associateBy { it.key }
        (previous.keys - desired.keys).forEach { cancel(it) }
        desired.values.filter { previous[it.key] != it }.forEach { entry ->
            register(entry.key, Instant.parse(entry.triggerAt).toEpochMilli(), gson.toJson(entry))
        }
        prefs.edit().putString("entries", gson.toJson(metadata.alarms)).apply()
        val next = metadata.nextRefreshAt
        if (next == null) cancel(BOUNDARY)
        else if (prefs.getString("boundary", null) != next) register(BOUNDARY, Instant.parse(next).toEpochMilli(), null)
        prefs.edit().putString("boundary", next).apply()
        NotificationDiagnostics.event("metadata.reconciled", "alarms" to desired.size,
            "realtime" to metadata.realtime.size, "nextRefreshAt" to next)
        if (metadata.realtime.isNotEmpty()) RealtimeNotificationService.start(context)
        else RealtimeNotificationService.refreshRunning()
    }

    @Synchronized fun cancelAll() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.activeNotifications.filter { it.tag?.startsWith("realtime:") == true || it.id == RealtimeNotificationService.FOREGROUND_ID }
            .forEach { manager.cancel(it.tag, it.id) }
        entries().forEach { cancel(it.key) }
        cancel(BOUNDARY)
        prefs.edit().remove("entries").remove("boundary").apply()
        RealtimeNotificationService.clearRunning()
    }

    private fun pending(key: String, json: String? = null): PendingIntent = PendingIntent.getBroadcast(context, 0,
        Intent(context, NotificationMetadataReceiver::class.java).setAction(ACTION)
            .setData(Uri.Builder().scheme("deadliner-metadata").authority(context.packageName).appendPath(key).build())
            .putExtra(EXTRA_ENTRY, json).putExtra(EXTRA_KEY, key), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun register(key: String, at: Long, json: String?) {
        val pending = pending(key, json)
        if (alarms.canScheduleExactAlarms()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        NotificationDiagnostics.event("metadata.alarm.registered", "key" to key, "at" to at, "exact" to alarms.canScheduleExactAlarms())
    }
    private fun cancel(key: String) {
        // FLAG_NO_CREATE avoids changing extras on an already queued delivery.
        val intent = Intent(context, NotificationMetadataReceiver::class.java).setAction(ACTION)
            .setData(Uri.Builder().scheme("deadliner-metadata").authority(context.packageName).appendPath(key).build())
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
            alarms.cancel(it); it.cancel()
        }
    }

    @Synchronized fun deliver(entry: NotificationMetadataEntry): Boolean {
        if (entry.kind == NotificationKind.HABIT_START) return false
        val receipt = entry.key + ":" + entry.triggerAt
        val delivered = prefs.getStringSet("delivered", emptySet()).orEmpty()
        if (receipt in delivered || !NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        NotificationUtil.createNotificationChannels(context)
        val title = when (entry.kind) {
            NotificationKind.TASK_NEARBY -> R.string.notification_task_nearby
            NotificationKind.TASK_DUE -> R.string.notification_task_due
            else -> R.string.notification_habit_due
        }
        val open = if (entry.kind == NotificationKind.HABIT_DUE) Intent(context, MainActivity::class.java)
            else DeadlineDetailActivity.newIntent(context, entry.ownerUid)
        open.data = Uri.Builder().scheme("deadliner").authority("notification").appendPath(entry.key).build()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(title)).setContentText(entry.title)
            .setContentIntent(PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true).build()
        context.getSystemService(NotificationManager::class.java).notify("metadata:${entry.key}", 1, notification)
        val posted = runCatching {
            gson.fromJson(prefs.getString("posted", "[]"), Array<NotificationMetadataEntry>::class.java).toList()
        }.getOrDefault(emptyList())
        prefs.edit().putString("posted", gson.toJson(posted.filterNot { it.key == entry.key } + entry)).apply()
        prefs.edit().putStringSet("delivered", (delivered + receipt).toList().takeLast(4096).toSet()).commit()
        NotificationDiagnostics.event("metadata.delivered", "key" to entry.key, "kind" to entry.kind)
        return true
    }

    companion object {
        const val ACTION = "com.aritxonly.deadliner.notification.METADATA"
        const val EXTRA_ENTRY = "entry"
        const val EXTRA_KEY = "key"
        const val BOUNDARY = "state-boundary"
    }
}
