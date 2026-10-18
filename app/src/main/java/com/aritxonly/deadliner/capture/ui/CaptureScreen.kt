package com.aritxonly.deadliner.capture.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.Companion.FullLine
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.aritxonly.deadliner.ui.base.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.aritxonly.deadliner.ui.base.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.AddDDLActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.capture.CaptureEffect
import com.aritxonly.deadliner.capture.CaptureViewModel
import com.aritxonly.deadliner.ui.base.AdaptiveMaterialScaffold
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.DeadlinerMiuixDefaults
import com.aritxonly.deadliner.ui.base.MiuixBottomSheet
import com.aritxonly.deadliner.ui.base.TopAppBar
import com.aritxonly.deadliner.ui.base.TopBarIconButton
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.base.syncTopBarMaterialScroll
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.main.shared.mainListContainerClip
import com.aritxonly.deadliner.ui.settings.RoundedTextField
import com.aritxonly.deadliner.ui.settings.RoundedTextFieldMetrics
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults as MiuixTextFieldDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class PendingCaptureDelete(
    val itemUid: String,
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
    mode: TopAppBarStyle = TopAppBarStyle.CENTER,
    scrollBehavior: ScrollBehavior? = null,
) {
    val ui by vm.uiState.collectAsState()
    var showDeleteSelectedConfirm by rememberSaveable { mutableStateOf(false) }
    if (!ui.isMultiSelectMode) {
        TopAppBar(
            title = stringResource(R.string.capture_title),
            forceMaterial3 = forceMaterial3,
            navigationIcon = if (showNavigationIcon) {
                {
                    TopBarIconButton(onClick = onClose) {
                        Icon(
                            painter = com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_close_wght400),
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
            mode = mode,
            isMainTitle = true,
            miuixScrollBehavior = scrollBehavior,
        )
    } else {
        TopAppBar(
            title = stringResource(R.string.capture_selected_count, ui.selectedUids.size),
            forceMaterial3 = forceMaterial3,
            navigationIcon = {
                TopBarIconButton(onClick = vm::exitMultiSelect) {
                    Icon(
                        painter = com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_close_wght400),
                        contentDescription = stringResource(R.string.close),
                        modifier = expressiveTypeModifier,
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = { showDeleteSelectedConfirm = true },
                    enabled = ui.selectedUids.isNotEmpty()
                ) {
                    Text(stringResource(R.string.capture_delete))
                }
                TextButton(
                    onClick = onRequestMerge,
                    enabled = ui.selectedUids.isNotEmpty()
                ) {
                    Text(stringResource(R.string.capture_merge_count, ui.selectedUids.size))
                }
            },
            mode = TopAppBarStyle.SMALL,
        )
    }

    if (showDeleteSelectedConfirm) {
        AlertDialog(
            show = true,
            onDismissRequest = { showDeleteSelectedConfirm = false },
            title = { Text(stringResource(R.string.capture_delete_selected_confirm_title, ui.selectedUids.size)) },
            miuixTitle = stringResource(R.string.capture_delete_selected_confirm_title, ui.selectedUids.size),
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
    val topBarScrollBehavior = MiuixScrollBehavior()

    AdaptiveMaterialScaffold(
        topBar = {
            if (!showTopBar) return@AdaptiveMaterialScaffold
            CaptureTopBar(
                vm = vm,
                onClose = onClose,
                showNavigationIcon = showNavigationIcon,
                onRequestMerge = { showMergeSheet = true },
                mode = TopAppBarStyle.LARGE,
                scrollBehavior = topBarScrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(modifier = Modifier.nestedScroll(topBarScrollBehavior.nestedScrollConnection)) {
            CaptureContent(
                vm = vm,
                contentPadding = innerPadding,
                showMergeSheet = showMergeSheet,
                onShowMergeSheetChange = { showMergeSheet = it },
            )
        }
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
    val editorEntry = com.aritxonly.deadliner.ui.editor.LocalDeadlineEditorEntry.current
    val layoutDirection = LocalLayoutDirection.current
    val ui by vm.uiState.collectAsState()
    val selectedCount = ui.selectedUids.size
    var pendingDelete by remember { mutableStateOf<PendingCaptureDelete?>(null) }
    var activeSwipeKey by remember(twoColumnLayout) { mutableStateOf<String?>(null) }
    LaunchedEffect(ui.isMultiSelectMode, ui.filteredItems.map { it.uid }) {
        if (ui.isMultiSelectMode || ui.filteredItems.none { "capture:${it.uid}" == activeSwipeKey }) activeSwipeKey = null
    }
    val listState = rememberLazyListState()
    val gridState = rememberLazyStaggeredGridState()
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
                    editorEntry.open(intent)
                }
                is CaptureEffect.OpenHabitEditor -> {
                    copyToClipboard(context, effect.sourceText)
                    val intent = Intent(context, AddDDLActivity::class.java).apply {
                        putExtra("EXTRA_CURRENT_TYPE", 1)
                        putExtra(AddDDLActivity.EXTRA_PREFILL_TEXT, effect.sourceText)
                        putExtra(AddDDLActivity.EXTRA_AUTO_RUN_AI, effect.autoRunAi)
                    }
                    editorEntry.open(intent)
                }
            }
        }
    }

    if (ui.editingItemUid != null) {
        CaptureDetailSheet(
            text = ui.editingText,
            loading = ui.isLoading,
            onTextChange = vm::updateEditingText,
            onClose = vm::closeDetail,
            onSave = vm::saveEditing,
            onDelete = {
                val editingUid = ui.editingItemUid ?: return@CaptureDetailSheet
                pendingDelete = PendingCaptureDelete(
                    itemUid = editingUid,
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
        MiuixBottomSheet(
            show = true,
            title = stringResource(
                if (selectedCount > 1) R.string.capture_merge_sheet_title else R.string.capture_merge_sheet_title_single,
            ),
            onDismissRequest = { onShowMergeSheetChange(false) },
        ) { sheetPadding ->
            val sheetScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .syncTopBarMaterialScroll(sheetScrollState)
                    .verticalScroll(sheetScrollState)
                    .padding(sheetPadding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MiuixText(
                    stringResource(
                        if (selectedCount > 1) R.string.capture_merge_sheet_desc else R.string.capture_merge_sheet_desc_single,
                    ),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                MiuixText(
                    stringResource(R.string.capture_ai_default_label),
                    style = MiuixTheme.textStyles.main,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiuixButton(
                        onClick = {
                            onShowMergeSheetChange(false)
                            vm.convertSelectedToTask(useAi = true)
                        },
                        modifier = Modifier.weight(1f),
                        cornerRadius = DeadlinerMiuixDefaults.ButtonCornerRadius,
                        colors = MiuixButtonDefaults.buttonColorsPrimary(),
                    ) { MiuixText(stringResource(R.string.capture_ai_task)) }
                    MiuixButton(
                        onClick = {
                            onShowMergeSheetChange(false)
                            vm.convertSelectedToHabit(useAi = true)
                        },
                        modifier = Modifier.weight(1f),
                        cornerRadius = DeadlinerMiuixDefaults.ButtonCornerRadius,
                    ) { MiuixText(stringResource(R.string.capture_ai_habit)) }
                }
                MiuixText(
                    stringResource(R.string.capture_direct_label),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiuixTextButton(
                        text = stringResource(R.string.capture_direct_task),
                        onClick = {
                            onShowMergeSheetChange(false)
                            vm.convertSelectedToTask(useAi = false)
                        },
                        modifier = Modifier.weight(1f),
                        cornerRadius = DeadlinerMiuixDefaults.ButtonCornerRadius,
                    )
                    MiuixTextButton(
                        text = stringResource(R.string.capture_direct_habit),
                        onClick = {
                            onShowMergeSheetChange(false)
                            vm.convertSelectedToHabit(useAi = false)
                        },
                        modifier = Modifier.weight(1f),
                        cornerRadius = DeadlinerMiuixDefaults.ButtonCornerRadius,
                    )
                }
            }
        }
    }

    pendingDelete?.let { deleteRequest ->
        val deleteText = deleteRequest.text.ifBlank {
            ui.items.firstOrNull { it.uid == deleteRequest.itemUid }?.text.orEmpty()
        }
        val previewTitle = if (deleteText.isBlank()) {
            stringResource(R.string.capture_delete_confirm_title)
        } else {
            stringResource(R.string.capture_delete_confirm_title_with_text, deleteText)
        }
        AlertDialog(
            show = true,
            onDismissRequest = { pendingDelete = null },
            title = { Text(previewTitle) },
            miuixTitle = previewTitle,
            text = { Text(stringResource(R.string.capture_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteItem(deleteRequest.itemUid)
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
            state = gridState,
            columns = StaggeredGridCells.Adaptive(CaptureWideGridMinCardWidth),
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    end = contentPadding.calculateEndPadding(layoutDirection),
                )
                .syncTopBarMaterialScroll(gridState)
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
                    key = { index -> ui.filteredItems[index].uid }
                ) { index ->
                    val item = ui.filteredItems[index]
                    InspirationLightCard(
                        item = item,
                        activeSwipeKey = activeSwipeKey,
                        onActiveSwipeChange = { activeSwipeKey = it },
                        selected = ui.selectedUids.contains(item.uid),
                        inMultiSelectMode = ui.isMultiSelectMode,
                        useHorizontalPadding = false,
                        onClick = {
                            if (ui.isMultiSelectMode) vm.toggleSelect(item.uid) else vm.openDetail(item.uid)
                        },
                        onDelete = {
                            pendingDelete = PendingCaptureDelete(
                                itemUid = item.uid,
                                text = item.text.trim()
                            )
                        },
                        onAiTask = { vm.convertItemToTask(item.uid, useAi = true) },
                        onAiHabit = { vm.convertItemToHabit(item.uid, useAi = true) }
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
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    end = contentPadding.calculateEndPadding(layoutDirection),
                )
                .syncTopBarMaterialScroll(listState)
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

                items(ui.filteredItems, key = { it.uid }) { item ->
                    InspirationLightCard(
                        item = item,
                        activeSwipeKey = activeSwipeKey,
                        onActiveSwipeChange = { activeSwipeKey = it },
                        selected = ui.selectedUids.contains(item.uid),
                        inMultiSelectMode = ui.isMultiSelectMode,
                        onClick = {
                            if (ui.isMultiSelectMode) vm.toggleSelect(item.uid) else vm.openDetail(item.uid)
                        },
                        onDelete = {
                            pendingDelete = PendingCaptureDelete(
                                itemUid = item.uid,
                                text = item.text.trim()
                            )
                        },
                        onAiTask = { vm.convertItemToTask(item.uid, useAi = true) },
                        onAiHabit = { vm.convertItemToHabit(item.uid, useAi = true) }
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
                painter = com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_lightbulb_2_wght400),
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

@OptIn(ExperimentalMaterial3Api::class)
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
    MiuixBottomSheet(
        show = true,
        title = stringResource(R.string.capture_detail_title),
        onDismissRequest = onClose,
        startAction = {
            TopBarIconButton(onClick = onClose) {
                Icon(
                    painter = com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_close_wght400),
                    contentDescription = stringResource(R.string.close),
                )
            }
        },
        endAction = {
            TopBarIconButton(onClick = onSave) {
                Icon(
                    painter = com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_check_wght400),
                    contentDescription = stringResource(R.string.save),
                )
            }
        },
    ) { sheetPadding ->
        val sheetScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .syncTopBarMaterialScroll(sheetScrollState)
                .verticalScroll(sheetScrollState)
                .padding(sheetPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MiuixTextField(
                value = text,
                onValueChange = onTextChange,
                label = stringResource(R.string.capture_detail_placeholder),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 152.dp),
                colors = MiuixTextFieldDefaults.textFieldColors(
                    backgroundColor = com.aritxonly.deadliner.ui.base.formContainerColor(MiuixTheme.colorScheme.secondaryContainer),
                    labelColor = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                ),
                useLabelAsPlaceholder = true,
                cornerRadius = DeadlinerMiuixDefaults.InputCornerRadius,
                singleLine = false,
            )

            if (loading) {
                MiuixText(
                    stringResource(R.string.capture_ai_loading),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.primary,
                )
            }

            MiuixText(stringResource(R.string.capture_ai_default_label), style = MiuixTheme.textStyles.main)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiuixButton(
                    onClick = onAiTask,
                    modifier = Modifier.weight(1f),
                    cornerRadius = DeadlinerMiuixDefaults.ButtonCornerRadius,
                    colors = MiuixButtonDefaults.buttonColorsPrimary(),
                ) { MiuixText(stringResource(R.string.capture_ai_task)) }
                MiuixButton(
                    onClick = onAiHabit,
                    modifier = Modifier.weight(1f),
                    cornerRadius = DeadlinerMiuixDefaults.ButtonCornerRadius,
                ) {
                    MiuixText(stringResource(R.string.capture_ai_habit))
                }
            }
            MiuixText(
                stringResource(R.string.capture_direct_label),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiuixTextButton(
                    text = stringResource(R.string.capture_direct_task),
                    onClick = onDirectTask,
                    modifier = Modifier.weight(1f),
                    cornerRadius = DeadlinerMiuixDefaults.ButtonCornerRadius,
                )
                MiuixTextButton(
                    text = stringResource(R.string.capture_direct_habit),
                    onClick = onDirectHabit,
                    modifier = Modifier.weight(1f),
                    cornerRadius = DeadlinerMiuixDefaults.ButtonCornerRadius,
                )
            }
            MiuixTextButton(
                text = stringResource(R.string.capture_delete_item),
                onClick = onDelete,
                modifier = Modifier.align(Alignment.End),
                cornerRadius = DeadlinerMiuixDefaults.ButtonCornerRadius,
                colors = MiuixButtonDefaults.textButtonColors(
                    color = Color.Transparent,
                    disabledColor = Color.Transparent,
                    textColor = MiuixTheme.colorScheme.error,
                ),
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.capture_clipboard_label), text))
    Toast.makeText(context, context.getString(R.string.capture_clipboard_copied), Toast.LENGTH_SHORT).show()
}
