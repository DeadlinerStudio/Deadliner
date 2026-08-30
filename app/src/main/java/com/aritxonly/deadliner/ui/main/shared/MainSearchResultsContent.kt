package com.aritxonly.deadliner.ui.main.shared

import android.content.Intent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.AddDDLActivity
import com.aritxonly.deadliner.DeadlineDetailActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.capture.data.CaptureRepository
import com.aritxonly.deadliner.capture.model.InspirationItem
import com.aritxonly.deadliner.data.HabitViewModel
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DDLState
import com.aritxonly.deadliner.model.DDLStatus
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.Habit
import com.aritxonly.deadliner.model.formatHint
import com.aritxonly.deadliner.ui.archive.ArchivedHabitCard
import com.aritxonly.deadliner.ui.archive.ArchivedTaskCard
import com.aritxonly.deadliner.ui.base.AlertDialog as AppAlertDialog
import com.aritxonly.deadliner.ui.base.TextButton as AppTextButton
import com.aritxonly.deadliner.ui.main.DDLItemCardSimplified
import com.aritxonly.deadliner.ui.main.shared.mainListContainerClip
import com.aritxonly.deadliner.ui.main.simplified.AnimatedItem
import com.aritxonly.deadliner.ui.main.simplified.HabitRow
import com.aritxonly.deadliner.ui.settings.RoundedTextField
import com.aritxonly.deadliner.ui.settings.RoundedTextFieldMetrics
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

