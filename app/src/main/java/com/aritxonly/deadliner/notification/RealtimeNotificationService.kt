package com.aritxonly.deadliner.notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.platform.NotificationMetadataEntry
import java.time.Instant

/** Shared native lifecycle; KMP metadata determines task/habit/terminal visibility. */
class RealtimeNotificationService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var foregroundStarted = false
    private val displayed = mutableSetOf<String>()
    private var signature: String? = null
    private val tick = Runnable { refresh() }
    override fun onCreate() {
        super.onCreate(); running = this
        NotificationUtil.createNotificationChannels(this)
        NotificationDiagnostics.event("realtime.service.created")
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (!foregroundStarted) {
                startForeground(FOREGROUND_ID, NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher).setContentTitle(getString(R.string.app_name)).setOngoing(true).build())
                foregroundStarted = true
            }
            refresh()
        } catch (error: Exception) { NotificationDiagnostics.failure("realtime.start.failed", error); stopSelf() }
        // System recreation receives a null intent and revalidates the persisted metadata.
        return START_STICKY
    }
    private fun refresh() {
        try {
            handler.removeCallbacks(tick)
            val metadata = KmpV6Runtime.notificationMetadata()
            val entries = metadata.realtime
            val manager = NotificationManagerCompat.from(this)
            val extras = entries.drop(1).map { it.key }.toSet()
            (displayed - extras).forEach { manager.cancel(tag(it), CARD_ID) }
            displayed.clear(); displayed.addAll(extras)
            if (entries.isEmpty() || !manager.areNotificationsEnabled()) {
                stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return
            }
            val current = signature(entries)
            if (signature != current) IslandWhitelistBypass.publish(this, "realtime", onPublication = { accepted ->
                if (!accepted) signature = null
            }) {
                if (running === this) {
                    // A delayed Root/Shizuku callback must re-read committed metadata.
                    val latest = KmpV6Runtime.notificationMetadata().realtime
                    val latestExtras = latest.drop(1).map { it.key }.toSet()
                    (displayed - latestExtras).forEach { manager.cancel(tag(it), CARD_ID) }
                    displayed.clear(); displayed.addAll(latestExtras)
                    if (latest.isEmpty()) {
                        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
                    }
                    latest.forEachIndexed { index, entry ->
                        val notification = RealtimeNotificationRenderer.build(this, entry)
                        val id = if (index == 0) FOREGROUND_ID else CARD_ID
                        if (index == 0) manager.notify(id, notification) else manager.notify(tag(entry.key), id, notification)
                        NotificationDiagnostics.built(this, notification, entry.ownerUid, id)
                        NotificationDiagnostics.observe(this, entry.ownerUid, id, if (index == 0) null else tag(entry.key))
                        NotificationDiagnostics.event("realtime.published", "key" to entry.key, "phase" to entry.phase,
                            "endsAt" to entry.endsAt, "foreground" to (index == 0))
                    }
                    signature = signature(latest)
                }
            }
            val untilBoundary = metadata.nextRefreshAt?.let { Instant.parse(it).toEpochMilli() - System.currentTimeMillis() }
            handler.postDelayed(tick, minOf(30_000L, untilBoundary ?: 30_000L).coerceAtLeast(100L))
        } catch (error: Exception) { NotificationDiagnostics.failure("realtime.refresh.failed", error); stopSelf() }
    }
    private fun signature(entries: List<NotificationMetadataEntry>) = entries.joinToString("|") {
        "${it.key}:${it.version}:${it.title}:${it.phase}:${it.actions}:${it.endsAt}:${it.habitWindow}:${it.completedCount}:${it.targetCount}:${it.focusSessionUid}:${it.focusDurationSeconds}:${RealtimeNotificationRenderer.deadline(this, it)}"
    } + IslandWhitelistBypass.mode(this)
    override fun onDestroy() {
        if (running === this) running = null
        handler.removeCallbacksAndMessages(null)
        displayed.forEach { NotificationManagerCompat.from(this).cancel(tag(it), CARD_ID) }
        NotificationDiagnostics.event("realtime.service.destroyed")
        super.onDestroy()
    }
    override fun onBind(intent: Intent?) = null
    companion object {
        const val FOREGROUND_ID = 530_001
        private const val CARD_ID = 1
        @Volatile private var running: RealtimeNotificationService? = null
        private fun tag(key: String) = "realtime:$key"
        fun refreshRunning() { running?.let { service -> service.handler.post { service.refresh() } } }
        fun clearRunning() { running?.let { service -> service.handler.post { service.stopForeground(STOP_FOREGROUND_REMOVE); service.stopSelf() } } }
        fun start(context: Context) {
            if (running != null) { refreshRunning(); return }
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
            try { context.startForegroundService(Intent(context, RealtimeNotificationService::class.java)) }
            catch (error: Exception) { NotificationDiagnostics.failure("realtime.start.blocked", error) }
        }
    }
}
