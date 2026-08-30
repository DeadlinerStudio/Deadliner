package com.aritxonly.deadliner.ui.category

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.MiuixBottomSheet
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem
import top.yukonga.miuix.kmp.basic.Checkbox as MiuixCheckbox
import top.yukonga.miuix.kmp.basic.RadioButton as MiuixRadioButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CategoryBadge(category: CategoryPresentation, modifier: Modifier = Modifier) {
    val color = CategoryPresentationRegistry.color(category.colorToken)
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = iconResource(CategoryPresentationRegistry.icon(category.iconSemanticId)),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Text(category.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun CategoryAssignmentField(
    categoryUid: String?,
    categories: List<CategoryPresentation>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val category = categories.firstOrNull { it.uid == categoryUid }
    val isMiuix = LocalAppDesignSystem.current == AppDesignSystem.MIUIX
    val containerColor = if (isMiuix) {
        MiuixTheme.colorScheme.surfaceContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (isMiuix) {
        MiuixTheme.colorScheme.onSurfaceContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius)),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (category != null) CategoryBadge(category) else Text("未分类", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
            Text("选择分类", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun CategoryPickerSheet(
    categories: List<CategoryPresentation>,
    selectedUid: String?,
    onSelected: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    MiuixBottomSheet(
        show = true,
        title = "选择分类",
        onDismissRequest = onDismiss,
    ) {
        CategoryChoiceContent(
            categories = categories,
            selectedUid = selectedUid,
            onSelected = { onSelected(it); onDismiss() },
        )
    }
}

@Composable
fun CategoryFilterSheet(
    categories: List<CategoryPresentation>,
    filter: CategoryFilter,
    onFilterChange: (CategoryFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val selected = (filter as? CategoryFilter.Selected)?.categoryUids.orEmpty()
    val includesUncategorized = (filter as? CategoryFilter.Selected)?.includesUncategorized == true
    fun update(categoryUids: Set<String> = selected, uncategorized: Boolean = includesUncategorized) {
        onFilterChange(CategoryFilterResolver.selected(categoryUids, uncategorized))
    }
    MiuixBottomSheet(
        show = true,
        title = "筛选分类",
        onDismissRequest = onDismiss,
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            CategorySheetRow(
                title = "未分类",
                leading = { Icon(iconResource(R.drawable.msr_flag_wght400), null) },
                selected = includesUncategorized,
                onClick = { update(uncategorized = !includesUncategorized) },
                control = {
                    MiuixCheckbox(
                        state = if (includesUncategorized) ToggleableState.On else ToggleableState.Off,
                        onClick = { update(uncategorized = !includesUncategorized) },
                    )
                },
            )
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(categories, key = CategoryPresentation::uid) { category ->
                    val checked = category.uid in selected
                    val toggle = { update(if (checked) selected - category.uid else selected + category.uid) }
                    CategorySheetRow(
                        title = category.name,
                        leading = { MiuixCategoryBadge(category) },
                        selected = checked,
                        onClick = toggle,
                        control = {
                            MiuixCheckbox(
                                state = if (checked) ToggleableState.On else ToggleableState.Off,
                                onClick = toggle,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChoiceContent(
    categories: List<CategoryPresentation>,
    selectedUid: String?,
    onSelected: (String?) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        CategorySheetRow(
            title = "未分类",
            selected = selectedUid == null,
            onClick = { onSelected(null) },
            control = {
                MiuixRadioButton(
                    selected = selectedUid == null,
                    onClick = { onSelected(null) },
                )
            },
        )
        LazyColumn(Modifier.heightIn(max = 420.dp)) {
            items(categories, key = CategoryPresentation::uid) { category ->
                CategorySheetRow(
                    title = category.name,
                    leading = { MiuixCategoryBadge(category) },
                    selected = selectedUid == category.uid,
                    onClick = { onSelected(category.uid) },
                    control = {
                        MiuixRadioButton(
                            selected = selectedUid == category.uid,
                            onClick = { onSelected(category.uid) },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun CategorySheetRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    control: @Composable () -> Unit,
    leading: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        MiuixText(
            text = title,
            modifier = Modifier.weight(1f),
            color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
            style = MiuixTheme.textStyles.main,
        )
        control()
    }
}

@Composable
private fun MiuixCategoryBadge(category: CategoryPresentation) {
    val color = CategoryPresentationRegistry.color(category.colorToken)
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = iconResource(CategoryPresentationRegistry.icon(category.iconSemanticId)),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = color,
        )
        MiuixText(
            text = category.name,
            style = MiuixTheme.textStyles.body2,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
