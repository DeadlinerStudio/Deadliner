package com.aritxonly.deadliner.widgets.glance.data

import android.content.Context
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object TaskWidgetSnapshotLoader {
    suspend fun load(context: Context): TaskWidgetSnapshot = withContext(Dispatchers.IO) {
        TaskWidgetRepository(KmpTaskWidgetDataSource()).load(AndroidTaskWidgetStrings(context))
    }
}
