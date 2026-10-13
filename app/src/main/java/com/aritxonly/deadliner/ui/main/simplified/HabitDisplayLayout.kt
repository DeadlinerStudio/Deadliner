package com.aritxonly.deadliner.ui.main.simplified

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.aritxonly.deadliner.ui.main.SwipeableItemCard
import com.aritxonly.deadliner.ui.focus.LocalHabitFocusTimerEntry
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.AddDDLActivity
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.Button
import top.yukonga.miuix.kmp.basic.Text
import com.deadliner.domain.focus.FocusTargetRef
import com.deadliner.domain.focus.FocusTargetKind
import com.deadliner.domain.habit.HabitStatusAction
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.model.DDLStatus
import com.deadliner.viewmodel.habit.HabitDayStatus
import com.deadliner.viewmodel.habit.HabitListViewModel
import com.deadliner.viewmodel.habit.HabitScheduleHintKind
import com.aritxonly.deadliner.ui.base.syncTopBarMaterialScroll
import com.aritxonly.deadliner.ui.main.shared.mainListContainerClip
import com.aritxonly.deadliner.ui.category.CategoryPresentation

private data class HabitDisplayItem(
    val habit: HabitDayStatus,
    val status: DDLStatus,
    val remainingText: String?,
)

@Composable
fun HabitScreen(
    habitViewModel: HabitListViewModel,
    selectionMode: Boolean = false,
    isSelected: (String) -> Boolean = { false },
    onItemLongPress: (String) -> Unit = {},
    onItemClickInSelection: (String) -> Unit = {},
    onToggleHabit: (String) -> Unit = {}
) {
    val state by habitViewModel.uiState.collectAsState()

    HabitDisplayLayout(
        state.weekOverview, state.selectedDate, state.visibleHabits, habitViewModel,
        selectionMode = selectionMode,
        isSelected = isSelected,
        onItemLongPress = onItemLongPress,
        onItemClickInSelection = onItemClickInSelection,
        onToggleHabit = onToggleHabit,
        isClassic = true
    )
}

