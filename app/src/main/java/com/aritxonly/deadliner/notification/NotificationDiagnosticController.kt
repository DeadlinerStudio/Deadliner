package com.aritxonly.deadliner.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

internal enum class NotificationTestPhase { IDLE, SCHEDULED, RUNNING, FINISHED, BLOCKED, FAILED }
internal data class NotificationTestState(val phase: NotificationTestPhase = NotificationTestPhase.IDLE, val run: String? = null, val detail: String = "")

/** Synthetic test: no task writes, one replaceable pending alarm, one foreground notification. */
internal object NotificationDiagnosticController {
    private val mutableState = MutableStateFlow(NotificationTestState())
    val state = mutableState.asStateFlow()
    const val EXTRA_RUN = "notification_test_run"
    const val NOTIFICATION_ID = 520_021
    private const val PREFERENCES = "notification_diagnostic"

    fun currentRun(context: Context): String? = context.getSharedPreferences(PREFERENCES, 0).getString("run", null)
    fun status(context: Context, run: String, phase: NotificationTestPhase, detail: String = "") {
        if (currentRun(context) == run) mutableState.value = NotificationTestState(phase, run, detail)
    }

    fun start(context: Context, throughAlarm: Boolean) {
        stop(context)
        val run = UUID.randomUUID().toString()
        context.getSharedPreferences(PREFERENCES, 0).edit().putString("run", run).apply()
        NotificationUtil.createNotificationChannels(context)
        NotificationDiagnostics.event("test.requested", "run" to run, "path" to if (throughAlarm) "alarm" else "direct")
        NotificationDiagnostics.snapshot(context, run)
        NotificationDiagnostics.postingBlock(context)?.let {
            status(context, run, NotificationTestPhase.BLOCKED, it)
            NotificationDiagnostics.event("test.blocked", "run" to run, "reason" to it)
            return
        }
        if (!throughAlarm) return launch(context, run)
        val manager = context.getSystemService(AlarmManager::class.java)
        val trigger = SystemClock.elapsedRealtime() + 10_000L
        val pending = pending(context, run, PendingIntent.FLAG_UPDATE_CURRENT)!!
        try {
            val exact = manager.canScheduleExactAlarms()
            if (exact) manager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pending)
            else manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pending)
            status(context, run, NotificationTestPhase.SCHEDULED, if (exact) "exact" else "inexact")
            NotificationDiagnostics.event("test.alarm.scheduled", "run" to run, "exact" to exact, "triggerElapsed" to trigger)
        } catch (error: Exception) {
            status(context, run, NotificationTestPhase.FAILED, error.javaClass.simpleName)
            NotificationDiagnostics.failure("test.alarm.failed", error, "run" to run)
        }
    }

    fun launch(context: Context, run: String) {
        if (currentRun(context) != run) {
            NotificationDiagnostics.event("test.stale", "run" to run)
            return
        }
        try {
            NotificationDiagnostics.snapshot(context, run)
            context.startForegroundService(Intent(context, NotificationDiagnosticService::class.java).putExtra(EXTRA_RUN, run))
            NotificationDiagnostics.event("test.service.requested", "run" to run)
        } catch (error: Exception) {
            status(context, run, NotificationTestPhase.FAILED, error.javaClass.simpleName)
            NotificationDiagnostics.failure("test.service.failed", error, "run" to run)
        }
    }

    fun stop(context: Context) {
        val run = currentRun(context)
        context.getSharedPreferences(PREFERENCES, 0).edit().remove("run").apply()
        pending(context, "", PendingIntent.FLAG_NO_CREATE)?.let {
            context.getSystemService(AlarmManager::class.java).cancel(it)
            it.cancel()
        }
        context.stopService(Intent(context, NotificationDiagnosticService::class.java))
        context.getSystemService(android.app.NotificationManager::class.java).cancel(NOTIFICATION_ID)
        mutableState.value = NotificationTestState()
        NotificationDiagnostics.event("test.stopped", "run" to run)
    }

    private fun pending(context: Context, run: String, flags: Int): PendingIntent? = PendingIntent.getBroadcast(
        context, NOTIFICATION_ID,
        Intent(context, NotificationDiagnosticReceiver::class.java)
            .setData(Uri.parse("deadliner://notification-diagnostic"))
            .putExtra(EXTRA_RUN, run),
        flags or PendingIntent.FLAG_IMMUTABLE,
    )
}

class NotificationDiagnosticReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val run = intent.getStringExtra(NotificationDiagnosticController.EXTRA_RUN) ?: return
        NotificationDiagnostics.event("test.alarm.received", "run" to run, "elapsed" to SystemClock.elapsedRealtime())
        NotificationDiagnosticController.launch(context, run)
    }
}
