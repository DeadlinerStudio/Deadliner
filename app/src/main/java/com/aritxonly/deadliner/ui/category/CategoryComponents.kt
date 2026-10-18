package com.aritxonly.deadliner.ui.category

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.formContainerBackground
import com.aritxonly.deadliner.ui.base.MiuixBottomSheet
import com.aritxonly.deadliner.ui.iconResource
import top.yukonga.miuix.kmp.basic.Checkbox as MiuixCheckbox
import top.yukonga.miuix.kmp.basic.RadioButton as MiuixRadioButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CategoryBadge(
    category: CategoryPresentation,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val color = CategoryPresentationRegistry.color(category.colorToken)
    val isMiuix = true
    val horizontalPadding = if (compact) 6.dp else 9.dp
    val verticalPadding = if (compact) 0.dp else 5.dp
    val iconSize = if (compact) 12.dp else 16.dp
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), CategoryBadgeShape)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp),
    ) {
        Icon(
            painter = iconResource(CategoryPresentationRegistry.icon(category.iconSemanticId)),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = color,
        )
        if (compact) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else if (isMiuix) {
            MiuixText(
                text = category.name,
                style = MiuixTheme.textStyles.body2,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
    val isMiuix = true
    val contentColor = if (isMiuix) {
        MiuixTheme.colorScheme.onSurfaceContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = modifier.fillMaxWidth().formContainerBackground(dimensionResource(R.dimen.item_corner_radius))
            .clickable(onClick = onClick),
        color = androidx.compose.ui.graphics.Color.Transparent,
        contentColor = contentColor,
        shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius)),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (category != null) CategoryBadge(category) else Text(stringResource(R.string.category_uncategorized), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.category_select), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun CategoryPickerSheet(
    categories: List<CategoryPresentation>,
    selectedUid: String?,
    onSelected: (String?) -> Unit,
    onManageCategories: () -> Unit,
    onDismiss: () -> Unit,
) {
    var sheetVisible by remember { mutableStateOf(true) }
    var manageAfterDismiss by remember { mutableStateOf(false) }
    MiuixBottomSheet(
        show = sheetVisible,
        title = stringResource(R.string.category_select),
        onDismissRequest = { sheetVisible = false },
        onDismissFinished = {
            onDismiss()
            if (manageAfterDismiss) onManageCategories()
        },
    ) { sheetPadding ->
        CategoryChoiceContent(
            contentPadding = sheetPadding,
            categories = categories,
            selectedUid = selectedUid,
            onSelected = {
                onSelected(it)
                sheetVisible = false
            },
            onManageCategories = {
                manageAfterDismiss = true
                sheetVisible = false
            },
        )
    }
}

@Composable
fun CategoryFilterSheet(
    categories: List<CategoryPresentation>,
    filter: CategoryFilter,
    onFilterChange: (CategoryFilter) -> Unit,
    onManageCategories: () -> Unit,
    onDismiss: () -> Unit,
) {
    var sheetVisible by remember { mutableStateOf(true) }
    var manageAfterDismiss by remember { mutableStateOf(false) }
    val selected = (filter as? CategoryFilter.Selected)?.categoryUids.orEmpty()
    val includesUncategorized = (filter as? CategoryFilter.Selected)?.includesUncategorized == true
    fun update(categoryUids: Set<String> = selected, uncategorized: Boolean = includesUncategorized) {
        onFilterChange(CategoryFilterResolver.selected(categoryUids, uncategorized))
    }
    MiuixBottomSheet(
        show = sheetVisible,
        title = stringResource(R.string.category_filter),
        onDismissRequest = { sheetVisible = false },
        onDismissFinished = {
            onDismiss()
            if (manageAfterDismiss) onManageCategories()
        },
    ) { sheetPadding ->
        Column(Modifier.fillMaxWidth().padding(sheetPadding).padding(bottom = 20.dp)) {
            CategorySheetRow(
                title = stringResource(R.string.category_uncategorized),
                leading = {
                    Icon(
                        painter = iconResource(R.drawable.msr_flag_wght400),
                        contentDescription = null,
                        tint = if (includesUncategorized) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurface
                        },
                    )
                },
                selected = includesUncategorized,
                onClick = { update(uncategorized = !includesUncategorized) },
                control = {
                    MiuixCheckbox(
                        state = if (includesUncategorized) ToggleableState.On else ToggleableState.Off,
                        onClick = { update(uncategorized = !includesUncategorized) },
                    )
                },
            )
            LazyColumn(Modifier.weight(1f)) {
                items(categories, key = CategoryPresentation::uid) { category ->
                    val checked = category.uid in selected
                    val toggle = { update(if (checked) selected - category.uid else selected + category.uid) }
                    CategorySheetRow(
                        title = null,
                        leading = { CategoryBadge(category) },
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
            ManageCategoriesRow(
                onClick = {
                    manageAfterDismiss = true
                    sheetVisible = false
                },
            )
        }
    }
}

@Composable
private fun CategoryChoiceContent(
    contentPadding: PaddingValues,
    categories: List<CategoryPresentation>,
    selectedUid: String?,
    onSelected: (String?) -> Unit,
    onManageCategories: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(contentPadding).padding(bottom = 20.dp)) {
        CategorySheetRow(
            title = stringResource(R.string.category_uncategorized),
            selected = selectedUid == null,
            onClick = { onSelected(null) },
            control = {
                MiuixRadioButton(
                    selected = selectedUid == null,
                    onClick = { onSelected(null) },
                )
            },
        )
        LazyColumn(Modifier.weight(1f)) {
            items(categories, key = CategoryPresentation::uid) { category ->
                CategorySheetRow(
                    title = null,
                    leading = { CategoryBadge(category) },
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
        ManageCategoriesRow(onClick = onManageCategories)
    }
}

@Composable
private fun ManageCategoriesRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = iconResource(R.drawable.msr_edit_wght400),
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
        )
        MiuixText(
            text = stringResource(R.string.category_manage),
            color = MiuixTheme.colorScheme.primary,
            style = MiuixTheme.textStyles.main,
        )
    }
}

@Composable
private fun CategorySheetRow(
    title: String?,
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
        if (title != null) {
            MiuixText(
                text = title,
                modifier = Modifier.weight(1f),
                color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.main,
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        control()
    }
}

private val CategoryBadgeShape = RoundedCornerShape(12.dp)
