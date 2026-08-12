package com.aritxonly.deadliner.ui.main.shared

import androidx.annotation.StringRes
import com.aritxonly.deadliner.R
import com.deadliner.domain.capture.CaptureItem

enum class MainSearchScope(@StringRes val labelRes: Int) {
    ALL(R.string.search_scope_all),
    ACTIVE(R.string.search_scope_active),
    ARCHIVE(R.string.search_scope_archive),
}

fun buildInspirationSearchResults(
    query: String,
    scope: MainSearchScope,
    inspirations: List<CaptureItem>,
): List<CaptureItem> {
    if (query.isBlank() || scope == MainSearchScope.ARCHIVE) return emptyList()
    return inspirations.filter { it.text.contains(query.trim(), ignoreCase = true) }
}
