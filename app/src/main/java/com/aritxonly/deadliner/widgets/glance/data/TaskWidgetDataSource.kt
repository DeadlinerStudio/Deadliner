package com.aritxonly.deadliner.widgets.glance.data

import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.domain.task.TaskState

internal data class TaskWidgetTaskRecord(
    val title: String,
    val dueAt: String?,
    val isStarred: Boolean,
    val isDeleted: Boolean,
    val isActive: Boolean,
)

internal fun interface TaskWidgetDataSource {
    fun loadTasks(): List<TaskWidgetTaskRecord>
}

internal class KmpTaskWidgetDataSource : TaskWidgetDataSource {
    override fun loadTasks(): List<TaskWidgetTaskRecord> =
        KmpV6Runtime.database.tasks.list().map { task ->
            TaskWidgetTaskRecord(
                title = task.title,
                dueAt = task.dueAt,
                isStarred = task.isStarred,
                isDeleted = task.isDeleted,
                isActive = task.state == TaskState.ACTIVE,
            )
        }
}
