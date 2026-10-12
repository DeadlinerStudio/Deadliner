package com.aritxonly.deadliner.widgets.glance.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.widgets.glance.data.TaskWidgetSnapshotLoader
import com.aritxonly.deadliner.widgets.glance.interaction.TaskWidgetActions
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetRenderStateSource
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetSnapshot
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetConfigurationStore
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetKind
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetBackgroundRenderer
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetColors
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetThemeResolver

class CapsuleTaskGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = TaskWidgetSnapshotLoader.load(context)
        TaskWidgetRenderStateSource.store.publish(snapshot)
        val appWidgetId = (id as AppWidgetId).appWidgetId
        provideContent {
            CapsuleTaskWidgetContent(
                snapshot = snapshot,
                appWidgetId = appWidgetId,
            )
        }
    }
}

@Composable
private fun CapsuleTaskWidgetContent(
    snapshot: TaskWidgetSnapshot,
    appWidgetId: Int,
) {
    val renderState by TaskWidgetRenderStateSource.store.state.collectAsState()
    val currentSnapshot = renderState.snapshot ?: snapshot
    val context = LocalContext.current
    val configuration = remember(renderState.revision, appWidgetId) {
        TaskWidgetConfigurationStore.from(context).read(
            appWidgetId = appWidgetId,
            kind = TaskWidgetKind.CAPSULE,
        )
    }
    val colors = TaskWidgetThemeResolver.resolve(
        context = context,
        visualState = currentSnapshot.visualState,
        widgetColorScheme = configuration.colorScheme,
        themeMode = configuration.themeMode,
    )
    val background = remember(colors) {
        ImageProvider(TaskWidgetBackgroundRenderer.renderCapsule(colors))
    }
    val openAppAction = remember(appWidgetId) {
        TaskWidgetActions.openAppAction(context, appWidgetId)
    }
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(
                top = configuration.topMarginDp().dp,
                bottom = configuration.bottomMarginDp().dp,
            ),
    ) {
        TaskWidgetLayeredCard(
            background = background,
            backgroundAlpha = configuration.backgroundAlpha(),
            cornerRadiusDp = configuration.cornerRadiusDp,
            openAppAction = openAppAction,
        ) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CapsuleMainRow(
                    snapshot = currentSnapshot,
                    colors = colors,
                )
                if (currentSnapshot.totalCount > 0) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(top = 8.dp, end = 10.dp),
                        contentAlignment = Alignment.TopEnd,
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .size(7.dp)
                                .background(colors.badgeContentColor(currentSnapshot.visualState).asProvider())
                                .cornerRadius(4.dp),
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun CapsuleMainRow(
    snapshot: TaskWidgetSnapshot,
    colors: TaskWidgetColors,
) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(start = 13.dp, top = 9.dp, end = 16.dp, bottom = 9.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = snapshot.summary,
                style = TextStyle(
                    color = colors.textPrimary.asProvider(),
                    fontSize = TaskWidgetTypography.CAPSULE_SUMMARY_SIZE_SP.sp,
                    fontFamily = TaskWidgetTypography.EMPHASIS_FONT_FAMILY,
                    fontWeight = FontWeight.Normal,
                ),
                maxLines = 1,
            )
            if (snapshot.totalCount > 0) {
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = context.getString(R.string.glance_task_widget_keep_going),
                    style = TextStyle(
                        color = colors.brandSoft.asProvider(),
                        fontSize = TaskWidgetTypography.CAPSULE_SUPPORTING_SIZE_SP.sp,
                        fontFamily = TaskWidgetTypography.SUPPORTING_FONT_FAMILY,
                        fontWeight = FontWeight.Normal,
                    ),
                    maxLines = 1,
                )
            }
        }
        Spacer(modifier = GlanceModifier.width(8.dp))
        if (snapshot.totalCount == 0) {
            Image(
                provider = ImageProvider(R.drawable.msr_check_circle_fill_wght400),
                contentDescription = null,
                modifier = GlanceModifier.size(34.dp),
                colorFilter = ColorFilter.tint(colors.completed.asProvider()),
            )
        } else {
            Text(
                text = snapshot.totalCount.toString(),
                style = TextStyle(
                    color = colors.badgeContentColor(snapshot.visualState).asProvider(),
                    fontSize = TaskWidgetTypography.CAPSULE_COUNT_SIZE_SP.sp,
                    fontFamily = TaskWidgetTypography.EMPHASIS_FONT_FAMILY,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    }
}
