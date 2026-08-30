package com.aritxonly.deadliner.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.aritxonly.deadliner.DeadlineAlarmScheduler
import com.aritxonly.deadliner.localutils.SearchFilter
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.TaskStateAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime

class MainViewModel(
    private val repo: DDLRepository
) : ViewModel() {
    private val _refreshState = MutableStateFlow<RefreshState>(RefreshState.Idle)
    val refreshState: StateFlow<RefreshState> = _refreshState

    // 用于存储经过筛选、排序后的数据
    private val _ddlList = MutableStateFlow<List<DDLItem>>(emptyList())
    val ddlList: LiveData<List<DDLItem>> = _ddlList.asLiveData()
    val ddlListFlow: StateFlow<List<DDLItem>> = _ddlList
    private val _taskList = MutableStateFlow<List<DDLItem>>(emptyList())
    val taskListFlow: StateFlow<List<DDLItem>> = _taskList
    private val _habitList = MutableStateFlow<List<DDLItem>>(emptyList())
    val habitListFlow: StateFlow<List<DDLItem>> = _habitList

    // 用于存储即将到的DDL
    private val _dueSoonCounts = MutableStateFlow<Map<DeadlineType, Int>>(emptyMap())
    val dueSoonCounts: LiveData<Map<DeadlineType, Int>> = _dueSoonCounts.asLiveData()

    // 当前筛选的 DeadlineType
    var currentType: DeadlineType = DeadlineType.TASK

    fun isEmpty(): Boolean? = _ddlList.value.isEmpty()

    /**
     * 计算某个类型下“即将到期”的 DDL 数量：
     * 例如：剩余时间小于 24 小时且未完成且未归档
     */
    private fun computeDueSoonCount(type: DeadlineType): Int {
        val now = LocalDateTime.now()
        return repo.getDDLsByType(type)
            .count { item ->
                if (!item.state.isActionable() || item.endTime.isEmpty()) return@count false
                val end = try {
                    GlobalUtils.parseDateTime(item.endTime)
                } catch (e: Exception) {
                    return@count false
                }
                if (end == null) return@count false
                val remaining = Duration.between(now, end).toMinutes()
                remaining <= 720
            }
    }

    /**
     * 对外一次性获取某 type 下的即将到期数量
     */
    fun dueSoonCount(type: DeadlineType): Int = computeDueSoonCount(type)

    private fun syncSelectedList() {
        _ddlList.value = when (currentType) {
            DeadlineType.TASK -> _taskList.value
            DeadlineType.HABIT -> _habitList.value
        }
    }

    fun selectType(type: DeadlineType) {
        currentType = type
        syncSelectedList()
    }

    private fun filterDataByList(ddlList: List<DDLItem>, type: DeadlineType): List<DDLItem> {
        var changed = false
        ddlList.forEach { item ->
            Log.d("updateData", "item ${item.id}, name ${item.name}, completeTime ${item.completeTime}, state ${item.state}")
            if (
                item.completeTime.isNotEmpty() &&
                item.state.canManualArchive() &&
                !GlobalUtils.filterArchived(item)
            ) {
                repo.applyTaskAction(item.id, TaskStateAction.MARK_ARCHIVE, confirmed = true)
                changed = true
            }
        }

        val source = if (changed) repo.getDDLsByType(type) else ddlList
        return source
            .filter { it.state.isMainListVisible() }
            .sortedWith(
            compareBy<DDLItem> { !it.state.isActionable() }
                .thenBy { !it.isStared }
                .thenBy {
                    when (GlobalUtils.filterSelection) {
                        1 -> {  // 按名称
                            it.name
                        }
                        2 -> {  // 按开始时间
                            GlobalUtils.safeParseDateTime(it.startTime)
                        }
                        3 -> {  // 按百分比
                            val startTime = GlobalUtils.safeParseDateTime(it.startTime)
                            val endTime = GlobalUtils.safeParseDateTime(it.endTime)
                            val remainingMinutes =
                                Duration.between(LocalDateTime.now(), endTime).toMinutes().toInt()
                            val fullTime =
                                Duration.between(startTime, endTime).toMinutes().toInt()
                            val progress = remainingMinutes.toFloat() / fullTime.toFloat()
                            progress
                        }
                        else -> {
                            val endTime = GlobalUtils.safeParseDateTime(it.endTime)
                            val remainingMinutes =
                                Duration.between(LocalDateTime.now(), endTime).toMinutes().toInt()
                            remainingMinutes
                        }
                    }
                }
            )
    }

    /**
     * 加载数据：调用 DatabaseHelper 根据 type 获取数据，
     * 再过滤（比如归档）、排序后更新 LiveData。
     * 如果 showArchived 为 false，则过滤掉已归档数据。
     */
    fun loadData(type: DeadlineType, silent: Boolean = false) {
        if (_refreshState.value is RefreshState.Loading) return

        currentType = type
        _refreshState.value = RefreshState.Loading(silent)
        viewModelScope.launch(Dispatchers.IO) {
            val list = filterDataByList(repo.getDDLsByType(type), type)
            when (type) {
                DeadlineType.TASK -> _taskList.value = list
                DeadlineType.HABIT -> _habitList.value = list
            }
            syncSelectedList()

            val map = DeadlineType.entries.associateWith { computeDueSoonCount(it) }
            _dueSoonCounts.value = map

            _refreshState.value = RefreshState.Success

            Log.d("Loading", "I reached here, ${_refreshState.value}")
        }
    }

    fun loadAllData(silent: Boolean = false) {
        if (_refreshState.value is RefreshState.Loading) return
        _refreshState.value = RefreshState.Loading(silent)
        viewModelScope.launch(Dispatchers.IO) {
            _taskList.value = filterDataByList(repo.getDDLsByType(DeadlineType.TASK), DeadlineType.TASK)
            _habitList.value = filterDataByList(repo.getDDLsByType(DeadlineType.HABIT), DeadlineType.HABIT)
            syncSelectedList()

            val map = DeadlineType.entries.associateWith { computeDueSoonCount(it) }
            _dueSoonCounts.value = map

            _refreshState.value = RefreshState.Success
        }
    }

    // 获取所有DDLItem，并根据条件过滤：
    // 1. note或name中必须包含纯文本查询（不区分大小写）
    // 2. 如果提供了时间过滤条件，则要求对应的开始时间或完成时间符合条件
    fun filterData(filter: SearchFilter, type: DeadlineType) {
        viewModelScope.launch(Dispatchers.IO) {
            val base = repo.getDDLsByType(type)                         // IO
            val filtered = withContext(Dispatchers.Default) {           // 纯 CPU
                base.filter { filter.matches(it) }
            }
            when (type) {
                DeadlineType.TASK -> _taskList.value = filtered
                DeadlineType.HABIT -> _habitList.value = filtered
            }
            syncSelectedList()
            val counts = DeadlineType.entries.associateWith { computeDueSoonCount(it) }
            _dueSoonCounts.value = counts
        }
    }

    suspend fun getBaseList(type: DeadlineType): List<DDLItem> =
        withContext(Dispatchers.IO) { repo.getDDLsByType(type) }

    /**
     * 手动下拉刷新专用：
     * - 先显式显示菊花（Loading(silent=false)）
     * - 同步（syncNow）
     * - 直接加载列表 + dueSoon（不再调用 loadData 避免二次 Loading/早退）
     * - 结束后 Success（菊花关闭）
     */
    fun refreshFromPull(type: DeadlineType) {
        viewModelScope.launch {
            _refreshState.value = RefreshState.Loading(silent = false)

            val start = System.currentTimeMillis()

            withContext(Dispatchers.IO) {
                repo.syncNow()

                // 同步完成后直接拉取 + 过滤 + 统计
                val data = filterDataByList(repo.getDDLsByType(type), type)
                val map = DeadlineType.entries.associateWith { computeDueSoonCount(it) }

                when (type) {
                    DeadlineType.TASK -> _taskList.value = data
                    DeadlineType.HABIT -> _habitList.value = data
                }
                syncSelectedList()
                _dueSoonCounts.value = map
            }

            val elapsed = System.currentTimeMillis() - start
            if (elapsed < 500) {
                delay(500 - elapsed)
            }

            _refreshState.value = RefreshState.Success
        }
    }

    fun refreshAllFromPull() {
        viewModelScope.launch {
            _refreshState.value = RefreshState.Loading(silent = false)

            val start = System.currentTimeMillis()

            withContext(Dispatchers.IO) {
                repo.syncNow()

                _taskList.value = filterDataByList(repo.getDDLsByType(DeadlineType.TASK), DeadlineType.TASK)
                _habitList.value = filterDataByList(repo.getDDLsByType(DeadlineType.HABIT), DeadlineType.HABIT)
                syncSelectedList()

                val map = DeadlineType.entries.associateWith { computeDueSoonCount(it) }
                _dueSoonCounts.value = map
            }

            val elapsed = System.currentTimeMillis() - start
            if (elapsed < 500) {
                delay(500 - elapsed)
            }

            _refreshState.value = RefreshState.Success
        }
    }

    sealed class RefreshState {
        object Idle : RefreshState()
        data class Loading(val silent: Boolean) : RefreshState()
        object Success : RefreshState()
    }

    var lastClipboardText: String? = null
}
