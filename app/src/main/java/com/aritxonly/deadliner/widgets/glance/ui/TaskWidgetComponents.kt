package com.aritxonly.deadliner.widgets.glance.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import androidx.glance.unit.ColorProvider
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetItem
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetSnapshot
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetColors

@Composable
internal fun TaskWidgetLayeredCard(
    background: ImageProvider,
    backgroundAlpha: Float,
    cornerRadiusDp: Int,
    openAppAction: Action,
    content: @Composable () -> Unit,
) {
    Box(modifier = GlanceModifier.fillMaxSize()) {
        Image(
            provider = background,
            contentDescription = null,
            alpha = backgroundAlpha,
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(cornerRadiusDp.dp)
                .appWidgetBackground()
                .clickable(openAppAction),
            contentScale = ContentScale.FillBounds,
        )
        content()
    }
}

@Composable
internal fun TaskWidgetHeader(snapshot: TaskWidgetSnapshot, colors: TaskWidgetColors) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(TaskWidgetLayoutMetrics.HEADER_HEIGHT_DP.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.mipmap.ic_launcher),
            contentDescription = null,
            modifier = GlanceModifier.size(18.dp).cornerRadius(4.dp),
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(
            text = "Deadliner",
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = colors.textPrimary.asProvider(),
                fontSize = TaskWidgetTypography.BRAND_SIZE_SP.sp,
                fontFamily = TaskWidgetTypography.EMPHASIS_FONT_FAMILY,
                fontWeight = FontWeight.Normal,
            ),
            maxLines = 1,
        )
        if (snapshot.totalCount > 0) {
            Text(
                text = snapshot.totalCount.toString(),
                modifier = GlanceModifier
                    .background(colors.badgeBackground.asProvider())
                    .cornerRadius(8.dp)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
                style = TextStyle(
                    color = colors.badgeContentColor(snapshot.visualState).asProvider(),
                    fontSize = TaskWidgetTypography.BADGE_SIZE_SP.sp,
                    fontFamily = TaskWidgetTypography.EMPHASIS_FONT_FAMILY,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun TaskWidgetItemRow(
    item: TaskWidgetItem,
    colors: TaskWidgetColors,
    openAppAction: Action?,
    modifier: GlanceModifier,
) {
    val rowColors = colors.rowColors(item.status)
    val rowModifier = modifier
        .fillMaxWidth()
        .background(colors.rowBackgroundColor(item.status).asProvider())
        .cornerRadius(9.dp)
        .let { base ->
            if (openAppAction == null) base else base.clickable(openAppAction)
        }
    Row(
        modifier = rowModifier
            .padding(start = 6.dp, end = 8.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .width(3.dp)
                .height(12.dp)
                .background(item.indicatorColorArgb(rowColors.indicator).asProvider())
                .cornerRadius(2.dp),
        ) {}
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(
            text = item.title,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = colors.textPrimary.asProvider(),
                fontSize = TaskWidgetTypography.TASK_TITLE_SIZE_SP.sp,
                fontFamily = TaskWidgetTypography.EMPHASIS_FONT_FAMILY,
                fontWeight = FontWeight.Normal,
            ),
            maxLines = 1,
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        Text(
            text = item.timeDescription,
            style = TextStyle(
                color = rowColors.timeText.asProvider(),
                fontSize = TaskWidgetTypography.TASK_TIME_SIZE_SP.sp,
                fontFamily = TaskWidgetTypography.SUPPORTING_FONT_FAMILY,
                fontWeight = FontWeight.Normal,
            ),
            maxLines = 1,
        )
    }
}

@Composable
internal fun TaskWidgetEmptyState(
    colors: TaskWidgetColors,
    modifier: GlanceModifier = GlanceModifier.height(100.dp),
) {
    val context = LocalContext.current
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.drawable.msr_check_circle_fill_wght400),
            contentDescription = null,
            modifier = GlanceModifier.size(30.dp),
            colorFilter = ColorFilter.tint(colors.completed.asProvider()),
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = context.getString(R.string.glance_task_widget_all_done),
            style = TextStyle(
                color = colors.textPrimary.asProvider(),
                fontSize = TaskWidgetTypography.EMPTY_SIZE_SP.sp,
                fontFamily = TaskWidgetTypography.EMPHASIS_FONT_FAMILY,
                fontWeight = FontWeight.Normal,
            ),
        )
    }
}

internal fun Int.asProvider(): ColorProvider = ColorProvider(Color(this))
