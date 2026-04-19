package com.aritxonly.deadliner.data

import com.aritxonly.deadliner.AppSingletons
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.TaskStateMachine
import com.aritxonly.deadliner.model.TaskStateAction
import com.aritxonly.deadliner.model.transition
import com.aritxonly.deadliner.sync.SyncService
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DDLRepository(
    private val db: DatabaseHelper = AppSingletons.db,
    private val sync: SyncService = AppSingletons.sync
) {

    // —— 同步去抖（避免连点导致频繁发网） —— //
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pendingJob: Job? = null
    private fun scheduleSync() {
        pendingJob?.cancel()
        pendingJob = scope.launch {
            delay(800) // 800ms 去抖
            try { sync.syncOnce() } catch (_: Exception) {}
        }
    }

    // —— 封装原有数据库操作（在成功后“记账 + 触发同步”） —— //

    fun insertDDL(
        name: String,
        startTime: String,
        endTime: String,
        note: String = "",
        type: DeadlineType = DeadlineType.TASK,
        calendarEventId: Long? = null
    ): Long {
        val id = db.insertDDL(name, startTime, endTime, note, type, calendarEventId)
        sync.onLocalInserted(id)
        scheduleSync()
        return id
    }

    fun updateDDL(item: DDLItem) {
        db.updateDDL(item)
        sync.onLocalUpdated(item.id)
        scheduleSync()
    }

    fun applyTaskAction(
        itemId: Long,
        action: TaskStateAction,
        confirmed: Boolean = false,
        now: LocalDateTime = LocalDateTime.now()
    ): DDLItem {
        val latest = db.getDDLById(itemId)
            ?: throw IllegalArgumentException("DDL not found for id=$itemId")
        if (TaskStateMachine.isNoOp(latest.state, action)) {
            return latest
        }
        val updated = latest.transition(using = action, confirmed = confirmed, now = now)
        db.updateDDL(updated)
        sync.onLocalUpdated(updated.id)
        scheduleSync()
        return updated
    }

    fun deleteDDL(id: Long) {
        sync.onLocalDeleting(id)   // 先记删除日志（包含远端识别用的 uid）
        db.deleteDDL(id)       // 再真实删除
        scheduleSync()
    }

    // —— 读操作直接透传（按你需要逐步搬迁） —— //
    fun getAllDDLs(): List<DDLItem> = db.getAllDDLs()
    fun getDDLById(id: Long)  = db.getDDLById(id)
    fun getDDLsByType(type: com.aritxonly.deadliner.model.DeadlineType) = db.getDDLsByType(type)

    // —— 手动同步（给“立即同步”按钮用） —— //
    suspend fun syncNow(): Boolean = sync.syncOnce()
}
