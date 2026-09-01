package com.aritxonly.deadliner.widgets.glance.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.aritxonly.deadliner.LauncherActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.widgets.glance.data.AndroidTaskWidgetStrings
import com.aritxonly.deadliner.widgets.glance.data.KmpTaskWidgetDataSource
import com.aritxonly.deadliner.widgets.glance.data.TaskWidgetRepository
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetItem
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetSnapshot
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetPaddingPreset
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetPreferences
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetBackgroundRenderer
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetColors
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetThemeResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CompactTaskGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(150.dp, 150.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = withContext(Dispatchers.IO) {
            TaskWidgetRepository(KmpTaskWidgetDataSource()).load(AndroidTaskWidgetStrings(context))
        }
        val colors = TaskWidgetThemeResolver.resolve(context)
        val background = TaskWidgetBackgroundRenderer.render(colors)
        val paddingPreset = TaskWidgetPreferences.readPaddingPreset(context)

        provideContent {
            CompactTaskWidgetContent(
                snapshot = snapshot,
                colors = colors,
                background = ImageProvider(background),
                paddingPreset = paddingPreset,
            )
        }
    }
}

@Composable
private fun CompactTaskWidgetContent(
    snapshot: TaskWidgetSnapshot,
    colors: TaskWidgetColors,
    background: ImageProvider,
    paddingPreset: TaskWidgetPaddingPreset,
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(imageProvider = background)
            .cornerRadius(24.dp)
            .appWidgetBackground()
            .clickable(actionStartActivity<LauncherActivity>()),
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(
                    start = 10.dp,
                    top = (10 + paddingPreset.extraTopPaddingDp).dp,
                    end = 10.dp,
                    bottom = 10.dp,
                ),
        ) {
            WidgetHeader(snapshot.totalCount, colors)
            Spacer(modifier = GlanceModifier.height(7.dp))

            if (snapshot.items.isEmpty()) {
                EmptyState(colors)
            } else {
                val visibleItems = snapshot.items.take(paddingPreset.maxVisibleTasks)
                visibleItems.forEachIndexed { index, item ->
                    CompactTaskItem(item, colors)
                    if (index != visibleItems.lastIndex) {
                        Spacer(modifier = GlanceModifier.height(5.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetHeader(totalCount: Int, colors: TaskWidgetColors) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(19.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.mipmap.ic_launcher),
            contentDescription = null,
            modifier = GlanceModifier
                .size(18.dp)
                .cornerRadius(4.dp),
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(
            text = "Deadliner",
            modifier = GlanceModifier.width(78.dp),
            style = TextStyle(
                color = colors.textPrimary.asProvider(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        if (totalCount > 0) {
            Text(
                text = totalCount.toString(),
                modifier = GlanceModifier
                    .background(colors.itemBackground.asProvider())
                    .cornerRadius(7.dp)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
                style = TextStyle(
                    color = (if (totalCount > 3) colors.urgent else colors.brand).asProvider(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CompactTaskItem(item: TaskWidgetItem, colors: TaskWidgetColors) {
    val indicator = if (item.isUrgent) colors.urgent else colors.brand
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(29.dp)
            .background(colors.itemBackground.asProvider())
            .cornerRadius(9.dp)
            .padding(start = 6.dp, end = 8.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .width(3.dp)
                .height(12.dp)
                .background(indicator.asProvider())
                .cornerRadius(2.dp),
        ) {}
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(
            text = item.title,
            modifier = GlanceModifier.width(55.dp),
            style = TextStyle(
                color = colors.textPrimary.asProvider(),
                fontSize = 12.sp,
                fontWeight = if (item.isUrgent) FontWeight.Medium else FontWeight.Normal,
            ),
            maxLines = 1,
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        Text(
            text = item.timeDescription,
            style = TextStyle(
                color = (if (item.isUrgent) colors.urgent else colors.textSecondary).asProvider(),
                fontSize = 10.sp,
                fontWeight = if (item.isUrgent) FontWeight.Medium else FontWeight.Normal,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun EmptyState(colors: TaskWidgetColors) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(100.dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = "🎉",
            style = TextStyle(fontSize = 28.sp),
        )
        Spacer(modifier = GlanceModifier.height(3.dp))
        Text(
            text = context.getString(R.string.glance_task_widget_all_done),
            style = TextStyle(
                color = colors.textPrimary.asProvider(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

private fun Int.asProvider(): ColorProvider = ColorProvider(Color(this))
