package com.aritxonly.deadliner.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class FocusTimerForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var updateJob: Job? = null
    private var alive = true
    private var signature: String? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(
            FocusRealtimeNotificationRenderer.CHANNEL_ID, getString(R.string.focus_live_notification), NotificationManager.IMPORTANCE_LOW))
        // Publish only the placeholder before the Root authorization window opens.
        startForeground(FocusRealtimeNotificationRenderer.NOTIFICATION_ID,
            NotificationCompat.Builder(this, FocusRealtimeNotificationRenderer.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_focus_notification).setContentTitle(getString(R.string.focus_live_notification))
                .setOngoing(true).setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE).build())
        NotificationDiagnostics.event("focus.notification.service.created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateJob?.cancel()
        updateJob = scope.launch {
            try {
                KmpV6Runtime.initialize(this@FocusTimerForegroundService)
                val viewModel = KmpV6Runtime.focusTimerViewModel
                viewModel.loadNow()
                var ticks = 1
                while (isActive) {
                    viewModel.tick()
                    if (ticks++ % 30 == 0) viewModel.loadNow()
                    val info = KmpV6Runtime.database.focusTimerNotificationInfo()
                    if (info.snapshot == null) {
                        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); break
                    }
                    val current = FocusRealtimeNotificationRenderer.signature(this@FocusTimerForegroundService, info)
                    if (current != signature) IslandWhitelistBypass.publish(this@FocusTimerForegroundService, "focus", onPublication = { accepted ->
                        if (!accepted) signature = null
                    }) {
                        if (alive) {
                            // Synchronous KMP read keeps notify inside the authorized publication window.
                            val latest = KmpV6Runtime.database.focusTimerNotificationInfo()
                            if (latest.snapshot == null) {
                                stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
                            } else {
                                val notification = FocusRealtimeNotificationRenderer.build(this@FocusTimerForegroundService, latest)
                                getSystemService(NotificationManager::class.java).notify(FocusRealtimeNotificationRenderer.NOTIFICATION_ID, notification)
                                signature = FocusRealtimeNotificationRenderer.signature(this@FocusTimerForegroundService, latest)
                                NotificationDiagnostics.built(this@FocusTimerForegroundService, notification,
                                    latest.snapshot!!.sessionUid, FocusRealtimeNotificationRenderer.NOTIFICATION_ID)
                                NotificationDiagnostics.event("focus.notification.published", "session" to latest.snapshot!!.sessionUid,
                                    "state" to latest.snapshot!!.state, "mode" to latest.snapshot!!.mode,
                                    "version" to latest.activeTimer?.updatedAt, "dispatch" to "notify_inside_window")
                                NotificationDiagnostics.observe(this@FocusTimerForegroundService, latest.snapshot!!.sessionUid,
                                    FocusRealtimeNotificationRenderer.NOTIFICATION_ID)
                            }
                        }
                    }
                    delay(1_000)
                }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                NotificationDiagnostics.failure("focus.notification.service.failed", error)
                stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        alive = false
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        NotificationDiagnostics.event("focus.notification.service.destroyed")
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun start(context: Context) = ContextCompat.startForegroundService(context, Intent(context, FocusTimerForegroundService::class.java))
        fun stop(context: Context) { context.stopService(Intent(context, FocusTimerForegroundService::class.java)) }
    }
}
