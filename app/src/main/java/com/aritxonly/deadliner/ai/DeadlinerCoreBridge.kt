package com.aritxonly.deadliner.ai

import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.domain.ai.model.AIConfig
import com.deadliner.domain.ai.model.CoreEvent
import com.deadliner.domain.ai.model.ToolCall
import com.deadliner.domain.ai.port.ToolResult
import com.deadliner.domain.habit.HabitAlgorithms
import com.deadliner.domain.habit.HabitGoalWindow
import com.deadliner.domain.habit.HabitScheduleKind
import com.deadliner.interfaces.DeadlinerCore
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.TimeZone

internal data class CoreRunOutput(
    val mixed: MixedResult,
    val toolRequests: List<AIToolRequest> = emptyList(),
)

internal sealed class ToolDecision {
    data object Approve : ToolDecision()
    data class Reject(val reason: String? = null) : ToolDecision()
}

/** Android UI event adapter. Lifi orchestration, memory and tool models are owned by KMP. */
internal class DeadlinerCoreBridge(
    private val gson: Gson = Gson(),
    private val toolCallAdapter: ToolCallAdapter = AndroidToolCallAdapter(),
) {
    private val gate = Mutex()

    suspend fun processInput(
        text: String,
        apiKey: String,
        baseUrl: String,
        modelId: String,
        platform: String = "android",
        onToolRequest: (suspend (AIToolRequest) -> ToolDecision)? = null,
        onTextStream: (suspend (String) -> Unit)? = null,
        onThinking: (suspend (agent: String, phase: String, message: String?) -> Unit)? = null,
        onLifecycle: (suspend (AgentLifecycleEvent) -> Unit)? = null,
    ): CoreRunOutput = gate.withLock {
        val runtime = KmpV6Runtime.createLifiCore(
            AIConfig(
                apiKey = apiKey,
                baseUrl = baseUrl,
                model = modelId,
                platform = platform,
                timezone = TimeZone.getDefault().id,
            ),
        )
        consumeRun(
            runtime = runtime,
            text = text,
            onToolRequest = onToolRequest,
            onTextStream = onTextStream,
            onThinking = onThinking,
            onLifecycle = onLifecycle,
        )
    }

    private suspend fun consumeRun(
        runtime: DeadlinerCore,
        text: String,
        onToolRequest: (suspend (AIToolRequest) -> ToolDecision)?,
        onTextStream: (suspend (String) -> Unit)?,
        onThinking: (suspend (agent: String, phase: String, message: String?) -> Unit)?,
        onLifecycle: (suspend (AgentLifecycleEvent) -> Unit)?,
    ): CoreRunOutput = coroutineScope {
        val events = Channel<CoreEvent>(Channel.UNLIMITED)
        val requests = mutableListOf<AIToolRequest>()
        val displayedToolCalls = mutableListOf<AIToolCall>()
        var finish: com.deadliner.domain.ai.model.MixedResult? = null
        var failure: String? = null

        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            runtime.events.collect(events::send)
        }
        val processor = launch { runtime.processInput(text) }

        try {
            while (finish == null && failure == null) {
                when (val event = events.receive()) {
                    is CoreEvent.OnLifecycle -> onLifecycle?.invoke(event.toAndroidLifecycle())
                    is CoreEvent.OnThinking -> onThinking?.invoke(event.agentName, event.phase, event.message)
                    is CoreEvent.OnTextStream -> onTextStream?.invoke(event.chunk)
                    is CoreEvent.OnMemoryCommitted -> onLifecycle?.invoke(
                        AgentLifecycleEvent(
                            stage = "memory_commit",
                            status = "completed",
                            state = AgentLifecycleState.MemorySynced,
                        ),
                    )
                    is CoreEvent.OnToolRequest -> {
                        val request = event.toAndroidRequest()
                        requests += request
                        displayedToolCalls += event.tool.toAndroidToolCall()
                        val decision = onToolRequest?.invoke(request) ?: ToolDecision.Approve
                        val payload = when (decision) {
                            ToolDecision.Approve -> executeApproved(request)
                            is ToolDecision.Reject -> gson.toJson(
                                mapOf(
                                    "denied" to true,
                                    "reason" to (decision.reason ?: "user_denied"),
                                ),
                            )
                        }
                        runtime.submitToolResult(event.id, ToolResult(request.tool, payload))
                    }
                    is CoreEvent.OnFinish -> finish = event.result
                    is CoreEvent.OnError -> failure = event.message
                }
            }
        } finally {
            processor.cancelAndJoin()
            collector.cancelAndJoin()
            events.close()
        }

        failure?.let { error(it) }
        val result = checkNotNull(finish) { "KMP Lifi returned without a result" }
        CoreRunOutput(
            mixed = result.toAndroidMixed(displayedToolCalls),
            toolRequests = requests,
        )
    }

    private suspend fun executeApproved(request: AIToolRequest): String = runCatching {
        toolCallAdapter.execute(
            ToolCallExecution(
                id = request.id,
                toolName = request.tool,
                argsJson = request.rawArgsJson ?: "{}",
            ),
        ).resultJson
    }.getOrElse { error ->
        gson.toJson(
            mapOf(
                "ok" to false,
                "errorCode" to "TOOL_EXECUTION_FAILED",
                "message" to (error.message ?: "tool execution failed"),
            ),
        )
    }

    private fun CoreEvent.OnLifecycle.toAndroidLifecycle(): AgentLifecycleEvent = AgentLifecycleEvent(
        requestId = requestId,
        stage = when (stage) {
            "started" -> "process_input"
            "completed" -> "orchestration"
            "failed" -> "process_input"
            else -> stage
        },
        status = when (status) {
            "running" -> "started"
            "success" -> "completed"
            "error" -> "failed"
            else -> status
        },
        message = message,
        state = when ("${stage.lowercase()}.${status.lowercase()}") {
            "started.running" -> AgentLifecycleState.Running
            "completed.success" -> AgentLifecycleState.Finished
            "failed.error" -> AgentLifecycleState.Failed
            else -> AgentLifecycleState.Unknown
        },
    )

    private fun CoreEvent.OnToolRequest.toAndroidRequest(): AIToolRequest {
        val rawArgs = serializeToolArguments(tool, gson)
        val readArgs = (tool as? ToolCall.ReadTasks)?.let {
            ReadTasksArgs(it.timeRangeDays, it.status, it.keywords, it.limit, it.sort)
        } ?: ReadTasksArgs()
        return AIToolRequest(
            id = id,
            tool = tool.toolName(),
            args = readArgs,
            reason = tool.reason,
            executionMode = executionMode?.name,
            rawArgsJson = rawArgs,
        )
    }

    private fun ToolCall.toAndroidToolCall(): AIToolCall = AIToolCall(
        tool = toolName(),
        args = (this as? ToolCall.ReadTasks)?.let {
            ReadTasksArgs(it.timeRangeDays, it.status, it.keywords, it.limit, it.sort)
        },
        reason = reason,
    )

    private fun ToolCall.toolName(): String = when (this) {
        is ToolCall.ReadTasks -> TOOL_READ_TASKS
        is ToolCall.ReadHabits -> TOOL_READ_HABITS
        is ToolCall.CreateTask -> TOOL_CREATE_TASK
        is ToolCall.CreateHabit -> TOOL_CREATE_HABIT
        is ToolCall.UpdateDeadline -> TOOL_UPDATE_DEADLINE
    }

    private fun com.deadliner.domain.ai.model.MixedResult.toAndroidMixed(
        displayedToolCalls: List<AIToolCall>,
    ): MixedResult = MixedResult(
        primaryIntent = primaryIntent ?: "ExtractTasks",
        tasks = tasks.map { AITask(it.title, it.dueAt.orEmpty(), it.note.orEmpty()) },
        habits = habits.map {
            AIHabit(
                name = it.name,
                period = it.schedule.legacyPeriod(it.goal.window),
                timesPerPeriod = it.goal.targetCount,
                goalType = it.goal.window.legacyGoalType(),
                totalTarget = it.goal.targetCount.takeIf { _ -> it.goal.window == HabitGoalWindow.LIFETIME },
                schedule = it.schedule,
                goal = it.goal,
                tracking = it.tracking,
                reminder = it.reminder,
            )
        },
        retrievedTasks = retrievedTasks.map { AITask(it.title, it.dueAt.orEmpty(), it.note.orEmpty()) },
        retrievedHabits = retrievedHabits.map {
            AIHabit(
                name = it.name,
                period = it.schedule.legacyPeriod(it.goal.window),
                timesPerPeriod = it.goal.targetCount,
                goalType = it.goal.window.legacyGoalType(),
                totalTarget = it.goal.targetCount.takeIf { _ -> it.goal.window == HabitGoalWindow.LIFETIME },
                schedule = it.schedule,
                goal = it.goal,
                tracking = it.tracking,
                reminder = it.reminder,
            )
        },
        newMemories = newMemories,
        chatResponse = chatResponse,
        sessionSummary = sessionSummary,
        userProfile = userProfile,
        toolCalls = displayedToolCalls,
    )

    companion object {
        const val TOOL_READ_TASKS = "read_tasks"
        const val TOOL_READ_HABITS = "read_habits"
        const val TOOL_CREATE_TASK = "create_task"
        const val TOOL_CREATE_HABIT = "create_habit"
        const val TOOL_UPDATE_DEADLINE = "update_deadline"
        const val TOOL_ADD_TO_CALENDAR = "addToCalendar"
    }
}

