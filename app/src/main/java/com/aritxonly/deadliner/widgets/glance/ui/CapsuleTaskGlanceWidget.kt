package com.aritxonly.deadliner.widgets.glance.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.aritxonly.deadliner.LauncherActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.widgets.glance.data.AndroidTaskWidgetStrings
import com.aritxonly.deadliner.widgets.glance.data.KmpTaskWidgetDataSource
import com.aritxonly.deadliner.widgets.glance.data.TaskWidgetRepository
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetSnapshot
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetBackgroundRenderer
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetColors
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetThemeResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CapsuleTaskGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(150.dp, 70.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = withContext(Dispatchers.IO) {
            TaskWidgetRepository(KmpTaskWidgetDataSource()).load(AndroidTaskWidgetStrings(context))
        }
        val colors = TaskWidgetThemeResolver.resolve(context, snapshot.visualState)

        provideContent {
            CapsuleTaskWidgetContent(
                snapshot = snapshot,
                colors = colors,
                background = ImageProvider(TaskWidgetBackgroundRenderer.renderCapsule(colors)),
            )
        }
    }
}

@Composable
private fun CapsuleTaskWidgetContent(
    snapshot: TaskWidgetSnapshot,
    colors: TaskWidgetColors,
    background: ImageProvider,
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(imageProvider = background)
            .cornerRadius(22.dp)
            .appWidgetBackground()
            .clickable(actionStartActivity<LauncherActivity>()),
        contentAlignment = Alignment.Center,
    ) {
        CapsuleMainRow(snapshot = snapshot, colors = colors)
        if (snapshot.totalCount > 0) {
            Box(
                modifier = GlanceModifier.fillMaxSize().padding(top = 8.dp, end = 10.dp),
                contentAlignment = Alignment.TopEnd,
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(7.dp)
                        .background(colors.badgeContentColor(snapshot.visualState).asProvider())
                        .cornerRadius(4.dp),
                ) {}
            }
        }
    }
}

@Composable
private fun CapsuleMainRow(snapshot: TaskWidgetSnapshot, colors: TaskWidgetColors) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(start = 13.dp, top = 9.dp, end = 16.dp, bottom = 9.dp),
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            if (snapshot.totalCount > 0) {
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = context.getString(R.string.glance_task_widget_keep_going),
                    style = TextStyle(
                        color = colors.brandSoft.asProvider(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
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
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    }
}
