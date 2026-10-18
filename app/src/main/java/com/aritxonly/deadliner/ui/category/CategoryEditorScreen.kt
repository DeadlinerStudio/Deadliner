package com.aritxonly.deadliner.ui.category

import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.OutlinedTextField
import com.aritxonly.deadliner.ui.base.TopBarIconButton
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.base.syncTopBarMaterialScroll
import com.aritxonly.deadliner.ui.editor.DeadlineBottomActions
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.settings.CollapsingTopBarScaffold
import com.aritxonly.deadliner.ui.settings.SettingsSection
import com.aritxonly.deadliner.ui.symbolPainterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryEditorScreen(
    initialDraft: CategoryEditorDraft,
    isEditing: Boolean,
    onBack: () -> Unit,
    onSave: (CategoryEditorDraft) -> Result<Unit>,
    onDelete: (() -> Unit)? = null,
) {
    var draft by remember(initialDraft) { mutableStateOf(initialDraft) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val layoutDirection = LocalLayoutDirection.current
    val listState = rememberLazyListState()
    val pageSurfaceColor = MaterialTheme.colorScheme.surface
    CollapsingTopBarScaffold(
        title = stringResource(if (isEditing) R.string.category_edit else R.string.category_add),
        containerColor = pageSurfaceColor,
        collapsible = false,
        topBarStyle = TopAppBarStyle.CENTER,
        navigationIcon = {
            IconButton(modifier = Modifier.miuixPressFeedback(enabled = true, shape = androidx.compose.material3.IconButtonDefaults.standardShape), onClick = onBack) {
                Icon(
                    painter = symbolPainterResource(R.drawable.msr_arrow_back_wght400),
                    contentDescription = stringResource(R.string.back),
                    modifier = expressiveTypeModifier,
                )
            }
        },
        actions = {
            if (onDelete != null) {
                TopBarIconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        painter = symbolPainterResource(R.drawable.msr_delete_wght400),
                        contentDescription = stringResource(R.string.category_delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = expressiveTypeModifier,
                    )
                }
            }
        },
        bottomBar = {
            DeadlineBottomActions(
                showSaveToCalendar = false,
                onSave = {
                    if (draft.name.isBlank()) {
                        errorMessage = "blank"
                    } else {
                        onSave(draft).onFailure { errorMessage = it.message ?: "save" }
                    }
                },
                onSaveToCalendar = {},
            )
        },
    ) { padding ->
        val topOverlayPadding = padding.calculateTopPadding()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(pageSurfaceColor)
                .syncTopBarMaterialScroll(listState),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                top = topOverlayPadding + 8.dp,
                end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                // Short editor content still needs enough travel to enter the overlay top bar;
                // otherwise the glass samples only the flat page surface and looks unblurred.
                bottom = padding.calculateBottomPadding() + topOverlayPadding + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    OutlinedTextField(
                        value = draft.name,
                        onValueChange = {
                            draft = draft.copy(name = it)
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.category_name)) },
                        miuixLabel = stringResource(R.string.category_name),
                        miuixFormStyle = true,
                        singleLine = true,
                        isError = errorMessage != null,
                    )
                    if (errorMessage != null) {
                        Text(
                            text = stringResource(
                                if (errorMessage == "blank") R.string.category_name_required
                                else R.string.category_save_failed,
                            ),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            item {
                SettingsSection(topLabel = stringResource(R.string.category_color)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CategoryPresentationRegistry.selectableColorHexes.forEach { hex ->
                            val color = CategoryPresentationRegistry.color(hex)
                            val selected = hex.equals(draft.colorHex, ignoreCase = true)
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(color)
                                    .then(
                                        if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier,
                                    )
                                    .semantics { contentDescription = hex.uppercase() }
                                    .selectable(
                                        selected = selected,
                                        role = Role.RadioButton,
                                        onClick = { draft = draft.copy(colorHex = hex) },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Icon(
                                        painter = iconResource(R.drawable.msr_check_wght400),
                                        contentDescription = null,
                                        tint = if (color.luminance() > 0.55f) Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                SettingsSection(topLabel = stringResource(R.string.category_icon)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CategoryPresentationRegistry.iconSemanticIds.chunked(6).forEachIndexed { rowIndex, rowIcons ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                rowIcons.forEachIndexed { columnIndex, semanticId ->
                                    val selected = semanticId == draft.iconSemanticId
                                    val selectedColor = CategoryPresentationRegistry.color(draft.colorHex)
                                    val optionDescription = stringResource(
                                        R.string.category_icon_option,
                                        rowIndex * 6 + columnIndex + 1,
                                    )
                                    Box(
                                        modifier = Modifier.size(46.dp).clip(CircleShape)
                                            .background(
                                                if (selected) selectedColor.copy(alpha = 0.18f)
                                                else MaterialTheme.colorScheme.surfaceContainerLow,
                                            )
                                            .then(
                                                if (selected) Modifier.border(2.dp, selectedColor, CircleShape)
                                                else Modifier,
                                            )
                                            .semantics { contentDescription = optionDescription }
                                            .selectable(
                                                selected = selected,
                                                role = Role.RadioButton,
                                                onClick = { draft = draft.copy(iconSemanticId = semanticId) },
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painter = iconResource(CategoryPresentationRegistry.icon(semanticId)),
                                            contentDescription = null,
                                            tint = if (selected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    CategoryDeleteDialog(
        categoryName = draft.name.takeIf { showDeleteDialog },
        onDismissRequest = { showDeleteDialog = false },
        onConfirm = {
            showDeleteDialog = false
            onDelete?.invoke()
        },
    )
}

private fun Color.luminance(): Float = red * 0.299f + green * 0.587f + blue * 0.114f
