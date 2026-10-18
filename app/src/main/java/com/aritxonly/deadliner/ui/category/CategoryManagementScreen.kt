package com.aritxonly.deadliner.ui.category

import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.settings.CollapsingTopBarScaffold
import com.aritxonly.deadliner.ui.settings.SettingsScrollColumn
import com.aritxonly.deadliner.ui.settings.SettingsSection
import com.aritxonly.deadliner.ui.settings.SettingsSectionDivider
import com.aritxonly.deadliner.ui.symbolPainterResource

@Composable
internal fun CategoryManagementScreen(
    categories: List<CategoryPresentation>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var deleteTarget by remember { mutableStateOf<CategoryPresentation?>(null) }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.category_management_title),
        topBarStyle = TopAppBarStyle.LARGE,
        navigationIcon = {
            IconButton(modifier = Modifier.miuixPressFeedback(enabled = true, shape = androidx.compose.material3.IconButtonDefaults.standardShape), onClick = onBack) {
                Icon(
                    painter = symbolPainterResource(R.drawable.msr_arrow_back_wght400),
                    contentDescription = stringResource(R.string.back),
                    modifier = expressiveTypeModifier,
                )
            }
        },
    ) { padding ->
        SettingsScrollColumn(contentPadding = padding) {
            SettingsSection(topLabel = stringResource(R.string.category_management_section)) {
                categories.forEachIndexed { index, category ->
                    CategoryManagementRow(
                        category = category,
                        onClick = { onEdit(category.uid) },
                        onDelete = { deleteTarget = category },
                    )
                    if (index != categories.lastIndex) SettingsSectionDivider()
                }
            }
            SettingsSection {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onAdd)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        painter = symbolPainterResource(R.drawable.msr_add_wght400),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.category_add),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    CategoryDeleteDialog(
        categoryName = deleteTarget?.name,
        onDismissRequest = { deleteTarget = null },
        onConfirm = {
            deleteTarget?.uid?.let(onDelete)
            deleteTarget = null
        },
    )
}

@Composable
private fun CategoryManagementRow(
    category: CategoryPresentation,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryBadge(category)
        Spacer(Modifier.weight(1f))
        IconButton(modifier = Modifier.miuixPressFeedback(enabled = true, shape = androidx.compose.material3.IconButtonDefaults.standardShape), onClick = onClick) {
            Icon(
                painter = symbolPainterResource(R.drawable.msr_edit_wght400),
                contentDescription = stringResource(R.string.category_edit),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(modifier = Modifier.miuixPressFeedback(enabled = true, shape = androidx.compose.material3.IconButtonDefaults.standardShape), onClick = onDelete) {
            Icon(
                painter = symbolPainterResource(R.drawable.msr_delete_wght400),
                contentDescription = stringResource(R.string.category_delete),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
