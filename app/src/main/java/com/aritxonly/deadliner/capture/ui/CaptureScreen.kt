package com.aritxonly.deadliner.capture.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.Companion.FullLine
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.AddDDLActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.capture.CaptureEffect
import com.aritxonly.deadliner.capture.CaptureViewModel
import com.aritxonly.deadliner.capture.model.InspirationItem
import com.aritxonly.deadliner.ui.base.AdaptiveMaterialScaffold
import com.aritxonly.deadliner.ui.base.RegisterAdvancedMaterialDialogBlur
import com.aritxonly.deadliner.ui.base.TopAppBar
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.main.shared.mainListContainerClip
import com.aritxonly.deadliner.ui.settings.RoundedTextField
import com.aritxonly.deadliner.ui.settings.RoundedTextFieldMetrics
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

private data class PendingCaptureDelete(
    val itemId: Long,
    val text: String,
)

private val CaptureWideGridMinCardWidth = 280.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureTopBar(
    vm: CaptureViewModel,
    onClose: () -> Unit,
    showNavigationIcon: Boolean = true,
    onRequestMerge: () -> Unit = {},
    forceMaterial3: Boolean = false,
    useParentMaterialContainer: Boolean = false,
) {
    val ui by vm.uiState.collectAsState()
    var showDeleteSelectedConfirm by rememberSaveable { mutableStateOf(false) }
    if (!ui.isMultiSelectMode) {
        TopAppBar(
            title = stringResource(R.string.capture_title),
            forceMaterial3 = forceMaterial3,
            navigationIcon = if (showNavigationIcon) {
                {
                    IconButton(onClick = onClose) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = stringResource(R.string.close),
                            modifier = expressiveTypeModifier,
                        )
                    }
                }
            } else null,
            actions = {
                TextButton(
                    onClick = vm::toggleMultiSelect,
                    enabled = ui.filteredItems.isNotEmpty()
                ) {
                    Text(stringResource(R.string.capture_multi_select))
                }
            },
            mode = if (showNavigationIcon) TopAppBarStyle.CENTER else TopAppBarStyle.SMALL,
            useParentMaterialContainer = useParentMaterialContainer,
            isMainTitle = true
        )
    } else {
        TopAppBar(
            title = stringResource(R.string.capture_selected_count, ui.selectedIds.size),
            forceMaterial3 = forceMaterial3,
            navigationIcon = {
                IconButton(onClick = vm::exitMultiSelect) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(R.string.close),
                        modifier = expressiveTypeModifier,
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = { showDeleteSelectedConfirm = true },
                    enabled = ui.selectedIds.isNotEmpty()
                ) {
                    Text(stringResource(R.string.capture_delete))
                }
                TextButton(
                    onClick = onRequestMerge,
                    enabled = ui.selectedIds.isNotEmpty()
                ) {
                    Text(stringResource(R.string.capture_merge_count, ui.selectedIds.size))
                }
            },
            useParentMaterialContainer = useParentMaterialContainer,
        )
    }

    if (showDeleteSelectedConfirm) {
        RegisterAdvancedMaterialDialogBlur()
        AlertDialog(
            onDismissRequest = { showDeleteSelectedConfirm = false },
            title = { Text(stringResource(R.string.capture_delete_selected_confirm_title, ui.selectedIds.size)) },
            text = { Text(stringResource(R.string.capture_delete_selected_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteSelectedConfirm = false
                        vm.deleteSelected()
                    }
                ) {
                    Text(stringResource(R.string.accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    vm: CaptureViewModel,
    onClose: () -> Unit,
    showTopBar: Boolean = true,
    showNavigationIcon: Boolean = true,
) {
    var showMergeSheet by rememberSaveable { mutableStateOf(false) }

    AdaptiveMaterialScaffold(
        wrapTopBarInMaterialContainer = false,
        topBar = {
            if (!showTopBar) return@AdaptiveMaterialScaffold
            CaptureTopBar(
                vm = vm,
                onClose = onClose,
                showNavigationIcon = showNavigationIcon,
                onRequestMerge = { showMergeSheet = true },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        CaptureContent(
            vm = vm,
            contentPadding = innerPadding,
            showMergeSheet = showMergeSheet,
            onShowMergeSheetChange = { showMergeSheet = it },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CaptureContent(
    vm: CaptureViewModel,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    topOverlayPadding: Dp = 0.dp,
    bottomLiftPadding: Dp = 28.dp,
    twoColumnLayout: Boolean = false,
    showMergeSheet: Boolean = false,
    onShowMergeSheetChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val ui by vm.uiState.collectAsState()
    val selectedCount = ui.selectedIds.size
    var showDirectConvertMenu by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PendingCaptureDelete?>(null) }
    val listContentPadding = remember(contentPadding, topOverlayPadding, bottomLiftPadding, layoutDirection) {
        PaddingValues(
            top = contentPadding.calculateTopPadding() + topOverlayPadding,
            bottom = contentPadding.calculateBottomPadding() + bottomLiftPadding,
        )
    }

    LaunchedEffect(Unit) {
        vm.updateQuery("")
        vm.effects.collect { effect ->
            when (effect) {
                is CaptureEffect.ToastRes -> {
                    Toast.makeText(context, context.getString(effect.resId), Toast.LENGTH_SHORT).show()
                }
                is CaptureEffect.OpenTaskEditor -> {
                    copyToClipboard(context, effect.sourceText)
                    val intent = Intent(context, AddDDLActivity::class.java).apply {
                        putExtra("EXTRA_CURRENT_TYPE", 0)
                        putExtra(AddDDLActivity.EXTRA_PREFILL_TEXT, effect.sourceText)
                        putExtra(AddDDLActivity.EXTRA_AUTO_RUN_AI, effect.autoRunAi)
                        effect.generated?.let { putExtra("EXTRA_GENERATE_DDL", it) }
                    }
                    context.startActivity(intent)
                }
                is CaptureEffect.OpenHabitEditor -> {
                    copyToClipboard(context, effect.sourceText)
                    val intent = Intent(context, AddDDLActivity::class.java).apply {
                        putExtra("EXTRA_CURRENT_TYPE", 1)
                        putExtra(AddDDLActivity.EXTRA_PREFILL_TEXT, effect.sourceText)
                        putExtra(AddDDLActivity.EXTRA_AUTO_RUN_AI, effect.autoRunAi)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    if (ui.editingItemId != null) {
        CaptureDetailSheet(
            text = ui.editingText,
            loading = ui.loading,
            onTextChange = vm::updateEditingText,
            onClose = vm::closeDetail,
            onSave = vm::saveEditing,
            onDelete = {
                val editingId = ui.editingItemId ?: return@CaptureDetailSheet
                pendingDelete = PendingCaptureDelete(
                    itemId = editingId,
                    text = ui.editingText.trim()
                )
                vm.closeDetail()
            },
            onAiTask = { vm.convertCurrentEditingToTask(useAi = true) },
            onAiHabit = { vm.convertCurrentEditingToHabit(useAi = true) },
            onDirectTask = { vm.convertCurrentEditingToTask(useAi = false) },
            onDirectHabit = { vm.convertCurrentEditingToHabit(useAi = false) }
        )
    }

    if (showMergeSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        RegisterAdvancedMaterialDialogBlur()
        ModalBottomSheet(
            onDismissRequest = { onShowMergeSheetChange(false) },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(
                        if (selectedCount > 1) {
                            R.string.capture_merge_sheet_title
                        } else {
                            R.string.capture_merge_sheet_title_single
                        }
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(
                        if (selectedCount > 1) {
                            R.string.capture_merge_sheet_desc
                        } else {
                            R.string.capture_merge_sheet_desc_single
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilledTonalButton(
                    onClick = {
                        onShowMergeSheetChange(false)
                        vm.convertSelectedToTask(useAi = true)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.capture_ai_task)) }
                FilledTonalButton(
                    onClick = {
                        onShowMergeSheetChange(false)
                        vm.convertSelectedToHabit(useAi = true)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.capture_ai_habit)) }

                Box {
                    OutlinedButton(
                        onClick = { showDirectConvertMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.capture_direct_secondary))
                    }
                    DropdownMenu(
                        expanded = showDirectConvertMenu,
                        onDismissRequest = { showDirectConvertMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.capture_direct_task)) },
                            onClick = {
                                showDirectConvertMenu = false
                                onShowMergeSheetChange(false)
                                vm.convertSelectedToTask(useAi = false)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.capture_direct_habit)) },
                            onClick = {
                                showDirectConvertMenu = false
                                onShowMergeSheetChange(false)
                                vm.convertSelectedToHabit(useAi = false)
                            }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { deleteRequest ->
        val deleteText = deleteRequest.text.ifBlank {
            ui.items.firstOrNull { it.id == deleteRequest.itemId }?.text.orEmpty()
        }
        val previewTitle = if (deleteText.isBlank()) {
            stringResource(R.string.capture_delete_confirm_title)
        } else {
            stringResource(R.string.capture_delete_confirm_title_with_text, deleteText)
        }
        RegisterAdvancedMaterialDialogBlur()
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(previewTitle) },
            text = { Text(stringResource(R.string.capture_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteItem(deleteRequest.itemId)
                        pendingDelete = null
                    }
                ) {
                    Text(stringResource(R.string.accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (twoColumnLayout) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(CaptureWideGridMinCardWidth),
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    end = contentPadding.calculateEndPadding(layoutDirection),
                )
                .mainListContainerClip(),
            contentPadding = listContentPadding,
            verticalItemSpacing = 12.dp,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = FullLine) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CaptureInputCard(
                        draftText = ui.draftText,
                        onDraftChange = vm::updateDraft,
                        onSave = vm::saveDraft
                    )
                }
            }

            if (ui.filteredItems.isNotEmpty()) {
                item(span = FullLine) {
                    CaptureSectionHeader(
                        title = stringResource(R.string.capture_recent_title),
                        subtitle = stringResource(
                            if (ui.isMultiSelectMode) {
                                R.string.capture_recent_subtitle_selection
                            } else {
                                R.string.capture_recent_subtitle
                            }
                        ),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                    )
                }

                items(
                    count = ui.filteredItems.size,
                    key = { index -> ui.filteredItems[index].id }
                ) { index ->
                    val item = ui.filteredItems[index]
                    InspirationLightCard(
                        item = item,
                        selected = ui.selectedIds.contains(item.id),
                        inMultiSelectMode = ui.isMultiSelectMode,
                        useHorizontalPadding = false,
                        onClick = {
                            if (ui.isMultiSelectMode) vm.toggleSelect(item.id) else vm.openDetail(item.id)
                        },
                        onDelete = {
                            pendingDelete = PendingCaptureDelete(
                                itemId = item.id,
                                text = item.text.trim()
                            )
                        },
                        onAiTask = { vm.convertItemToTask(item.id, useAi = true) },
                        onAiHabit = { vm.convertItemToHabit(item.id, useAi = true) }
                    )
                }
            } else {
                item(span = FullLine) {
                    EmptyCaptureHint(
                        text = stringResource(R.string.capture_empty_hint_default)
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    end = contentPadding.calculateEndPadding(layoutDirection),
                )
                .mainListContainerClip(),
            contentPadding = listContentPadding,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CaptureInputCard(
                            draftText = ui.draftText,
                            onDraftChange = vm::updateDraft,
                            onSave = vm::saveDraft
                        )
                    }
                }

                if (ui.filteredItems.isNotEmpty()) {
                    item {
                        CaptureSectionHeader(
                            title = stringResource(R.string.capture_recent_title),
                            subtitle = stringResource(
                                if (ui.isMultiSelectMode) {
                                    R.string.capture_recent_subtitle_selection
                                } else {
                                    R.string.capture_recent_subtitle
                                }
                            ),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                        )
                    }
                }

                items(ui.filteredItems, key = { it.id }) { item ->
                    InspirationLightCard(
                        item = item,
                        selected = ui.selectedIds.contains(item.id),
                        inMultiSelectMode = ui.isMultiSelectMode,
                        onClick = {
                            if (ui.isMultiSelectMode) vm.toggleSelect(item.id) else vm.openDetail(item.id)
                        },
                        onDelete = {
                            pendingDelete = PendingCaptureDelete(
                                itemId = item.id,
                                text = item.text.trim()
                            )
                        },
                        onAiTask = { vm.convertItemToTask(item.id, useAi = true) },
                        onAiHabit = { vm.convertItemToHabit(item.id, useAi = true) }
                    )
                }

                if (ui.filteredItems.isEmpty()) {
                    item {
                        EmptyCaptureHint(
                            text = stringResource(R.string.capture_empty_hint_default)
                        )
                    }
                }
        }
    }
}

@Composable
private fun CaptureInputCard(
    draftText: String,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val sectionShape = RoundedCornerShape(28.dp)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = sectionShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = stringResource(R.string.capture_input_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.capture_input_subtitle),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(R.string.capture_input_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            RoundedTextField(
                value = draftText,
                onValueChange = onDraftChange,
                hint = stringResource(R.string.capture_input_placeholder),
                metrics = RoundedTextFieldMetrics(
                    singleLine = false,
                    minHeight = 112.dp,
                    cornerSize = 18.dp
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSave,
                    enabled = draftText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.capture_save)) }
            }
        }
    }
}

@Composable
private fun InspirationLightCard(
    item: InspirationItem,
    selected: Boolean,
    inMultiSelectMode: Boolean,
    modifier: Modifier = Modifier,
    useHorizontalPadding: Boolean = true,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onAiTask: () -> Unit,
    onAiHabit: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val sectionShape = RoundedCornerShape(24.dp)
    val cardColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (useHorizontalPadding) Modifier.padding(horizontal = 16.dp) else Modifier)
            .clickable { onClick() },
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
                painter = painterResource(
                    id = if (inMultiSelectMode) {
                        if (selected) R.drawable.ic_check_circle_fill else R.drawable.ic_check_circle
                    } else {
                        R.drawable.ic_quote
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (!inMultiSelectMode) {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more),
                                    contentDescription = stringResource(R.string.settings_more)
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.capture_delete)) },
                                    onClick = {
                                        menuExpanded = false
                                        onDelete()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.capture_ai_task)) },
                                    onClick = {
                                        menuExpanded = false
                                        onAiTask()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.capture_ai_habit)) },
                                    onClick = {
                                        menuExpanded = false
                                        onAiHabit()
                                    }
                                )
                            }
                        }
                    }
                }

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

