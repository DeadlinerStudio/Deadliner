package com.aritxonly.deadliner.notification

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationManagerCompat
import com.aritxonly.deadliner.DeadlineAlarmScheduler
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineType
import java.time.Duration
import java.time.LocalDateTime

class UpcomingLiveUpdateService : Service() {

    companion object {
        @Volatile
        private var activeDdlId: Long? = null

        fun start(context: Context, ddl: DDLItem) {
            val i = Intent(context, UpcomingLiveUpdateService::class.java)
                .putExtra("DDL_ID", ddl.id)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else context.startService(i)
        }

        fun stop(context: Context, ddlId: Long) {
            NotificationManagerCompat.from(context).cancel(ddlId.hashCode())
            if (activeDdlId == ddlId) {
                context.stopService(Intent(context, UpcomingLiveUpdateService::class.java))
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var ddl: DDLItem? = null
    private var notificationId = 0

    private val tick = object : Runnable {
        override fun run() {
            val d = ddl ?: return
            val latest = DatabaseHelper.getInstance(this@UpcomingLiveUpdateService)
                .getDDLById(d.id) ?: return stopSelf()

            ddl = latest
            val remaining = calculateRemainingSeconds(latest)
            val liveWindowSeconds = GlobalUtils.liveUpdatesInAdvance * 60L

            if (latest.type != DeadlineType.TASK || !latest.state.isActionable()) {
                stopLiveUpdate(latest.id)
                return
            }

            if (remaining > liveWindowSeconds) {
                DeadlineAlarmScheduler.scheduleUpcomingDDLAlarm(this@UpcomingLiveUpdateService, latest)
                stopLiveUpdate(latest.id)
                return
            }

            if (remaining <= -300) {
                stopLiveUpdate(latest.id)
                return
            }

            // 到期或完成就做最后一版并退出
            if (remaining <= 0) {
                NotificationUtil.sendUpcomingDDLNotification(this@UpcomingLiveUpdateService, latest, remaining)
                stopSelf()
                return
            }

            // 常规 30s 刷新
            NotificationUtil.sendUpcomingDDLNotification(this@UpcomingLiveUpdateService, latest, remaining)
            handler.postDelayed(this, 30_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        NotificationUtil.createNotificationChannels(this) // 你项目里已有的话保留
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ddlId = intent?.getLongExtra("DDL_ID", -1L) ?: -1L
        if (ddlId <= 0) return START_NOT_STICKY

        // 拉取最新 DDL
        val d = DatabaseHelper.getInstance(this).getDDLById(ddlId) ?: return START_NOT_STICKY
        val remaining = calculateRemainingSeconds(d)
        val liveWindowSeconds = GlobalUtils.liveUpdatesInAdvance * 60L

        if (d.type != DeadlineType.TASK || !d.state.isActionable()) {
            stopLiveUpdate(ddlId)
            return START_NOT_STICKY
        }
        if (remaining <= 0L || remaining > liveWindowSeconds) {
            if (remaining > liveWindowSeconds) {
                DeadlineAlarmScheduler.scheduleUpcomingDDLAlarm(this, d)
            }
            stopLiveUpdate(ddlId)
            return START_NOT_STICKY
        }

        ddl = d
        activeDdlId = d.id
        notificationId = d.id.hashCode()

        // 首帧：用 NotificationUtil 构建通知并前台化
        val first = NotificationUtil.createUpcomingDDLNotification(this, d, remaining)
        startForeground(notificationId, first)

        // 周期刷新
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(tick, 30_000L)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (activeDdlId == ddl?.id) {
            activeDdlId = null
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun calculateRemainingSeconds(d: DDLItem): Long {
        val now = LocalDateTime.now()
        val end = GlobalUtils.safeParseDateTime(d.endTime)
        return Duration.between(now, end).seconds
    }

    private fun stopLiveUpdate(ddlId: Long) {
        stopForeground(STOP_FOREGROUND_REMOVE)
        NotificationManagerCompat.from(this).cancel(ddlId.hashCode())
        stopSelf()
    }
}
