package com.aritxonly.deadliner.ai

enum class IntentType { ExtractTasks, PlanDay, SplitToSteps }

fun String.toIntentTypeOrDefault(default: IntentType = IntentType.ExtractTasks): IntentType =
    when (this) {
        "PlanDay" -> IntentType.PlanDay
        "SplitToSteps" -> IntentType.SplitToSteps
        "ExtractTasks" -> IntentType.ExtractTasks
        else -> default
    }

data class IntentGuess(
    val intent: IntentType,
    val confidence: Double,   // 0.0 ~ 1.0
    val reason: String        // 调试/打点
)

data class UserProfile(
    val preferredLang: String?,         // 例: "zh-CN"；为空则用设备语言
    val defaultEveningHour: Int = 20,   // “晚上”映射
    val defaultReminderMinutes: List<Int> = listOf(30), // 默认提醒
    val defaultWorkdayStart: String? = null,  // "09:00"
    val defaultWorkdayEnd: String? = null     // "18:00"
)

data class AITask(
    val name: String,
    val dueTime: String,          // "yyyy-MM-dd HH:mm"
    val note: String = ""
)

data class ExtractTasksResult(
    val tasks: List<AITask> = emptyList(),
    val timezone: String = "",
    val resolvedAt: String = ""
)

data class PlanBlock(
    val title: String,
    val start: String,     // "yyyy-MM-dd HH:mm"
    val end: String,       // "
    val location: String? = null,
    val energy: String? = null,   // low/med/high
    val linkTask: String? = null
)

data class SplitStepsResult(
    val title: String,
    val checklist: List<String>
)

sealed class AIResult {
    data class ExtractTasks(val data: ExtractTasksResult): AIResult()
    data class PlanDay(val blocks: List<PlanBlock>): AIResult()
    data class SplitToSteps(val data: SplitStepsResult): AIResult()
}

data class MixedResult(
    val primaryIntent: String = "ExtractTasks", // "ExtractTasks" | "PlanDay" | "SplitToSteps" | "Chat"
    val tasks: List<AITask> = emptyList(),
    val habits: List<AIHabit> = emptyList(),
    val retrievedTasks: List<AITask> = emptyList(),
    val retrievedHabits: List<AIHabit> = emptyList(),
    val newMemories: List<String> = emptyList(),
    val chatResponse: String? = null,
    val sessionSummary: String? = null,
    val userProfile: String? = null,
    val toolCalls: List<AIToolCall> = emptyList(),
    val planBlocks: List<PlanBlock> = emptyList(),
    val steps: List<SplitStepsResult> = emptyList()
)

data class AIHabit(
    val name: String,
    val period: String,
    val timesPerPeriod: Int,
    val goalType: String = "",
    val totalTarget: Int? = null
)

data class ReadTasksArgs(
    val timeRangeDays: Int? = null,
    val status: String? = null,       // OPEN | DONE | ALL
    val keywords: List<String>? = null,
    val limit: Int? = null,
    val sort: String? = null          // DUE_ASC | UPDATED_DESC
)

data class AddToCalendarArgs(
    val title: String? = null,
    val start: String? = null,
    val end: String? = null,
    val location: String? = null,
    val description: String? = null
)

data class AIToolCall(
    val tool: String,
    val args: ReadTasksArgs? = null,
    val addToCalendarArgs: AddToCalendarArgs? = null,
    val reason: String? = null
)

data class AIToolRequest(
    val id: String,
    val tool: String,
    val args: ReadTasksArgs = ReadTasksArgs(),
    val addToCalendarArgs: AddToCalendarArgs? = null,
    val reason: String? = null,
    val executionMode: String? = null,
    val retryCount: Long? = null,
    val retryFromErrorCode: String? = null,
    val rawArgsJson: String? = null,
)

enum class AgentLifecycleState {
    Running,
    WaitingTool,
    Finished,
    MemorySynced,
    Failed,
    Unknown,
}

data class AgentLifecycleEvent(
    val requestId: String? = null,
    val stage: String,
    val status: String,
    val message: String? = null,
    val state: AgentLifecycleState = AgentLifecycleState.Unknown,
)

data class TaskDigestItem(
    val id: Long,
    val name: String,
    val due: String,
    val status: String,
    val notePreview: String
)

data class TaskSummary(
    val count: Int,
    val overdue: Int,
    val dueSoon24h: Int
)

data class ReadTasksResultPayload(
    val tasks: List<TaskDigestItem>,
    val summary: TaskSummary
)

data class AIToolResult(
    val id: String,
    val tool: String,
    val appliedArgs: ReadTasksArgs,
    val payload: ReadTasksResultPayload,
    val generatedAtEpochMs: Long = System.currentTimeMillis()
)