@Composable
fun HabitDisplayLayout(
    weekOverview: List<com.deadliner.viewmodel.habit.HabitDayOverview>,
    selectedDate: String,
    habits: List<HabitDayStatus>,
    habitViewModel: HabitListViewModel,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    isSelected: (String) -> Boolean = { false },
    onItemLongPress: (String) -> Unit = {},
    onItemClickInSelection: (String) -> Unit = {},
    onToggleHabit: (String) -> Unit = {},
    isClassic: Boolean = false,
    listState: LazyListState = LazyListState(),
    categoriesByUid: Map<String, CategoryPresentation> = emptyMap(),
    activeSwipeKey: String? = null,
    onActiveSwipeChange: ((String?) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusEntry = LocalHabitFocusTimerEntry.current
    val swipeColors = com.aritxonly.deadliner.ui.theme.rememberTaskSwipeActionColors()
    var localSwipeKey by remember { mutableStateOf<String?>(null) }
    val resolvedSwipeKey = if (onActiveSwipeChange != null) activeSwipeKey else localSwipeKey
    val updateSwipeKey: (String?) -> Unit = { if (onActiveSwipeChange != null) onActiveSwipeChange(it) else localSwipeKey = it }
    LaunchedEffect(selectedDate, selectionMode, isClassic) { updateSwipeKey(null) }
    var pendingChange by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var pendingSessionUid by remember { mutableStateOf<String?>(null) }
    fun requestChange(uid: String, deleting: Boolean) {
        pendingSessionUid = KmpV6Runtime.focusTimerViewModel.uiState.value.timer
            ?.takeIf { it.target == FocusTargetRef(FocusTargetKind.HABIT, uid) }?.sessionUid
        pendingChange = uid to deleting
    }
    val pending = pendingChange
    val hasFocus = pendingSessionUid != null
    AlertDialog(show = pending != null, onDismissRequest = { pendingChange = null },
        miuixTitle = stringResource(if (pending?.second == true) R.string.delete else R.string.archive),
        miuixSummary = if (hasFocus) stringResource(R.string.habit_focus_end_before_change) else stringResource(R.string.habit_focus_change_confirm),
        confirmButton = { Button(onClick = {
            val change = pending ?: return@Button
            val expectedSession = pendingSessionUid
            pendingChange = null
            scope.launch {
                if (expectedSession != null) {
                    val next = KmpV6Runtime.focusTimerViewModel.resolveHabitChangeNow(change.first, change.second, expectedSession)
                    if (next.lastOutcome !in setOf(com.deadliner.domain.focus.FocusTimerOutcome.APPLIED, com.deadliner.domain.focus.FocusTimerOutcome.NO_OP)) {
                        android.widget.Toast.makeText(context, R.string.focus_confirmation_expired, android.widget.Toast.LENGTH_LONG).show()
                    }
                }
                else if (change.second) habitViewModel.deleteHabitNow(change.first, java.time.Instant.now().toString())
                else habitViewModel.applyHabitStatusNow(change.first, HabitStatusAction.ARCHIVE, java.time.Instant.now().toString())
                KmpV6Runtime.focusTimerViewModel.loadNow()
                habitViewModel.loadNow()
                KmpV6Runtime.refreshNotifications()
            }
        }) { Text(stringResource(R.string.accept)) } },
        dismissButton = { Button(onClick = { pendingChange = null }) { Text(stringResource(R.string.cancel)) } })

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        val todayOverview = weekOverview.firstOrNull { it.date == selectedDate }
        val ratio = todayOverview?.completionRatio ?: 0f

        if (!isClassic) {
            HabitCircleProgress(
                progress = ratio,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
        } else {
            HabitLinearProgress(
                progress = ratio,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        }

        // 本周日期横条
        WeekRow(
            weekOverview = weekOverview,
            selectedDate = selectedDate,
            onSelectDate = habitViewModel::selectDate
        )

        val displayHabits = remember(habits, context) {
            habits.map { item ->
                val remainingText = when (item.scheduleState.hintKind) {
                    HabitScheduleHintKind.STARTS_IN -> item.scheduleState.daysUntil?.let { "${it}d" }
                    HabitScheduleHintKind.NEXT_REVIEW_IN -> item.scheduleState.daysUntil?.let { "${it}d" }
                    HabitScheduleHintKind.COMPLETED_CYCLE -> null
                    null -> null
                }
                HabitDisplayItem(
                    habit = item,
                    status = if (item.isCompleted) DDLStatus.COMPLETED else DDLStatus.UNDERGO,
                    remainingText = remainingText,
                )
            }
        }

        // 习惯列表
        LazyColumn(
            contentPadding = PaddingValues(
                top = 12.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .syncTopBarMaterialScroll(listState)
                .mainListContainerClip(),
            state = listState,
        ) {
            itemsIndexed(
                items = displayHabits,
                key = { _, it -> it.habit.habit.uid }
            ) { index, item ->
                val uid = item.habit.habit.uid
                val selected = isSelected(uid)

                SwipeableItemCard(modifier = Modifier.testTag("habit-card-$uid"), swipeKey = "habit:$uid", activeSwipeKey = resolvedSwipeKey,
                    onActiveSwipeChange = updateSwipeKey,
                    primaryActionLabel = stringResource(R.string.archive), secondaryActionLabel = stringResource(R.string.focus_swipe),
                    primaryActionIcon = com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_move_to_inbox_wght400),
                    primaryActionColor = swipeColors.archive,
                    secondaryActionColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    secondaryActionIcon = com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_schedule_wght400),
                    onPrimaryAction = { requestChange(uid, false) },
                    onSecondaryAction = { focusEntry(uid, item.habit.habit.name) },
                    onEditAction = { context.startActivity(AddDDLActivity.newEditHabitIntent(context, uid)) },
                    onDeleteAction = { requestChange(uid, true) }, selectionMode = selectionMode, accessibilityLabel = item.habit.habit.name,
                ) { closeOrRun ->
                if (!isClassic) {
                    HabitRow(
                        data = item.habit,
                        status = item.status,
                        isSelected = selected,
                        canToggle = selectionMode || item.habit.canApplyPrimaryAction,
                        onToggle = { closeOrRun {
                            if (selectionMode) {
                                onItemClickInSelection(uid)
                            } else {
                                onToggleHabit(uid)
                            }
                        } },
                        onLongPress = {
                            onItemLongPress(uid)
                        },
                        remainingText = item.remainingText,
                        category = item.habit.habit.categoryUid?.let(categoriesByUid::get),
                    )
                } else {
                    HabitRowClassic(
                        data = item.habit,
                        isSelected = selected,
                        canToggle = selectionMode || item.habit.canApplyPrimaryAction,
                        onToggle = { closeOrRun {
                            if (selectionMode) {
                                onItemClickInSelection(uid)
                            } else {
                                onToggleHabit(uid)
                            }
                        } },
                        onLongPress = {
                            onItemLongPress(uid)
                        },
                        remainingText = item.remainingText,
                        category = item.habit.habit.categoryUid?.let(categoriesByUid::get),
                    )
                }
                }
            }
        }
    }
}
