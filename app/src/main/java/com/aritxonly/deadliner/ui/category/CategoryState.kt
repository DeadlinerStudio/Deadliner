package com.aritxonly.deadliner.ui.category

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime

@Composable
fun rememberActiveCategories(refreshKey: Any? = Unit): List<CategoryPresentation> {
    val lifecycleOwner = LocalLifecycleOwner.current
    fun load() = KmpV6Runtime.database.categories.list()
        .filterNot { it.isDeleted }
        .sortedBy { it.sortOrder }
        .map { it.toPresentation() }

    var categories by remember { mutableStateOf(load()) }

    LaunchedEffect(refreshKey) {
        categories = load()
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) categories = load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return categories
}
