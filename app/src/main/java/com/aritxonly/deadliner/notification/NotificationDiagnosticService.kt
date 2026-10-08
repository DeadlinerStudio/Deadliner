package com.aritxonly.deadliner.notification

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import java.util.Locale

/** Real foreground service and shared adapter, synthetic two-minute countdown. */
class NotificationDiagnosticService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var run: String? = null
    private var endAt = 0L
    private var sequence = 0
    private val tick = object : Runnable {
        override fun run() {
            val token = run ?: return
            if (NotificationDiagnosticController.currentRun(this@NotificationDiagnosticService) != token) {
                stopSelf()
                return
            }
            val remaining = ((endAt - SystemClock.elapsedRealtime()) / 1_000L).coerceAtLeast(0)
            if (remaining <= 0) {
                NotificationDiagnostics.event("test.finished", "run" to token)
                NotificationDiagnosticController.status(this@NotificationDiagnosticService, token, NotificationTestPhase.FINISHED)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }
            if (NotificationDiagnostics.postingBlock(this@NotificationDiagnosticService) != null) {
                NotificationDiagnosticController.status(this@NotificationDiagnosticService, token, NotificationTestPhase.BLOCKED, "permission_changed")
                NotificationDiagnostics.event("test.blocked", "run" to token, "reason" to "permission_changed")
                stopSelf()
                return
            }
            try {
                publish(token, remaining)
                handler.postDelayed(this, 1_000L)
            } catch (error: Exception) {
                NotificationDiagnostics.failure("test.tick.failed", error, "run" to token)
                NotificationDiagnosticController.status(this@NotificationDiagnosticService, token, NotificationTestPhase.FAILED, error.javaClass.simpleName)
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        NotificationUtil.createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val token = intent?.getStringExtra(NotificationDiagnosticController.EXTRA_RUN).orEmpty()
        run = token
        endAt = SystemClock.elapsedRealtime() + 120_000L
        sequence = 0
        handler.removeCallbacksAndMessages(null)
        try {
            // Always promote promptly; cancellation can race a foreground-service request.
            startForeground(NotificationDiagnosticController.NOTIFICATION_ID,
                NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.ic_focus_notification)
                    .setContentTitle(getString(R.string.notification_test_title)).setOngoing(true)
                    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE).build())
            if (NotificationDiagnosticController.currentRun(this) != token) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } else {
                NotificationDiagnosticController.status(this, token, NotificationTestPhase.RUNNING)
                NotificationDiagnostics.event("test.service.started", "run" to token, "durationSeconds" to 120)
                publish(token, 120)
                handler.postDelayed(tick, 1_000L)
            }
        } catch (error: Exception) {
            NotificationDiagnostics.failure("test.publish.failed", error, "run" to token)
            NotificationDiagnosticController.status(this, token, NotificationTestPhase.FAILED, error.javaClass.simpleName)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun publish(token: String, remaining: Long) {
        val title = getString(R.string.notification_test_title)
        val content = getString(R.string.notification_test_content, remaining)
        val clock = String.format(Locale.ROOT, "%02d:%02d", remaining / 60, remaining % 60)
        val open = PendingIntent.getActivity(this, NotificationDiagnosticController.NOTIFICATION_ID,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_focus_notification).setContentTitle(title).setContentText(content)
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setProgress(120, (120 - remaining).toInt(), false)
            .setWhen(System.currentTimeMillis() + remaining * 1_000L).setUsesChronometer(true).setChronometerCountDown(true)
        val notification = LiveUpdateNotificationAdapter.apply(this, builder, title, content, clock, R.drawable.ic_focus_notification).build()
        val observe = sequence++ % 10 == 0
        if (observe) NotificationDiagnostics.built(this, notification, token, NotificationDiagnosticController.NOTIFICATION_ID)
        IslandWhitelistBypass.publish(this, "notification-test") {
            if (run == token && NotificationDiagnosticController.currentRun(this) == token) {
                getSystemService(android.app.NotificationManager::class.java)
                    .notify(NotificationDiagnosticController.NOTIFICATION_ID, notification)
                if (observe) {
                    NotificationDiagnostics.event("test.published", "run" to token, "remainingSeconds" to remaining,
                        "dispatch" to "notify_inside_window")
                    NotificationDiagnostics.observe(this, token, NotificationDiagnosticController.NOTIFICATION_ID)
                }
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        NotificationDiagnostics.event("test.service.destroyed", "run" to run)
        run = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}
