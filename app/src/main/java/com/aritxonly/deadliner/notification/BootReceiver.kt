package com.aritxonly.deadliner.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aritxonly.deadliner.localutils.AppIconManager
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        GlobalUtils.init(context)

        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                AppIconManager.applyCurrentMode(context)
                KmpV6Runtime.initialize(context)
                KmpV6Runtime.refreshNotifications()
            }
        }
    }
}
