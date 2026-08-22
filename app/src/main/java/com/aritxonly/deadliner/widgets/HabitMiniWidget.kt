package com.aritxonly.deadliner.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import com.aritxonly.deadliner.LauncherActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import java.time.LocalDate
import java.time.Instant

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [HabitWidgetConfigureActivity]
 */
class HabitMiniWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppMiniHabitWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val provider = ComponentName(context, javaClass)
        // When the user deletes the widget, delete the preference associated with it.
        for (appWidgetId in appWidgetIds) {
            deleteIdPref(context, appWidgetId, provider)
        }
        super.onDeleted(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (context == null || intent == null) return

        when (intent.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_CONFIGURATION_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> {
                // 日期/时间/时区变化时，刷新全部 → 按当天状态更新“打卡/已完成”
                refreshAllWidgets(context)
                return
            }
        }

        if (intent.action == ACTION_CHECK_IN) {
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            val habitUid = intent.getStringExtra(EXTRA_HABIT_UID)
            if (widgetId == -1 || habitUid == null) return

            val status = habitWidgetStatus(habitUid)
            if (status == null) {
                // 对应任务或习惯已经不存在，刷新一下 widget 做兜底
                refreshOneWidget(context, widgetId)
                return
            }

            val canClick = status.canApplyPrimaryAction

            if (!canClick) {
                // 今天已经到上限了，提示一下，然后刷新 UI
                Toast.makeText(
                    context,
                    context.getString(R.string.snackbar_already_checkin),
                    Toast.LENGTH_SHORT
                ).show()
                refreshOneWidget(context, widgetId)
                return
            }

            // shared ViewModel 决定执行勾选还是按 defaultIncrement 增量记录。
            KmpV6Runtime.habitListViewModel.toggleHabitRecordNow(
                habitUid,
                LocalDate.now().toString(),
                Instant.now().toString(),
            )

            Toast.makeText(context, R.string.habit_success, Toast.LENGTH_SHORT).show()
            refreshOneWidget(context, widgetId)
        }
    }

    private fun refreshOneWidget(context: Context, appWidgetId: Int) {
        val awm = AppWidgetManager.getInstance(context)
        val info = awm.getAppWidgetInfo(appWidgetId)
        if (info != null) {
            when (info.provider.className) {
                HabitMiniWidget::class.java.name -> updateAppMiniHabitWidget(context, awm, appWidgetId)
                HabitMediumWidget::class.java.name -> updateMediumAppWidget(context, awm, appWidgetId)
                else -> updateAppMiniHabitWidget(context, awm, appWidgetId) // 默认兜底
            }
        } else {
            // 极少数情况（系统已移除 info），可以尝试两类都刷一下（安全起见一般不需要）
            updateAppMiniHabitWidget(context, awm, appWidgetId)
        }
    }

    private fun refreshAllWidgets(context: Context?) {
        if (context == null) return
        val awm = AppWidgetManager.getInstance(context)

        // 刷新 Mini
        awm.getAppWidgetIds(ComponentName(context, HabitMiniWidget::class.java))
            .forEach { updateAppMiniHabitWidget(context, awm, it) }

        // 刷新 Medium
        awm.getAppWidgetIds(ComponentName(context, HabitMediumWidget::class.java))
            .forEach { updateMediumAppWidget(context, awm, it) }
    }

    companion object {
        fun updateWidget(context: Context,
                         appWidgetManager: AppWidgetManager,
                         appWidgetId: Int) {
            updateMediumAppWidget(context, appWidgetManager, appWidgetId)
            updateAppMiniHabitWidget(context, appWidgetManager, appWidgetId)
        }
    }
}

internal const val ACTION_CHECK_IN = "com.aritxonly.deadliner.ACTION_CHECK_IN"
internal const val EXTRA_HABIT_UID = "extra_habit_uid"

internal fun updateAppMiniHabitWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
) {
    val awm = AppWidgetManager.getInstance(context)
    val info = awm.getAppWidgetInfo(appWidgetId)
    val provider = info.provider

    val habitUid = loadUidPref(context, appWidgetId, provider)

    val mainIntent = Intent(context, LauncherActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val containerPi = PendingIntent.getActivity(
        context,
        0,
        mainIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val checkInIntent = Intent(context, HabitMiniWidget::class.java).apply {
        action = ACTION_CHECK_IN
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        putExtra(EXTRA_HABIT_UID, habitUid)
    }
    val checkInPi = PendingIntent.getBroadcast(
        context,
        appWidgetId,
        checkInIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val status = habitUid?.let(::habitWidgetStatus)

    val views = RemoteViews(context.packageName, R.layout.habit_mini_widget).apply {
        if (status == null) {
            // 任务/习惯不存在，给一个占位内容
            setTextViewText(R.id.mini_text, context.getString(R.string.app_name))
            setFloat(R.id.btn_check_in, "setAlpha", 0.6f)
        } else {
            setTextViewText(R.id.mini_text, status.habit.name)
            val canClick = status.canApplyPrimaryAction

            setFloat(R.id.btn_check_in, "setAlpha", if (canClick) 1f else 0.6f)
        }

        setOnClickPendingIntent(R.id.widget_container, containerPi)
        setOnClickPendingIntent(R.id.btn_check_in, checkInPi)
    }

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

internal fun habitWidgetStatus(uid: String) =
    KmpV6Runtime.habitListViewModel.loadNow().visibleHabits.firstOrNull { it.habit.uid == uid }
