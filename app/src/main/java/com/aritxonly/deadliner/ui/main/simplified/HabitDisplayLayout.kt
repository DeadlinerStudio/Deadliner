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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
) {
    val context = LocalContext.current

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

                if (!isClassic) {
                    HabitRow(
                        data = item.habit,
                        status = item.status,
                        isSelected = selected,
                        canToggle = item.habit.canApplyPrimaryAction,
                        onToggle = {
                            if (selectionMode) {
                                onItemClickInSelection(uid)
                            } else {
                                onToggleHabit(uid)
                            }
                        },
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
                        canToggle = item.habit.canApplyPrimaryAction,
                        onToggle = {
                            if (selectionMode) {
                                onItemClickInSelection(uid)
                            } else {
                                onToggleHabit(uid)
                            }
                        },
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