@Composable
private fun CaptureSectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyCaptureHint(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_lightbulb),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = stringResource(R.string.capture_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CaptureDetailSheet(
    text: String,
    loading: Boolean,
    onTextChange: (String) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onAiTask: () -> Unit,
    onAiHabit: () -> Unit,
    onDirectTask: () -> Unit,
    onDirectHabit: () -> Unit
) {
    var secondaryMenuExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    RegisterAdvancedMaterialDialogBlur()
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(R.string.close),
                        modifier = expressiveTypeModifier,
                    )
                }
                Text(
                    stringResource(R.string.capture_detail_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onSave) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = stringResource(R.string.save)
                    )
                }
            }

            RoundedTextField(
                value = text,
                onValueChange = onTextChange,
                hint = stringResource(R.string.capture_detail_placeholder),
                metrics = RoundedTextFieldMetrics(
                    singleLine = false,
                    minHeight = 180.dp,
                    cornerSize = 12.dp
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (loading) {
                Text(
                    stringResource(R.string.capture_ai_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                stringResource(R.string.capture_ai_default_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(onClick = onAiTask) { Text(stringResource(R.string.capture_ai_task)) }
                FilledTonalButton(onClick = onAiHabit) { Text(stringResource(R.string.capture_ai_habit)) }
                Box {
                    OutlinedButton(onClick = { secondaryMenuExpanded = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more),
                            contentDescription = null
                        )
                        Text(" ${stringResource(R.string.capture_direct_secondary_short)}")
                    }
                    DropdownMenu(
                        expanded = secondaryMenuExpanded,
                        onDismissRequest = { secondaryMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.capture_direct_task)) },
                            onClick = {
                                secondaryMenuExpanded = false
                                onDirectTask()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.capture_direct_habit)) },
                            onClick = {
                                secondaryMenuExpanded = false
                                onDirectHabit()
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            TextButton(onClick = onDelete) { Text(stringResource(R.string.capture_delete_item)) }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.capture_clipboard_label), text))
    Toast.makeText(context, context.getString(R.string.capture_clipboard_copied), Toast.LENGTH_SHORT).show()
}

private fun formatRelativeTime(context: Context, timestampMillis: Long): String {
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