@Composable
fun MainSearchResultsContent(
    query: String,
    scope: MainSearchScope,
    onScopeChange: (MainSearchScope) -> Unit,
    items: List<DDLItem>,
    habitViewModel: HabitViewModel,
    activity: MainActivity,
    inspirations: List<InspirationItem> = emptyList(),
    archivedHabitByDdlId: Map<Long, Habit> = emptyMap(),
    onCaptureChanged: () -> Unit = {},
    onRestoreArchivedTask: (DDLItem) -> Unit = {},
    onDeleteArchivedTask: (DDLItem) -> Unit = {},
    onRestoreArchivedHabit: (Habit) -> Unit = {},
    onDeleteArchivedHabit: (Habit) -> Unit = {},
) {
    val context = LocalContext.current
    val captureRepo = remember(context) { CaptureRepository(context.applicationContext) }
    val selectedDate by habitViewModel.selectedDate.collectAsState()
    val habitsForSelectedDate by habitViewModel.habitsForSelectedDate.collectAsState()
    val statusByDdlId = habitsForSelectedDate.associateBy { it.habit.ddlId }
    val canToggleOnThisDate = !selectedDate.isAfter(LocalDate.now())
    val sections = remember(query, scope, items) {
        buildMainSearchSections(
            query = query,
            scope = scope,
            items = items,
        )
    }
    val inspirationResults = remember(query, scope, inspirations) {
        buildInspirationSearchResults(
            query = query,
            scope = scope,
            inspirations = inspirations,
        )
    }
    var selectedInspiration by remember { mutableStateOf<InspirationItem?>(null) }
    var inspirationDraftText by rememberSaveable { mutableStateOf("") }
    var pendingDeleteInspiration by remember { mutableStateOf<InspirationItem?>(null) }
    var pendingDeleteArchivedTask by remember { mutableStateOf<DDLItem?>(null) }
    var pendingDeleteArchivedHabit by remember { mutableStateOf<Habit?>(null) }

    fun openInspirationEditor(text: String, typeIndex: Int, autoRunAi: Boolean) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val intent = Intent(activity, AddDDLActivity::class.java).apply {
            putExtra("EXTRA_CURRENT_TYPE", typeIndex)
            putExtra(AddDDLActivity.EXTRA_PREFILL_TEXT, trimmed)
            putExtra(AddDDLActivity.EXTRA_AUTO_RUN_AI, autoRunAi)
        }
        activity.startActivity(intent)
    }

    fun updateInspirationText(item: InspirationItem, newText: String) {
        val trimmed = newText.trim()
        if (trimmed.isEmpty()) return
        val updatedItems = captureRepo.load()
            .map { current ->
                if (current.id == item.id) {
                    current.copy(
                        text = trimmed,
                        updatedAt = System.currentTimeMillis(),
                    )
                } else {
                    current
                }
            }
            .sortedByDescending { it.updatedAt }
        captureRepo.save(updatedItems)
        onCaptureChanged()
    }

    fun deleteInspiration(item: InspirationItem) {
        captureRepo.save(captureRepo.load().filterNot { it.id == item.id })
        onCaptureChanged()
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .mainListContainerClip(),
    ) {
        item {
            SearchScopePicker(
                selectedScope = scope,
                onScopeChange = onScopeChange,
            )
        }

        when {
            query.isBlank() -> {
                item {
                    SearchMessageCard(
                        title = stringResource(R.string.search_empty_query_title),
                        body = stringResource(R.string.search_empty_query_hint),
                    )
                }
            }

            sections.isEmpty() && inspirationResults.isEmpty() -> {
                item {
                    SearchMessageCard(
                        title = stringResource(R.string.search_no_result_title),
                        body = stringResource(R.string.search_no_result_hint),
                    )
                }
            }

            else -> {
                if (inspirationResults.isNotEmpty()) {
                    item(key = "header-inspiration") {
                        SearchSectionHeader(
                            title = stringResource(R.string.search_group_inspiration),
                            iconRes = R.drawable.ic_quote,
                        )
                    }

                    items(
                        items = inspirationResults,
                        key = { "inspiration-${it.id}" },
                    ) { item ->
                        SearchInspirationCard(
                            item = item,
                            onOpen = {
                                selectedInspiration = item
                                inspirationDraftText = item.text
                            },
                            onDelete = {
                                pendingDeleteInspiration = item
                            },
                            onAiTask = { openInspirationEditor(item.text, 0, true) },
                            onAiHabit = { openInspirationEditor(item.text, 1, true) },
                            onDirectTask = { openInspirationEditor(item.text, 0, false) },
                            onDirectHabit = { openInspirationEditor(item.text, 1, false) },
                        )
                    }
                }

                sections.forEach { section ->
                    item(key = "header-${section.titleRes}") {
                        SearchSectionHeader(
                            title = stringResource(section.titleRes),
                            iconRes = section.iconRes,
                        )
                    }

                    items(
                        items = section.items,
                        key = { "${section.titleRes}-${it.type}-${it.id}" },
                    ) { item ->
                        AnimatedItem(
                            item = item,
                            index = 0,
                        ) {
                            when {
                                item.state.isArchiveListVisible() && item.type == DeadlineType.TASK -> {
                                    ArchivedTaskCard(
                                        item = item,
                                        onRestore = { onRestoreArchivedTask(item) },
                                        onDelete = { pendingDeleteArchivedTask = item },
                                    )
                                }

                                item.state.isArchiveListVisible() && item.type == DeadlineType.HABIT -> {
                                    archivedHabitByDdlId[item.id]?.let { archivedHabit ->
                                        ArchivedHabitCard(
                                            habit = archivedHabit,
                                            onRestore = { onRestoreArchivedHabit(archivedHabit) },
                                            onDelete = { pendingDeleteArchivedHabit = archivedHabit },
                                        )
                                    }
                                }

                                item.type == DeadlineType.HABIT -> {
                                    HabitSearchItem(
                                        item = item,
                                        activity = activity,
                                        canToggle = canToggleOnThisDate,
                                        onToggle = {
                                            statusByDdlId[item.id]?.let { status ->
                                                habitViewModel.onToggleHabit(status.habit.id)
                                            }
                                        },
                                        statusByDdlId = statusByDdlId,
                                    )
                                }

                                else -> {
                                    TaskSearchItem(
                                        item = item,
                                        activity = activity,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedInspiration != null) {
        SearchInspirationDetailSheet(
            text = inspirationDraftText,
            onTextChange = { inspirationDraftText = it },
            onClose = { selectedInspiration = null },
            onSave = {
                val current = selectedInspiration ?: return@SearchInspirationDetailSheet
                updateInspirationText(current, inspirationDraftText)
                selectedInspiration = null
            },
            onDelete = {
                pendingDeleteInspiration = selectedInspiration
                selectedInspiration = null
            },
            onAiTask = { openInspirationEditor(inspirationDraftText, 0, true) },
            onAiHabit = { openInspirationEditor(inspirationDraftText, 1, true) },
            onDirectTask = { openInspirationEditor(inspirationDraftText, 0, false) },
            onDirectHabit = { openInspirationEditor(inspirationDraftText, 1, false) },
        )
    }

    pendingDeleteInspiration?.let { item ->
        val title = if (item.text.isBlank()) {
            stringResource(R.string.capture_delete_confirm_title)
        } else {
            stringResource(R.string.capture_delete_confirm_title_with_text, item.text)
        }
        val message = stringResource(R.string.capture_delete_confirm_message)
        AppAlertDialog(
            show = true,
            onDismissRequest = { pendingDeleteInspiration = null },
            title = {
                Text(text = title)
            },
            text = {
                Text(message)
            },
            miuixTitle = title,
            miuixSummary = message,
            confirmButton = {
                AppTextButton(
                    onClick = {
                        deleteInspiration(item)
                        pendingDeleteInspiration = null
                    },
                    miuixText = stringResource(R.string.accept),
                ) {
                    Text(stringResource(R.string.accept))
                }
            },
            dismissButton = {
                AppTextButton(
                    onClick = { pendingDeleteInspiration = null },
                    miuixText = stringResource(R.string.cancel),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingDeleteArchivedTask?.let { item ->
        val message = if (item.name.isBlank()) {
            stringResource(R.string.archive_delete_message_fallback)
        } else {
            stringResource(R.string.archive_delete_message, item.name)
        }
        AppAlertDialog(
            show = true,
            onDismissRequest = { pendingDeleteArchivedTask = null },
            title = { Text(stringResource(R.string.archive_delete_title)) },
            text = { Text(message) },
            miuixTitle = stringResource(R.string.archive_delete_title),
            miuixSummary = message,
            confirmButton = {
                AppTextButton(
                    onClick = {
                        onDeleteArchivedTask(item)
                        pendingDeleteArchivedTask = null
                    },
                    miuixText = stringResource(R.string.delete),
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                AppTextButton(
                    onClick = { pendingDeleteArchivedTask = null },
                    miuixText = stringResource(R.string.cancel),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingDeleteArchivedHabit?.let { habit ->
        val message = if (habit.name.isBlank()) {
            stringResource(R.string.archive_delete_message_fallback)
        } else {
            stringResource(R.string.archive_delete_message, habit.name)
        }
        AppAlertDialog(
            show = true,
            onDismissRequest = { pendingDeleteArchivedHabit = null },
            title = { Text(stringResource(R.string.archive_delete_title)) },
            text = { Text(message) },
            miuixTitle = stringResource(R.string.archive_delete_title),
            miuixSummary = message,
            confirmButton = {
                AppTextButton(
                    onClick = {
                        onDeleteArchivedHabit(habit)
                        pendingDeleteArchivedHabit = null
                    },
                    miuixText = stringResource(R.string.delete),
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                AppTextButton(
                    onClick = { pendingDeleteArchivedHabit = null },
                    miuixText = stringResource(R.string.cancel),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchScopePicker(
    selectedScope: MainSearchScope,
    onScopeChange: (MainSearchScope) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        MainSearchScope.entries.forEachIndexed { index, scope ->
            val selected = scope == selectedScope
            val iconSlotWidth by animateDpAsState(
                targetValue = if (selected) 20.dp else 0.dp,
                animationSpec = tween(
                    durationMillis = 220,
                ),
                label = "search-scope-icon-slot",
            )
            val iconAlpha by animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = tween(
                    durationMillis = 180,
                ),
                label = "search-scope-icon-alpha",
            )
            val iconScale by animateFloatAsState(
                targetValue = if (selected) 1f else 0.82f,
                animationSpec = tween(
                    durationMillis = 220,
                ),
                label = "search-scope-icon-scale",
            )
            val textShift by animateDpAsState(
                targetValue = if (selected) 1.5.dp else 0.dp,
                animationSpec = tween(
                    durationMillis = 220,
                ),
                label = "search-scope-text-shift",
            )
            ToggleButton(
                checked = selected,
                onCheckedChange = { onScopeChange(scope) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                shapes = searchScopeButtonShapes(index, MainSearchScope.entries.lastIndex),
                colors = searchScopeButtonColors(),
                contentPadding = PaddingValues(
                    horizontal = 10.dp,
                    vertical = 0.dp,
                ),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(width = iconSlotWidth, height = 20.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_ok),
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .alpha(iconAlpha)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                },
                        )
                    }
                    Text(
                        text = stringResource(scope.labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                placeable.placeRelative(textShift.roundToPx(), 0)
                            }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun searchScopeButtonShapes(
    index: Int,
    lastIndex: Int,
): ToggleButtonShapes {
    return when (index) {
        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
        lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun searchScopeButtonColors() = ToggleButtonDefaults.toggleButtonColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    checkedContainerColor = MaterialTheme.colorScheme.primary,
    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
)

@Composable
private fun SearchMessageCard(
    title: String,
    body: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .size(40.dp)
                .alpha(0.7f),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun SearchSectionHeader(
    title: String,
    iconRes: Int,
) {
    Row(
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SearchInspirationCard(
    item: InspirationItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onAiTask: () -> Unit,
    onAiHabit: () -> Unit,
    onDirectTask: () -> Unit,
    onDirectHabit: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_quote),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.80f),
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = stringResource(R.string.settings_more),
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.capture_delete)) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.capture_ai_task)) },
                                onClick = {
                                    menuExpanded = false
                                    onAiTask()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.capture_ai_habit)) },
                                onClick = {
                                    menuExpanded = false
                                    onAiHabit()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.capture_direct_task)) },
                                onClick = {
                                    menuExpanded = false
                                    onDirectTask()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.capture_direct_habit)) },
                                onClick = {
                                    menuExpanded = false
                                    onDirectHabit()
                                },
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = formatInspirationRelativeTime(context, item.updatedAt),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.search_inspiration_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SearchInspirationDetailSheet(
    text: String,
    onTextChange: (String) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onAiTask: () -> Unit,
    onAiHabit: () -> Unit,
    onDirectTask: () -> Unit,
    onDirectHabit: () -> Unit,
) {
    var secondaryMenuExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(R.string.close),
                    )
                }
                Text(
                    stringResource(R.string.capture_detail_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onSave) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = stringResource(R.string.save),
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
                    cornerSize = 12.dp,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                stringResource(R.string.capture_ai_default_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(onClick = onAiTask) {
                    Text(stringResource(R.string.capture_ai_task))
                }
                FilledTonalButton(onClick = onAiHabit) {
                    Text(stringResource(R.string.capture_ai_habit))
                }
                Box {
                    OutlinedButton(onClick = { secondaryMenuExpanded = true }) {
                        Text(stringResource(R.string.capture_direct_secondary_short))
                    }
                    DropdownMenu(
                        expanded = secondaryMenuExpanded,
                        onDismissRequest = { secondaryMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.capture_direct_task)) },
                            onClick = {
                                secondaryMenuExpanded = false
                                onDirectTask()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.capture_direct_habit)) },
                            onClick = {
                                secondaryMenuExpanded = false
                                onDirectHabit()
                            },
                        )
                    }
                }
            }

            androidx.compose.material3.TextButton(onClick = onDelete) {
                Text(
                    text = stringResource(R.string.capture_delete_item),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun HabitSearchItem(
    item: DDLItem,
    activity: MainActivity,
    canToggle: Boolean,
    onToggle: () -> Unit,
    statusByDdlId: Map<Long, com.aritxonly.deadliner.model.HabitWithDailyStatus>,
) {
    val data = statusByDdlId[item.id] ?: return
    val context = LocalContext.current
    val st = GlobalUtils.parseDateTime(item.startTime)
    val et = GlobalUtils.parseDateTime(item.endTime)
    val status = DDLStatus.calculateStatus(st, et, isCompleted = item.isCompleted)
    val remainingText = et?.let {
        GlobalUtils.buildRemainingTime(
            context,
            st,
            et,
            false,
            LocalDateTime.now(),
        )
    }

    HabitRow(
        data = data,
        status = status,
        isSelected = false,
        canToggle = canToggle && data.scheduleState?.isDue != false,
        onToggle = onToggle,
        onLongPress = {
            val intent = DeadlineDetailActivity.newIntent(activity, item)
            activity.startActivity(intent)
        },
        remainingText = data.scheduleState?.formatHint(context) ?: remainingText,
    )
}

private fun formatInspirationRelativeTime(
    context: android.content.Context,
    timestampMillis: Long,
): String {
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
                ZoneId.systemDefault(),
            )
            date.format(DateTimeFormatter.ofPattern(context.getString(R.string.capture_date_short_pattern)))
        }
    }
}

@Composable
private fun TaskSearchItem(
    item: DDLItem,
    activity: MainActivity,
) {
    val startTime = GlobalUtils.parseDateTime(item.startTime)
    val endTime = GlobalUtils.parseDateTime(item.endTime)
    val now = LocalDateTime.now()

    val remainingTimeText =
        if (item.state == DDLState.ABANDONED)
            stringResource(R.string.abandoned)
        else if (!item.state.isCompletedFamily())
            GlobalUtils.buildRemainingTime(
                activity,
                startTime,
                endTime,
                true,
                now,
            )
        else stringResource(R.string.completed)

    val progress = computeProgress(startTime, endTime, now)
    val status =
        if (item.state.isCompletedFamily() || item.state.isAbandonedFamily()) {
            DDLStatus.COMPLETED
        } else {
            DDLStatus.calculateStatus(
                startTime,
                endTime,
                now,
                false,
            )
        }

    DDLItemCardSimplified(
        title = item.name,
        remainingTimeAlt = remainingTimeText,
        note = item.note,
        progress = progress,
        isStarred = item.isStared,
        useDisabledCompletedStyle = item.state.isAbandonedFamily(),
        status = status,
        onClick = {
            val intent = DeadlineDetailActivity.newIntent(activity, item)
            activity.startActivity(intent)
        },
    )
}
