package com.aritxonly.deadliner.widgets.glance.data

import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.domain.task.TaskState

internal data class TaskWidgetTaskRecord(
    val id: String,
    val title: String,
    val dueAt: String?,
    val isDeleted: Boolean,
    val isActive: Boolean,
    val categoryColorHex: String? = null,
)

internal fun interface TaskWidgetDataSource {
    fun loadTasks(): List<TaskWidgetTaskRecord>
}

internal class KmpTaskWidgetDataSource : TaskWidgetDataSource {
    override fun loadTasks(): List<TaskWidgetTaskRecord> {
        val categoryColors = KmpV6Runtime.database.categories.list()
            .asSequence()
            .filterNot { it.isDeleted }
            .associate { it.uid to it.colorHex }
        return KmpV6Runtime.database.tasks.list().map { task ->
            TaskWidgetTaskRecord(
                id = task.uid,
                title = task.title,
                dueAt = task.dueAt,
                isDeleted = task.isDeleted,
                isActive = task.state == TaskState.ACTIVE,
                categoryColorHex = task.categoryUid?.let(categoryColors::get),
            )
        }
    }
}
