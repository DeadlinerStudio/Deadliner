package com.aritxonly.deadliner.ui.main.modern

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.semantic.DeadlineSemanticInput
import com.aritxonly.deadliner.model.semantic.HabitSemanticInput
import com.aritxonly.deadliner.model.semantic.PageToneResolver
import com.aritxonly.deadliner.model.semantic.SemanticTone
import com.aritxonly.deadliner.ui.category.CategoryFilter
import com.aritxonly.deadliner.ui.category.CategoryFilterResolver
import com.aritxonly.deadliner.ui.main.shared.MainSection
import com.aritxonly.deadliner.ui.semantic.SemanticTopAtmosphere
import com.aritxonly.deadliner.ui.semantic.SemanticAtmosphereModule
import com.deadliner.domain.task.Task
import com.deadliner.viewmodel.habit.HabitDayStatus
import java.time.LocalDateTime

internal fun semanticAtmosphereModuleFor(section: MainSection): SemanticAtmosphereModule? = when (section) {
    MainSection.LIST -> null
    MainSection.OVERVIEW -> SemanticAtmosphereModule.OVERVIEW
    MainSection.CAPTURE -> SemanticAtmosphereModule.CAPTURE
    MainSection.LIFI -> null
}

internal fun shouldShowSemanticAtmosphere(section: MainSection): Boolean = section != MainSection.LIFI

internal fun semanticAtmosphereMaterialProgress(
    section: MainSection,
    listProgress: Float,
    observedScrollProgress: Float?,
): Float = when (section) {
    MainSection.LIST -> listProgress
    MainSection.OVERVIEW,
    MainSection.CAPTURE -> observedScrollProgress ?: 0f
    MainSection.LIFI -> 0f
}.coerceIn(0f, 1f)

@Composable
internal fun ModernSemanticAtmosphere(
    tasks: List<Task>,
    visibleHabits: List<HabitDayStatus>,
    categoryFilter: CategoryFilter,
    availableCategoryUids: Set<String>,
    selectedSection: MainSection,
    selectedPage: DeadlineType,
    materialProgress: Float,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            kotlinx.coroutines.delay(60_000L)
        }
    }

    val taskInputs = remember(tasks) {
        tasks.map { task ->
            DeadlineSemanticInput(
                id = task.uid,
                state = task.state,
                dueAt = task.dueAt?.let { value ->
                    runCatching { GlobalUtils.parseDateTime(value) }.getOrNull()
                },
            )
        }
    }
    val taskSnapshot = remember(taskInputs, now) {
        PageToneResolver.resolveTasks(taskInputs, now)
    }
    val filteredHabits = remember(visibleHabits, categoryFilter, availableCategoryUids) {
        visibleHabits.filter { habit ->
            CategoryFilterResolver.matches(
                categoryFilter,
                habit.habit.categoryUid,
                availableCategoryUids,
            )
        }
    }
    val habitSnapshot = remember(filteredHabits) {
        PageToneResolver.resolveHabits(
            filteredHabits.map { habit ->
                HabitSemanticInput(
                    id = habit.habit.uid,
                    isCompleted = habit.isCompleted,
                )
            },
        )
    }
    val tone = when (selectedSection) {
        MainSection.LIST -> when (selectedPage) {
            DeadlineType.TASK -> taskSnapshot.tone
            DeadlineType.HABIT -> habitSnapshot.tone
        }

        MainSection.OVERVIEW,
        MainSection.CAPTURE,
        MainSection.LIFI -> SemanticTone.CALM
    }
    val module = semanticAtmosphereModuleFor(selectedSection)

    SemanticTopAtmosphere(
        tone = tone,
        module = module,
        materialProgress = materialProgress,
        modifier = modifier,
    )
}
