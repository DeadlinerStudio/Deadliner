package com.aritxonly.deadliner.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.category.CategoryPresentation
import com.aritxonly.deadliner.ui.category.CategoryPresentationRegistry
import com.aritxonly.deadliner.ui.iconResource
import com.deadliner.domain.task.Task
import com.deadliner.domain.task.TaskState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DetailTaskMetaCard(deadline: Task, categories: List<CategoryPresentation>, onEdit: () -> Unit) {
    val startTime = deadline.startAt?.let(GlobalUtils::safeParseDateTime) ?: LocalDateTime.now()
    val endTime = deadline.dueAt?.let(GlobalUtils::safeParseDateTime) ?: startTime
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.detail_task_info_title), style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f))
                // Keep the 48dp hit target without making the heading row taller than its text.
                DetailTextAction(stringResource(R.string.edit), R.drawable.msr_edit_wght400, onEdit,
                    modifier = Modifier.height(24.dp).wrapContentHeight(unbounded = true))
            }
            Text(stringResource(R.string.label_start_time, startTime.format(formatter)),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.label_end_time, endTime.format(formatter)),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val scheme = MiuixTheme.colorScheme
                val label = when (deadline.state) {
                    TaskState.ACTIVE -> R.string.state_active
                    TaskState.COMPLETED -> R.string.state_completed
                    TaskState.ARCHIVED -> R.string.state_archived
                    TaskState.ABANDONED -> R.string.state_abandoned
                    TaskState.ABANDONED_ARCHIVED -> R.string.state_abandoned_archived
                }
                val stateColor = when (deadline.state) {
                    TaskState.ACTIVE, TaskState.COMPLETED -> scheme.primary
                    TaskState.ABANDONED, TaskState.ABANDONED_ARCHIVED -> scheme.error
                    TaskState.ARCHIVED -> scheme.onSurfaceVariantSummary
                }
                DetailBadge(stringResource(label), stateColor, Modifier.testTag("detail-state-badge"))
                val category = categories.firstOrNull { it.uid == deadline.categoryUid }
                DetailBadge(
                    label = category?.name ?: stringResource(R.string.category_uncategorized),
                    color = category?.let { CategoryPresentationRegistry.color(it.colorToken) }
                        ?: scheme.onSurfaceVariantSummary,
                    modifier = Modifier.testTag("detail-category-badge"),
                    iconRes = category?.let { CategoryPresentationRegistry.icon(it.iconSemanticId) },
                )
            }
        }
    }
}

@Composable
private fun DetailBadge(label: String, color: Color, modifier: Modifier = Modifier, iconRes: Int? = null) {
    Row(
        modifier.heightIn(min = 28.dp).background(color.copy(alpha = 0.15f), RoundedCornerShape(percent = 50))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconRes != null) {
            Icon(iconResource(iconRes), contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
            Spacer(Modifier.width(4.dp))
        }
        Text(label, color = color,
            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp, lineHeight = 18.sp),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
