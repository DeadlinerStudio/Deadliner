package com.aritxonly.deadliner.ui.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.data.HabitRepository
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.Habit
import com.aritxonly.deadliner.model.HabitStatus
import com.aritxonly.deadliner.model.TaskStateAction
import com.aritxonly.deadliner.ui.base.AdaptiveMaterialScaffold
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.Button
import com.aritxonly.deadliner.ui.base.TabRow
import com.aritxonly.deadliner.ui.base.TextButton
import com.aritxonly.deadliner.ui.base.TopAppBar
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.advancedTextureBlur
import com.aritxonly.deadliner.ui.theme.rememberBlurColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.blur.BlendColorEntry

private enum class ArchiveTab {
    TASK,
    HABIT,
}

private sealed interface ArchiveDeleteTarget {
    val displayName: String

    data class Task(val item: DDLItem) : ArchiveDeleteTarget {
        override val displayName: String = item.name
    }

    data class Habit(val habit: com.aritxonly.deadliner.model.Habit) : ArchiveDeleteTarget {
        override val displayName: String = habit.name
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onClose: () -> Unit,
    onDataChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val taskRepo = remember { DDLRepository() }
    val habitRepo = remember { HabitRepository() }
    val pageSurfaceColor = MaterialTheme.colorScheme.surface
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val layoutDirection = LocalLayoutDirection.current

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var archivedTasks by remember { mutableStateOf<List<DDLItem>>(emptyList()) }
    var archivedHabits by remember { mutableStateOf<List<Habit>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var deleteTarget by remember { mutableStateOf<ArchiveDeleteTarget?>(null) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteAllDialog by rememberSaveable { mutableStateOf(false) }

    val selectedTab = ArchiveTab.entries[selectedTabIndex]
    val tabTitles = listOf(
        stringResource(R.string.task),
        stringResource(R.string.habit),
    )
    val canDeleteAll = when (selectedTab) {
        ArchiveTab.TASK -> archivedTasks.isNotEmpty()
        ArchiveTab.HABIT -> archivedHabits.isNotEmpty()
    }
    val resultCount = when (selectedTab) {
        ArchiveTab.TASK -> archivedTasks.size
        ArchiveTab.HABIT -> archivedHabits.size
    }

    suspend fun refreshData() {
        isLoading = true
        try {
            when (selectedTab) {
                ArchiveTab.TASK -> {
                    archivedTasks = withContext(Dispatchers.IO) {
                        taskRepo.getAllDDLs()
                            .filter { it.state.isArchiveListVisible() }
                            .sortedByDescending { it.completeTime.ifBlank { it.endTime } }
                    }
                }
                ArchiveTab.HABIT -> {
                    archivedHabits = withContext(Dispatchers.IO) {
                        habitRepo.getAllHabits()
                            .filter { it.status == HabitStatus.ARCHIVED }
                            .sortedByDescending { it.updatedAt }
                    }
                }
            }
        } finally {
            isLoading = false
        }
    }

    fun reload() {
        scope.launch { refreshData() }
    }

    LaunchedEffect(selectedTab) {
        refreshData()
    }

    DisposableEffect(lifecycleOwner, selectedTab) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                reload()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AdaptiveMaterialScaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentColor = contentColorFor(Color.Transparent),
        contentWindowInsets = WindowInsets(0),
        advancedMaterialTopBarTintColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (advancedMaterial.enabled) Color.Transparent else pageSurfaceColor),
            ) {
                TopAppBar(
                    title = stringResource(R.string.archive),
                    mode = TopAppBarStyle.CENTER,
                    titleTextStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                    useParentMaterialContainer = advancedMaterial.enabled,
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.close),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                )
                TabRow(
                    tabs = tabTitles,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                    divider = { HorizontalDivider(color = androidx.compose.ui.graphics.Color.Transparent) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                )
            }
        },
        bottomBar = {
            ArchiveBottomActions(
                enabled = canDeleteAll,
                onDeleteAllClick = { showDeleteAllDialog = true },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(pageSurfaceColor),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(pageSurfaceColor)
                    .padding(
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        end = innerPadding.calculateEndPadding(layoutDirection),
                    ),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    bottom = 160.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!isLoading && resultCount > 0) {
                    item {
                        ArchiveSectionHeader(
                            title = stringResource(
                                when (selectedTab) {
                                    ArchiveTab.TASK -> R.string.archive_section_tasks
                                    ArchiveTab.HABIT -> R.string.archive_section_habits
                                }
                            ),
                            count = resultCount,
                        )
                    }
                }

                if (!isLoading) {
                    if (selectedTab == ArchiveTab.TASK && archivedTasks.isEmpty()) {
                        item {
                            ArchiveEmptyState(
                                title = stringResource(R.string.archive_empty_tasks),
                                description = stringResource(R.string.archive_empty_tasks_description),
                            )
                        }
                    } else if (selectedTab == ArchiveTab.HABIT && archivedHabits.isEmpty()) {
                        item {
                            ArchiveEmptyState(
                                title = stringResource(R.string.archive_empty_habits),
                                description = stringResource(R.string.archive_empty_habits_description),
                            )
                        }
                    } else if (selectedTab == ArchiveTab.TASK) {
                        items(
                            items = archivedTasks,
                            key = { it.id },
                        ) { item ->
                            ArchivedTaskCard(
                                item = item,
                                onRestore = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            taskRepo.applyTaskAction(
                                                itemId = item.id,
                                                action = TaskStateAction.UNARCHIVE,
                                                confirmed = true,
                                            )
                                        }
                                        onDataChanged()
                                        refreshData()
                                    }
                                },
                                onDelete = {
                                    deleteTarget = ArchiveDeleteTarget.Task(item)
                                    showDeleteDialog = true
                                },
                            )
                        }
                    } else {
                        items(
                            items = archivedHabits,
                            key = { it.id },
                        ) { habit ->
                            ArchivedHabitCard(
                                habit = habit,
                                onRestore = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            habitRepo.updateHabit(
                                                habit.copy(status = HabitStatus.ACTIVE),
                                            )
                                        }
                                        onDataChanged()
                                        refreshData()
                                    }
                                },
                                onDelete = {
                                    deleteTarget = ArchiveDeleteTarget.Habit(habit)
                                    showDeleteDialog = true
                                },
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }

    AlertDialog(
        show = showDeleteDialog,
        onDismissRequest = {
            showDeleteDialog = false
            deleteTarget = null
        },
        title = { Text(stringResource(R.string.archive_delete_title)) },
        text = {
            val name = deleteTarget?.displayName.orEmpty()
            Text(
                text = if (name.isBlank()) {
                    stringResource(R.string.archive_delete_message_fallback)
                } else {
                    stringResource(R.string.archive_delete_message, name)
                }
            )
        },
        miuixTitle = stringResource(R.string.archive_delete_title),
        miuixSummary = deleteTarget?.displayName
            ?.takeIf { it.isNotBlank() }
            ?.let { stringResource(R.string.archive_delete_message, it) }
            ?: stringResource(R.string.archive_delete_message_fallback),
        dismissButton = {
            TextButton(
                onClick = {
                    showDeleteDialog = false
                    deleteTarget = null
                },
                miuixText = stringResource(R.string.cancel),
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val target = deleteTarget ?: return@TextButton
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            when (target) {
                                is ArchiveDeleteTarget.Task -> taskRepo.deleteDDL(target.item.id)
                                is ArchiveDeleteTarget.Habit -> habitRepo.deleteHabitByDdlId(target.habit.ddlId)
                            }
                        }
                        onDataChanged()
                        showDeleteDialog = false
                        deleteTarget = null
                        refreshData()
                    }
                },
                miuixText = stringResource(R.string.delete),
            ) {
                Text(
                    text = stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )

    AlertDialog(
        show = showDeleteAllDialog,
        onDismissRequest = { showDeleteAllDialog = false },
        title = { Text(stringResource(R.string.archive_delete_all_title)) },
        text = {
            Text(
                text = stringResource(
                    when (selectedTab) {
                        ArchiveTab.TASK -> R.string.archive_delete_all_tasks_message
                        ArchiveTab.HABIT -> R.string.archive_delete_all_habits_message
                    }
                )
            )
        },
        miuixTitle = stringResource(R.string.archive_delete_all_title),
        miuixSummary = stringResource(
            when (selectedTab) {
                ArchiveTab.TASK -> R.string.archive_delete_all_tasks_message
                ArchiveTab.HABIT -> R.string.archive_delete_all_habits_message
            }
        ),
        dismissButton = {
            TextButton(
                onClick = { showDeleteAllDialog = false },
                miuixText = stringResource(R.string.cancel),
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            when (selectedTab) {
                                ArchiveTab.TASK -> archivedTasks.forEach { taskRepo.deleteDDL(it.id) }
                                ArchiveTab.HABIT -> archivedHabits.forEach { habitRepo.deleteHabitByDdlId(it.ddlId) }
                            }
                        }
                        onDataChanged()
                        showDeleteAllDialog = false
                        refreshData()
                    }
                },
                miuixText = stringResource(R.string.clear_all),
            ) {
                Text(
                    text = stringResource(R.string.clear_all),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

@Composable
private fun ArchiveEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_archive),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(52.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ArchiveSectionHeader(
    title: String,
    count: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.weight(1f))
        ArchiveCountPill(count = count)
    }
}

@Composable
private fun ArchiveCountPill(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Text(
            text = stringResource(R.string.archive_count_label, count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun ArchiveBottomActions(
    enabled: Boolean,
    onDeleteAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val backdrop = LocalAdvancedMaterialBackdrop.current
    val surfaceTint = MaterialTheme.colorScheme.surface.copy(alpha = advancedMaterial.navigationTintAlpha * 0.55f)
    val materialColors = advancedMaterial.rememberBlurColors(listOf(BlendColorEntry(surfaceTint)))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (advancedMaterial.enabled && backdrop != null) {
                    Modifier.advancedTextureBlur(
                        advancedMaterial = advancedMaterial,
                        backdrop = backdrop,
                        shape = RectangleShape,
                        colors = materialColors,
                    )
                } else {
                    Modifier.background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RectangleShape,
                    )
                }
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = 12.dp,
                ),
        ) {
            Button(
                onClick = onDeleteAllClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(text = stringResource(R.string.clear_all))
            }
        }
    }
}