internal fun serializeToolArguments(tool: ToolCall, gson: Gson = Gson()): String = gson.toJson(
    when (tool) {
        is ToolCall.ReadTasks -> mapOf(
            "timeRangeDays" to tool.timeRangeDays,
            "status" to tool.status,
            "keywords" to tool.keywords,
            "limit" to tool.limit,
            "sort" to tool.sort,
        )
        is ToolCall.CreateTask -> mapOf("name" to tool.name, "dueTime" to tool.dueTime, "note" to tool.note)
        is ToolCall.UpdateDeadline -> mapOf("taskId" to tool.taskId, "newDueTime" to tool.newDueTime)
        is ToolCall.ReadHabits -> mapOf("keywords" to tool.keywords)
        is ToolCall.CreateHabit -> mapOf(
            "name" to tool.name,
            "schedule" to tool.schedule,
            "goal" to tool.goal,
            "tracking" to tool.tracking,
            "reminder" to tool.reminder,
            "compatibilityWarning" to tool.compatibilityWarning,
        )
    },
)

private fun com.deadliner.domain.habit.HabitSchedule.legacyPeriod(window: HabitGoalWindow): String = when {
    kind == HabitScheduleKind.ONCE -> "ONCE"
    algorithmKey == HabitAlgorithms.EBBINGHAUS_CLASSIC_V1 -> "EBBINGHAUS"
    window == HabitGoalWindow.WEEK -> "WEEKLY"
    window == HabitGoalWindow.MONTH -> "MONTHLY"
    else -> "DAILY"
}

private fun HabitGoalWindow.legacyGoalType(): String =
    if (this == HabitGoalWindow.LIFETIME) "TOTAL" else "PER_PERIOD"
