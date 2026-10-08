package com.aritxonly.deadliner.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.platform.NotificationMetadataEntry
import com.google.gson.Gson

class NotificationMetadataReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AndroidNotificationMetadataScheduler.ACTION) return
        NotificationDiagnostics.event("metadata.alarm.received", "key" to intent.getStringExtra(AndroidNotificationMetadataScheduler.EXTRA_KEY))
        try {
            KmpV6Runtime.initialize(context)
            val entry = runCatching { Gson().fromJson(intent.getStringExtra(AndroidNotificationMetadataScheduler.EXTRA_ENTRY), NotificationMetadataEntry::class.java) }.getOrNull()
            if (entry != null) {
                if (KmpV6Runtime.isNotificationEntryValid(entry)) AndroidNotificationMetadataScheduler(context).deliver(entry)
                else NotificationDiagnostics.event("metadata.alarm.rejected", "key" to entry.key, "reason" to "stale_or_disabled")
            }
            KmpV6Runtime.triggerNotificationInfoUpdate()
        } catch (error: Exception) { NotificationDiagnostics.failure("metadata.alarm.failed", error) }
    }
}
