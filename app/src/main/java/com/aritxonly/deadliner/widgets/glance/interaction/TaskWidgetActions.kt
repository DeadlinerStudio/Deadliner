package com.aritxonly.deadliner.widgets.glance.interaction

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.app.PendingIntent
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity
import com.aritxonly.deadliner.LauncherActivity

/** Explicit per-instance action shared by every interaction target within one task widget. */
object TaskWidgetActions {
    private const val URI_SCHEME = "deadliner"
    private const val URI_AUTHORITY = "widget"

    fun openAppAction(context: Context, appWidgetId: Int): Action = actionStartActivity(
        openAppIntent(context, appWidgetId),
    )

    fun openCollectionPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            appWidgetId xor Int.MIN_VALUE,
            openAppIntent(context, appWidgetId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

    internal fun openAppIntent(context: Context, appWidgetId: Int): Intent =
        Intent(context, LauncherActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.Builder()
                .scheme(URI_SCHEME)
                .authority(URI_AUTHORITY)
                .appendPath("open")
                .appendPath(appWidgetId.toString())
                .build()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
}
