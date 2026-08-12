package com.aritxonly.deadliner.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.aritxonly.deadliner.LauncherActivity
import com.aritxonly.deadliner.R
import com.deadliner.domain.habit.HabitPeriod
import com.deadliner.viewmodel.habit.HabitScheduleHintKind

class HabitMediumWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (appWidgetId in appWidgetIds) {
            updateMediumAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val provider = ComponentName(context, javaClass)
        for (appWidgetId in appWidgetIds) {
            deleteIdPref(context, appWidgetId, provider)
        }
        super.onDeleted(context, appWidgetIds)
    }

    companion object {
        fun updateWidget(context: Context,
                         appWidgetManager: AppWidgetManager,
                         appWidgetId: Int) {
            updateMediumAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}

internal fun updateMediumAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
) {
    val provider = ComponentName(context, HabitMediumWidget::class.java)
    val habitUid = loadUidPref(context, appWidgetId, provider)

    val views = RemoteViews(context.packageName, R.layout.habit_medium_widget)

    val status = habitUid?.let(::habitWidgetStatus)
    val habit = status?.habit

    if (habit != null) {
        // 标题 = DDL 名称
        views.setTextViewText(R.id.medium_title, habit.name)

        // 频率文案：沿用原本的 string 资源，但用 Habit 的字段
        val freqDesc = when (habit.period) {
            HabitPeriod.DAILY ->
                if (habit.totalTarget == null || habit.totalTarget == 0)
                    context.getString(R.string.daily_frequency, habit.timesPerPeriod)
                else
                    context.getString(
                        R.string.daily_frequency_with_total,
                        habit.timesPerPeriod,
                        habit.totalTarget
                    )

            HabitPeriod.WEEKLY ->
                if (habit.totalTarget == null || habit.totalTarget == 0)
                    context.getString(R.string.weekly_frequency, habit.timesPerPeriod)
                else
                    context.getString(
                        R.string.weekly_frequency_with_total,
                        habit.timesPerPeriod,
                        habit.totalTarget
                    )

            HabitPeriod.MONTHLY ->
                if (habit.totalTarget == null || habit.totalTarget == 0)
                    context.getString(R.string.monthly_frequency, habit.timesPerPeriod)
                else
                    context.getString(
                        R.string.monthly_frequency_with_total,
                        habit.timesPerPeriod,
                        habit.totalTarget
                    )
            HabitPeriod.EBBINGHAUS ->
                context.getString(R.string.editor_habit_period_ebbinghaus)
            HabitPeriod.ONCE ->
                context.getString(R.string.daily_frequency, 1)
        }
        val scheduleHint = when (status.scheduleState?.hintKind) {
            HabitScheduleHintKind.STARTS_IN -> status.scheduleState?.daysUntil?.let {
                context.getString(R.string.habit_ebbinghaus_starts_in_days, it)
            }
            HabitScheduleHintKind.NEXT_REVIEW_IN -> status.scheduleState?.daysUntil?.let {
                context.getString(R.string.habit_ebbinghaus_review_in_days, it)
            }
            HabitScheduleHintKind.COMPLETED_CYCLE -> context.getString(R.string.habit_ebbinghaus_cycle_completed)
            null -> null
        }
        views.setTextViewText(R.id.medium_description, scheduleHint?.let { "$freqDesc · $it" } ?: freqDesc)

        // 今天打卡次数 vs 每周期次数
        val isDueToday = status.scheduleState?.isDue != false
        val canClick = isDueToday && !status.isCompleted
        val label = if (canClick) {
            context.getString(R.string.check_habit)
        } else if (!isDueToday) {
            scheduleHint ?: context.getString(R.string.editor_habit_period_ebbinghaus)
        } else {
            context.getString(R.string.completed)   // 或 R.string.complete，看你现有资源
        }
        views.setTextViewText(R.id.tv_checkin, label)

        // 点击：能打卡 -> 广播；不能 -> 打开 App
        val pending = if (canClick) {
            PendingIntent.getBroadcast(
                context, appWidgetId,
                Intent(context, HabitMiniWidget::class.java).apply {
                    action = ACTION_CHECK_IN
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    putExtra(EXTRA_HABIT_UID, habitUid)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                context, appWidgetId,
                Intent(context, LauncherActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                ),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        views.setOnClickPendingIntent(R.id.btn_checkin, pending)

        views.setFloat(R.id.btn_checkin, "setAlpha", if (canClick) 1f else 0.6f)
    } else {
        // 没配好 / 已被删
        views.setTextViewText(R.id.medium_title, context.getString(R.string.app_name))
        views.setTextViewText(R.id.medium_description, "")
        views.setTextViewText(R.id.tv_checkin, context.getString(R.string.add_widget))
        views.setOnClickPendingIntent(
            R.id.btn_checkin,
            PendingIntent.getActivity(
                context, appWidgetId,
                Intent(context, LauncherActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    // 容器点击 → 打开 App（保持不变）
    views.setOnClickPendingIntent(
        R.id.widget_container,
        PendingIntent.getActivity(
            context, 0,
            Intent(context, LauncherActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    )

    appWidgetManager.updateAppWidget(appWidgetId, views)
}
