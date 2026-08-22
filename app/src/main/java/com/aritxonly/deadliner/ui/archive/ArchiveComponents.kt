package com.aritxonly.deadliner.ui.archive

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.main.simplified.presentationLabel
import com.deadliner.domain.habit.Habit
import com.deadliner.domain.habit.HabitGoalWindow
import com.deadliner.domain.task.Task
import com.deadliner.domain.task.TaskState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val ArchiveDateFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

fun formatArchiveDate(value: String): String {
    val date = GlobalUtils.parseDateTime(value) ?: return value.ifBlank { "Unknown time" }
    return date.format(ArchiveDateFormatter)
}

fun formatArchiveDate(value: LocalDateTime): String = value.format(ArchiveDateFormatter)

internal data class ArchivedTaskCardModel(
    val badgeText: String,
    val title: String,
    val primaryText: String,
    val secondaryText: String,
    val note: String,
    val isAbandoned: Boolean,
)

internal data class ArchivedHabitCardModel(
    val title: String,
    val primaryText: String,
    val secondaryText: String,
    val note: String,
)

@Composable
internal fun Task.toArchiveCardModel(): ArchivedTaskCardModel {
    val abandoned = state == TaskState.ABANDONED_ARCHIVED
    val archivedAt = completedAt
    return ArchivedTaskCardModel(
        badgeText = stringResource(
            if (abandoned) R.string.archive_label_abandoned else R.string.archive_label_completed,
        ),
        title = title,
        primaryText = formatArchiveDate(startAt.orEmpty()),
        secondaryText = when {
            archivedAt.isNullOrBlank() && abandoned -> stringResource(R.string.archive_state_abandoned)
            archivedAt.isNullOrBlank() -> stringResource(R.string.archive_state_archived)
            abandoned -> stringResource(R.string.archive_abandoned_at, formatArchiveDate(archivedAt))
            else -> stringResource(R.string.archive_completed_at, formatArchiveDate(archivedAt))
        },
        note = note,
        isAbandoned = abandoned,
    )
}

@Composable
internal fun Habit.toArchiveCardModel(): ArchivedHabitCardModel {
    val context = LocalContext.current
    val goalLabel = when (goal.window) {
        HabitGoalWindow.LIFETIME -> null
        else -> tracking.unit?.takeIf(String::isNotBlank)?.let { "${goal.targetCount} $it" }
            ?: stringResource(R.string.archive_habit_period_target, goal.targetCount)
    }
    return ArchivedHabitCardModel(
        title = name,
        primaryText = listOfNotNull(presentationLabel(context), goalLabel).distinct().joinToString(" · "),
        secondaryText = stringResource(R.string.archive_archived_at, formatArchiveDate(updatedAt)),
        note = description.orEmpty(),
    )
}

@Composable
internal fun ArchivedTaskCard(
    item: ArchivedTaskCardModel,
    modifier: Modifier = Modifier,
    onRestore: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    ArchivedItemCard(
        badgeText = item.badgeText,
        title = item.title,
        primaryText = item.primaryText,
        secondaryText = item.secondaryText,
        note = item.note,
        indicatorColor = if (item.isAbandoned) {
            MaterialTheme.colorScheme.outline
        } else {
            MaterialTheme.colorScheme.primary
        },
        onRestore = onRestore,
        onDelete = onDelete,
        modifier = modifier,
    )
}

@Composable
internal fun ArchivedHabitCard(
    habit: ArchivedHabitCardModel,
    modifier: Modifier = Modifier,
    noteFallback: String = stringResource(R.string.detail_note_empty),
    onRestore: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    ArchivedItemCard(
        badgeText = stringResource(R.string.habit),
        title = habit.title,
        primaryText = habit.primaryText,
        secondaryText = habit.secondaryText,
        note = habit.note.takeIf { it.isNotBlank() } ?: noteFallback,
        indicatorColor = MaterialTheme.colorScheme.primary,
        onRestore = onRestore,
        onDelete = onDelete,
        modifier = modifier,
    )
}

@Composable
private fun ArchivedItemCard(
    badgeText: String,
    title: String,
    primaryText: String,
    secondaryText: String,
    note: String,
    indicatorColor: Color,
    modifier: Modifier = Modifier,
    onRestore: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val cardCorner = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
    val contentCorner = RoundedCornerShape(22.dp)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = cardCorner,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = contentCorner,
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                ArchiveBadge(text = badgeText, accentColor = indicatorColor)
                            }

                            Text(
                                text = "$primaryText · $secondaryText",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        if (note.isNotBlank()) {
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (onRestore != null || onDelete != null) {
                        Row(
                            modifier = Modifier.padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (onRestore != null) {
                                ArchiveActionButton(
                                    icon = ImageVector.vectorResource(R.drawable.ic_undo),
                                    contentDescription = stringResource(R.string.unarchive),
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    onClick = onRestore,
                                )
                            }
                            if (onDelete != null) {
                                ArchiveActionButton(
                                    icon = ImageVector.vectorResource(R.drawable.ic_delete),
                                    contentDescription = stringResource(R.string.delete),
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                    onClick = onDelete,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveBadge(
    text: String,
    accentColor: Color,
) {
    Box(
        modifier = Modifier
            .background(
                color = accentColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = accentColor,
        )
    }
}

@Composable
private fun ArchiveActionButton(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(containerColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
    }
}
