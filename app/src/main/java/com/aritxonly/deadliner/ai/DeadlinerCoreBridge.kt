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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import java.util.UUID
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
    data class ApproveWithArguments(val argumentsJson: String) : ToolDecision()
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
        onToolExecuted: (suspend (AIToolRequest, String) -> Unit)? = null,
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
            onToolExecuted = onToolExecuted,
        )
    }

    private suspend fun consumeRun(
        runtime: DeadlinerCore,
        text: String,
        onToolRequest: (suspend (AIToolRequest) -> ToolDecision)?,
        onTextStream: (suspend (String) -> Unit)?,
        onThinking: (suspend (agent: String, phase: String, message: String?) -> Unit)?,
        onLifecycle: (suspend (AgentLifecycleEvent) -> Unit)?,
        onToolExecuted: (suspend (AIToolRequest, String) -> Unit)?,
    ): CoreRunOutput = coroutineScope {
        val consumerId = "android-" + UUID.randomUUID().toString()
        var cursor = runtime.snapshotV2().lastSeq
        val runId = runtime.startRunV2(text)
        val requests = mutableListOf<AIToolRequest>()
        val displayedToolCalls = mutableListOf<AIToolCall>()
        var finish: com.deadliner.domain.ai.model.MixedResult? = null
        var failure: String? = null

        try {
            while (finish == null && failure == null) {
                check(KmpV6Runtime.isCurrentLifiCore(runtime)) { "模型配置已切换，请重新发起请求。" }
                val batch = runtime.readEventsV2(consumerId, cursor)
                check(batch.protocolVersion == 2) { "UNSUPPORTED_PROTOCOL" }
                LifiV2Session.update(runtime.snapshotV2())
                val records = if (batch.resyncRequired) {
                    val snapshot = runtime.snapshotV2()
                    listOfNotNull(snapshot.terminal, snapshot.pending?.let { pending ->
                        com.deadliner.domain.ai.agent.AgentV2Event(sessionId = snapshot.sessionId, runId = snapshot.runId,
                            seq = snapshot.lastSeq, eventId = "snapshot", type = "ApprovalRequested", timestamp = "",
                            payload = kotlinx.serialization.json.JsonObject(emptyMap()), compatibilityEvent = pending)
                    })
                } else batch.events
                for (record in records) {
                    if (record.runId != runId) continue
                    if (!batch.resyncRequired && record.seq != cursor + 1) error("EVENT_GAP")
                    cursor = record.seq
                    val event = record.compatibilityEvent ?: continue
                    when (event) {
                    is CoreEvent.OnLifecycle -> onLifecycle?.invoke(event.toAndroidLifecycle())
                    is CoreEvent.OnThinking -> onThinking?.invoke(event.agentName, event.phase, event.message ?: if (event.phase == "retrying") "网络暂时不可用，正在重试…" else null)
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
                        val decision = withTimeout(300_000) {
                            onToolRequest?.invoke(request) ?: if (request.tool.startsWith("read_"))
                                ToolDecision.Approve else ToolDecision.Reject("confirmation_unavailable")
                        }
                        check(KmpV6Runtime.isCurrentLifiCore(runtime)) { "模型配置已切换，原确认已失效。" }
                        val payload = when (decision) {
                            ToolDecision.Approve -> executeApproved(request)
                            is ToolDecision.ApproveWithArguments -> executeApproved(request.copy(rawArgsJson = decision.argumentsJson))
                            is ToolDecision.Reject -> gson.toJson(
                                mapOf(
                                    "denied" to true,
                                    "reason" to (decision.reason ?: "user_denied"),
                                ),
                            )
                        }
                        onToolExecuted?.invoke(request, payload)
                        check(runtime.submitToolResultV2(event.id, ToolResult(request.tool, payload))) { "STALE_ACTION" }
                    }
                    is CoreEvent.OnFinish -> if (record.type == "RunTerminated") finish = event.result
                    is CoreEvent.OnError -> if (record.type == "RunTerminated") failure = event.message
                    }
                }
                check(runtime.ackEventsV2(consumerId, batch.batchId, batch.lastSeq)) { "ACK_REJECTED" }
                cursor = maxOf(cursor, batch.lastSeq)
                if (finish == null && failure == null) delay(50)
            }
        } finally {
            withContext(NonCancellable) {
                val snapshot = runtime.snapshotV2()
                if (snapshot.runId == runId && snapshot.terminal == null) runtime.cancelRun()
                runtime.detachV2(consumerId)
                LifiV2Session.update(runtime.snapshotV2())
            }
        }

        failure?.let { error(com.deadliner.domain.ai.agent.AgentErrorMessages.describe(it)) }
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
        if (error is CancellationException) throw error
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
        val rawArgs = serializeToolArguments(tool)
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

internal fun serializeToolArguments(tool: ToolCall): String =
    com.deadliner.domain.ai.agent.ToolArgumentCodec.encode(tool)

private fun com.deadliner.domain.habit.HabitSchedule.legacyPeriod(window: HabitGoalWindow): String = when {
    kind == HabitScheduleKind.ONCE -> "ONCE"
    algorithmKey == HabitAlgorithms.EBBINGHAUS_CLASSIC_V1 -> "EBBINGHAUS"
    window == HabitGoalWindow.WEEK -> "WEEKLY"
    window == HabitGoalWindow.MONTH -> "MONTHLY"
    else -> "DAILY"
}

private fun HabitGoalWindow.legacyGoalType(): String =
    if (this == HabitGoalWindow.LIFETIME) "TOTAL" else "PER_PERIOD"
