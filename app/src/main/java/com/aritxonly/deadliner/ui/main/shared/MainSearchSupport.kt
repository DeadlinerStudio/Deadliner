package com.aritxonly.deadliner.ui.main.shared

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.capture.model.InspirationItem
import com.aritxonly.deadliner.localutils.SearchFilter
import com.aritxonly.deadliner.model.DDLState
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineType
import java.time.LocalDateTime

enum class MainSearchScope(
    @StringRes val labelRes: Int,
) {
    ALL(R.string.search_scope_all),
    ACTIVE(R.string.search_scope_active),
    ARCHIVE(R.string.search_scope_archive),
    ;

    val allowsActive: Boolean
        get() = this == ALL || this == ACTIVE

    val allowsArchive: Boolean
        get() = this == ALL || this == ARCHIVE
}

data class MainSearchSection(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val items: List<DDLItem>,
)

fun buildMainSearchSections(
    query: String,
    scope: MainSearchScope,
    items: List<DDLItem>,
): List<MainSearchSection> {
    val trimmedQuery = query.trim()
    if (trimmedQuery.isEmpty()) return emptyList()

    val filter = SearchFilter.parse(trimmedQuery)
    val tokens = queryTokens(filter.query)

    fun score(item: DDLItem, visibilityScope: SearchFilter.VisibilityScope): Int? {
        if (!matchesVisibility(item, visibilityScope)) return null
        if (!matchesDateFilters(item, filter)) return null

        if (tokens.isEmpty()) {
            return if (filter.query.isBlank()) 1 else null
        }

        val subtitle = taskSubtitle(item)
        val detail = listOf(item.note, item.startTime, item.endTime, item.completeTime)
            .joinToString(separator = "\n")
        val score = searchScore(
            title = item.name,
            subtitle = subtitle,
            detail = detail,
            tokens = tokens,
        )
        return score.takeIf { it > 0 }
    }

    val activeTasks = if (scope.allowsActive) {
        items.rankForSearch(
            visibilityScope = SearchFilter.VisibilityScope.ACTIVE,
            type = DeadlineType.TASK,
            score = ::score,
        )
    } else {
        emptyList()
    }
    val activeHabits = if (scope.allowsActive) {
        items.rankForSearch(
            visibilityScope = SearchFilter.VisibilityScope.ACTIVE,
            type = DeadlineType.HABIT,
            score = ::score,
        )
    } else {
        emptyList()
    }
    val archivedTasks = if (scope.allowsArchive) {
        items.rankForSearch(
            visibilityScope = SearchFilter.VisibilityScope.ARCHIVE,
            type = DeadlineType.TASK,
            score = ::score,
        )
    } else {
        emptyList()
    }
    val archivedHabits = if (scope.allowsArchive) {
        items.rankForSearch(
            visibilityScope = SearchFilter.VisibilityScope.ARCHIVE,
            type = DeadlineType.HABIT,
            score = ::score,
        )
    } else {
        emptyList()
    }

    return buildList {
        if (activeTasks.isNotEmpty()) {
            add(MainSearchSection(R.string.search_group_task, R.drawable.ic_task, activeTasks))
        }
        if (activeHabits.isNotEmpty()) {
            add(MainSearchSection(R.string.search_group_habit, R.drawable.ic_habit, activeHabits))
        }
        if (archivedTasks.isNotEmpty()) {
            add(MainSearchSection(R.string.search_group_archived_task, R.drawable.ic_archive, archivedTasks))
        }
        if (archivedHabits.isNotEmpty()) {
            add(MainSearchSection(R.string.search_group_archived_habit, R.drawable.ic_archive, archivedHabits))
        }
    }
}

