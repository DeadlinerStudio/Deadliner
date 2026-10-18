package com.aritxonly.deadliner.ui.main.simplified

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.model.DDLStatus
import com.deadliner.viewmodel.habit.HabitDayOverview
import com.deadliner.viewmodel.habit.HabitDayStatus
import com.aritxonly.deadliner.ui.theme.rememberTaskIndicatorColors
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

import com.aritxonly.deadliner.ui.base.Checkbox
import com.aritxonly.deadliner.ui.category.CategoryBadge
import com.aritxonly.deadliner.ui.category.CategoryPresentation

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HabitCircleProgress(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 800)
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularWavyProgressIndicator(
            progress = { animatedProgress.value },
            modifier = Modifier.size(84.dp)
        )
        AnimatedProgressText(progress = progress)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HabitLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 800)
        )
    }

    Box(
        modifier = modifier
    ) {
        LinearProgressIndicator(
            progress = { animatedProgress.value },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun WeekRow(
    weekOverview: List<HabitDayOverview>,
    selectedDate: String,
    onSelectDate: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        weekOverview.forEach { day ->
            val date = LocalDate.parse(day.date)
            val isSelected = day.date == selectedDate
            val label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val dayNum = date.dayOfMonth

            Column(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius)))
                    .clickable { onSelectDate(day.date) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayNum.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 小圆点表示完成度
                val ratio = day.completionRatio
                if (ratio > 0f) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HabitRow(
    data: HabitDayStatus,
    status: DDLStatus,
    isSelected: Boolean,
    canToggle: Boolean,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    remainingText: String? = null,
    category: CategoryPresentation? = null,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
    val progress = (data.doneCount.toFloat() / data.targetCount.coerceAtLeast(1)).coerceIn(0f, 1f)

    val indicatorColors = rememberTaskIndicatorColors(
        status = status,
    )
    val indicatorColor = indicatorColors.indicatorColor
    val checkboxCheckedColor = indicatorColor.copy(alpha = 1f)
    val baseBgColor = indicatorColors.baseBackgroundColor
    val overlayBgColor = indicatorColors.overlayBackgroundColor

    val progressText = data.progressText()
    val bottomLine = remainingText?.takeIf(String::isNotBlank)?.let { "$it · $progressText" } ?: progressText
    val rightLabel = data.habit.presentationLabel(context)
    val accessibilityState = stringResource(
        when {
            data.isCompleted -> R.string.habit_accessibility_completed
            !canToggle -> R.string.habit_accessibility_unavailable
            else -> R.string.habit_accessibility_pending
        }
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .semantics { stateDescription = accessibilityState }
            .combinedClickable(
                onClick = { if (canToggle) onToggle() },
                onLongClick = onLongPress
            ),
        shape = shape
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(baseBgColor)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(overlayBgColor)
            )
            // 进度前景条
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                    indicatorColor.copy(alpha = indicatorColor.alpha * 0.45f), // 👈 进度条渐变应用
                                    indicatorColor
                                )
                            )
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp, bottom = 12.dp, end = 12.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    enabled = canToggle,
                    checked = data.isCompleted,
                    onCheckedChange = { if (canToggle) onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = checkboxCheckedColor,
                        uncheckedColor = checkboxCheckedColor,
                        checkmarkColor = MaterialTheme.colorScheme.surface,
                        disabledCheckedColor = checkboxCheckedColor.copy(alpha = 0.6f),
                        disabledUncheckedColor = checkboxCheckedColor.copy(alpha = 0.6f),
                    )
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = data.habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        category?.let { CategoryBadge(it) }
                        if (category != null) Spacer(Modifier.size(6.dp))
                        Text(text = bottomLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }

                Text(
                    text = rightLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 👇 多选叠加层：保持使用 primary 作为统一的选中高亮色
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            shape = shape
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitRowClassic(
    data: HabitDayStatus,
    isSelected: Boolean,
    canToggle: Boolean,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    remainingText: String? = null,
    category: CategoryPresentation? = null,
) {
    val context = LocalContext.current

    // 这里引用现有 drawable 背景
    val backgroundRes = if (isSelected) {
        R.drawable.item_background_selected
    } else {
        R.drawable.item_background
    }

    val progressText = data.progressText()
    val bottomLine = remainingText?.takeIf(String::isNotBlank)?.let { "$it · $progressText" } ?: progressText
    val rightLabel = data.habit.presentationLabel(context)
    val accessibilityState = stringResource(
        when {
            data.isCompleted -> R.string.habit_accessibility_completed
            !canToggle -> R.string.habit_accessibility_unavailable
            else -> R.string.habit_accessibility_pending
        }
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Transparent)
                ),
                shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
            )
            .clip(RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius)))
            .semantics { stateDescription = accessibilityState }
            .combinedClickable(
                onClick = { if (canToggle) onToggle() },
                onLongClick = onLongPress
            )
            .background(
                color = Color.Transparent,
                shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
            )
            .drawBehind {
                val drawable = ContextCompat.getDrawable(context, backgroundRes)
                drawable?.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                drawable?.draw(drawContext.canvas.nativeCanvas)
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(start = 4.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Checkbox(
                enabled = canToggle,
                checked = data.isCompleted,
                onCheckedChange = { if (canToggle) onToggle() }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = data.habit.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    category?.let { CategoryBadge(it) }
                    if (category != null) Spacer(Modifier.size(6.dp))
                    Text(text = bottomLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }

            Text(
                text = rightLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
