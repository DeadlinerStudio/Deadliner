package com.aritxonly.deadliner.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aritxonly.deadliner.DeadlineAlarmScheduler
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DeadlineType

class UpcomingLiveUpdatesReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 获取DDL ID
        val ddlId = intent.getLongExtra("DDL_ID", -1)

        Log.d("AlarmDebug", "LiveUpdates收到闹钟广播！DDL_ID: $ddlId")

        if (ddlId <= 0) return

        val ddl = DatabaseHelper.getInstance(context).getDDLById(ddlId) ?: return

        if (ddl.type != DeadlineType.TASK || !ddl.state.isActionable()) {
            DeadlineAlarmScheduler.syncScheduledNotifications(context, ddl)
            return
        }

        val remaining = DeadlineAlarmScheduler.calculateRemainingTime(ddl)
        val liveWindowSeconds = GlobalUtils.liveUpdatesInAdvance * 60L

        if (remaining <= 0L) return

        if (remaining > liveWindowSeconds) {
            UpcomingLiveUpdateService.stop(context, ddl.id)
            DeadlineAlarmScheduler.scheduleUpcomingDDLAlarm(context, ddl)
            return
        }

        UpcomingLiveUpdateService.start(context, ddl)
    }
}
