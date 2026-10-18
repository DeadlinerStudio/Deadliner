package com.aritxonly.deadliner.capture.ui

import androidx.compose.ui.draw.clip
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.main.SwipeableItemCard
import com.aritxonly.deadliner.ui.symbolPainterResource
import com.deadliner.domain.capture.CaptureItem
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

@Composable
internal fun InspirationLightCard(
    item: CaptureItem,
    selected: Boolean,
    activeSwipeKey: String?,
    onActiveSwipeChange: (String?) -> Unit,
    inMultiSelectMode: Boolean,
    modifier: Modifier = Modifier,
    useHorizontalPadding: Boolean = true,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onAiTask: () -> Unit,
    onAiHabit: () -> Unit
) {
    val context = LocalContext.current
    val sectionShape = RoundedCornerShape(24.dp)
    val cardColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    SwipeableItemCard(
        swipeKey = "capture:${item.uid}", activeSwipeKey = activeSwipeKey,
        onActiveSwipeChange = onActiveSwipeChange,
        primaryActionLabel = stringResource(R.string.capture_swipe_ai_task),
        secondaryActionLabel = stringResource(R.string.capture_swipe_ai_habit),
        primaryActionIcon = symbolPainterResource(R.drawable.msr_task_alt_wght400),
        secondaryActionIcon = com.aritxonly.deadliner.ui.materialSymbolPainter("repeat"),
        primaryActionColor = MaterialTheme.colorScheme.primary,
        secondaryActionColor = com.aritxonly.deadliner.ui.theme.rememberTaskSwipeActionColors().complete,
        onPrimaryAction = onAiTask, onSecondaryAction = onAiHabit, onDeleteAction = onDelete,
        fullSwipePrimaryActionEnabled = false, selectionMode = inMultiSelectMode,
        accessibilityLabel = item.text,
        modifier = modifier.fillMaxWidth()
            .then(if (useHorizontalPadding) Modifier.padding(horizontal = 16.dp) else Modifier),
    ) { closeOrRun ->
        Card(
            modifier = Modifier.fillMaxWidth().clip(sectionShape).clickable { closeOrRun(onClick) },
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = sectionShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    painter = com.aritxonly.deadliner.ui.symbolPainterResource(
                        sourceId = if (inMultiSelectMode) {
                            if (selected) R.drawable.msr_check_circle_fill_wght400 else R.drawable.msr_check_circle_wght400
                        } else {
                            R.drawable.msr_format_quote_wght400
                        }
                    ),
                    contentDescription = null,
                    tint = if (inMultiSelectMode) {
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.80f)
                    },
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(if (inMultiSelectMode) 22.dp else 18.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = formatRelativeTime(context, item.updatedAt),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(
                                if (inMultiSelectMode) {
                                    R.string.capture_card_tip_selected
                                } else {
                                    R.string.capture_card_tip
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatRelativeTime(context: Context, timestamp: String): String {
    val timestampMillis = runCatching { java.time.Instant.parse(timestamp).toEpochMilli() }.getOrDefault(0L)
    val nowMillis = System.currentTimeMillis()
    val diff = Duration.ofMillis((nowMillis - timestampMillis).absoluteValue)
    val minutes = diff.toMinutes()
    val hours = diff.toHours()
    val days = diff.toDays()
    return when {
        minutes < 1 -> context.getString(R.string.capture_relative_just_now)
        minutes < 60 -> context.getString(R.string.capture_relative_minutes_ago, minutes)
        hours < 24 -> context.getString(R.string.capture_relative_hours_ago, hours)
        days < 7 -> context.getString(R.string.capture_relative_days_ago, days)
        else -> {
            val date = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestampMillis),
                ZoneId.systemDefault()
            )
            date.format(DateTimeFormatter.ofPattern(context.getString(R.string.capture_date_short_pattern)))
        }
    }
}
