package com.aritxonly.deadliner.notification

import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import com.deadliner.domain.task.Task

internal object LiveUpdateNotificationAdapter {
    fun apply(context: Context, builder: NotificationCompat.Builder, title: String,
              content: String, status: String, @DrawableRes icon: Int, ongoing: Boolean = true, task: Task? = null): NotificationCompat.Builder {
        builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (!ongoing) return builder
        // Capsulyric selects native Live Update and Super Island as separate surfaces.
        if (shouldRequestPromotion(context)) {
            builder.setRequestPromotedOngoing(true).setShortCriticalText(status)
        }
        if (usesIsland(context)) builder.addExtras(
            if (task == null) XiaomiIslandExtras.create(context, title, content, status, icon)
            else XiaomiIslandExtras.createUpcoming(context, task))
        return builder
    }

    internal fun usesIsland(context: Context): Boolean = supportsIsland() &&
        (Build.VERSION.SDK_INT < 36 || IslandWhitelistBypass.mode(context) != IslandBypassMode.OFF)

    internal fun shouldRequestPromotion(context: Context, sdk: Int = Build.VERSION.SDK_INT): Boolean =
        sdk >= 36 && !(supportsIsland() && IslandWhitelistBypass.mode(context) != IslandBypassMode.OFF)

    fun supportsIsland(): Boolean = Build.VERSION.SDK_INT >= 35 &&
        listOf("xiaomi", "redmi", "poco").any {
            Build.MANUFACTURER.equals(it, true) || Build.BRAND.equals(it, true)
        }
}