fun buildInspirationSearchResults(
    query: String,
    scope: MainSearchScope,
    inspirations: List<InspirationItem>,
): List<InspirationItem> {
    val trimmedQuery = query.trim()
    if (trimmedQuery.isEmpty() || !scope.allowsActive) return emptyList()

    val tokens = queryTokens(SearchFilter.parse(trimmedQuery).query)
    if (tokens.isEmpty()) return emptyList()

    return inspirations
        .mapNotNull { item ->
            val score = searchScore(
                title = item.text,
                subtitle = item.text,
                detail = item.text,
                tokens = tokens,
            )
            if (score > 0) item to score else null
        }
        .sortedWith(
            compareByDescending<Pair<InspirationItem, Int>> { it.second }
                .thenBy { it.first.text.lowercase() }
        )
        .map { it.first }
}

private fun List<DDLItem>.rankForSearch(
    visibilityScope: SearchFilter.VisibilityScope,
    type: DeadlineType,
    score: (DDLItem, SearchFilter.VisibilityScope) -> Int?,
): List<DDLItem> {
    return asSequence()
        .filter { it.type == type }
        .mapNotNull { item -> score(item, visibilityScope)?.let { item to it } }
        .sortedWith(
            compareByDescending<Pair<DDLItem, Int>> { it.second }
                .thenBy { it.first.name.lowercase() }
        )
        .map { it.first }
        .toList()
}

private fun matchesVisibility(
    item: DDLItem,
    visibilityScope: SearchFilter.VisibilityScope,
): Boolean {
    return when (visibilityScope) {
        SearchFilter.VisibilityScope.ACTIVE -> item.state.isMainListVisible()
        SearchFilter.VisibilityScope.ARCHIVE -> item.state.isArchiveListVisible()
        SearchFilter.VisibilityScope.ALL ->
            item.state.isMainListVisible() || item.state.isArchiveListVisible()
    }
}

private fun matchesDateFilters(
    item: DDLItem,
    filter: SearchFilter,
): Boolean {
    if (filter.year == null && filter.month == null && filter.day == null && filter.hour == null) {
        return true
    }

    val startTime = runCatching { LocalDateTime.parse(item.startTime) }.getOrNull()
    val completeTime = runCatching { LocalDateTime.parse(item.completeTime) }.getOrNull()
    val endTime = runCatching { LocalDateTime.parse(item.endTime) }.getOrNull()

    fun matches(t: LocalDateTime?): Boolean {
        return (filter.year == null || t?.year == filter.year) &&
            (filter.month == null || t?.monthValue == filter.month) &&
            (filter.day == null || t?.dayOfMonth == filter.day) &&
            (filter.hour == null || t?.hour == filter.hour)
    }

    return matches(startTime) || matches(endTime) || matches(completeTime)
}

private fun queryTokens(query: String): List<String> {
    return query
        .lowercase()
        .split(Regex("\\s+"))
        .map(String::trim)
        .filter { it.isNotEmpty() }
}

private fun searchScore(
    title: String,
    subtitle: String,
    detail: String,
    tokens: List<String>,
): Int {
    if (tokens.isEmpty()) return 0

    val titleLower = title.lowercase()
    val subtitleLower = subtitle.lowercase()
    val detailLower = detail.lowercase()

    var score = 0

    for (token in tokens) {
        if (!titleLower.contains(token) && !subtitleLower.contains(token) && !detailLower.contains(token)) {
            return 0
        }

        score += when {
            titleLower == token -> 140
            titleLower.startsWith(token) -> 100
            titleLower.contains(token) -> 70
            else -> 0
        }

        if (subtitleLower.contains(token)) score += 28
        if (detailLower.contains(token)) score += 12
    }

    if (tokens.size > 1) score += 16
    return score
}

private fun taskSubtitle(item: DDLItem): String {
    if (item.note.isNotBlank()) return item.note

    return when {
        item.state.isArchiveListVisible() && item.state.isAbandonedFamily() && item.completeTime.isBlank() -> "已放弃归档"
        item.state.isArchiveListVisible() && item.completeTime.isNotBlank() -> item.completeTime
        item.state.isArchiveListVisible() -> "已归档"
        item.state == DDLState.ABANDONED -> "已放弃"
        item.endTime.isNotBlank() -> item.endTime
        else -> ""
    }
}
