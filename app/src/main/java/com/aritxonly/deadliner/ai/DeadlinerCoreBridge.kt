package com.aritxonly.deadliner.ai

import android.content.Context
import android.util.Log
import android.os.SystemClock
import com.aritxonly.deadliner.lifi.CoreCallback
import com.aritxonly.deadliner.lifi.CoreEvent
import com.aritxonly.deadliner.lifi.DeadlinerCore
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

internal data class CoreRunOutput(
    val mixed: MixedResult,
    val memorySyncJson: String? = null,
    val toolRequests: List<AIToolRequest> = emptyList(),
)

internal sealed class ToolDecision {
    data object Approve : ToolDecision()
    data class Reject(val reason: String? = null) : ToolDecision()
}

internal class DeadlinerCoreBridge(
    private val appContext: Context,
    private val gson: Gson = Gson(),
    private val toolCallAdapter: ToolCallAdapter = AndroidToolCallAdapter(gson),
) {
    private val gate = Mutex()
    private var core: DeadlinerCore? = null

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
    ): CoreRunOutput {
        val lockWaitStart = SystemClock.elapsedRealtime()
        return gate.withLock {
        val lockAcquiredAt = SystemClock.elapsedRealtime()
        val lockWaitMs = lockAcquiredAt - lockWaitStart
        Log.d("DeadlinerCoreBridge", "processInput start: text=${text.take(120)}")
        val startMs = SystemClock.elapsedRealtime()
        Log.d("DeadlinerCoreBridge", "timing: lock_wait=${lockWaitMs}ms")
        onLifecycle?.invoke(
            AgentLifecycleEvent(
                stage = "process_input",
                status = "started",
                state = AgentLifecycleState.Running,
            )
        )
        val ensureStartMs = SystemClock.elapsedRealtime()
        val runtime = ensureCore(apiKey = apiKey, baseUrl = baseUrl, modelId = modelId, platform = platform)
        Log.d("DeadlinerCoreBridge", "timing: ensure_core=${SystemClock.elapsedRealtime() - ensureStartMs}ms")

        val events = Channel<CoreEvent>(capacity = Channel.UNLIMITED)
        val toolRequests = mutableListOf<AIToolRequest>()
        val toolCalls = mutableListOf<AIToolCall>()
        val committedMemories = mutableListOf<String>()

        var lastFinish: CoreEvent.OnFinish? = null
        var lastError: String? = null
        var sawMemoryCommitted = false

        runtime.setCallback(object : CoreCallback {
            override fun onEvent(event: CoreEvent) {
                events.trySend(event)
            }
        })

        coroutineScope {
            val processJob = async(Dispatchers.IO) { runtime.processInput(text) }
            var firstEventAtMs: Long? = null

            while (true) {
                // Avoid long tail latency: when core has already finished but no more events arrive,
                // a long receive timeout can make UI appear "stuck" (especially in release builds).
                if (processJob.isCompleted && (lastFinish != null || lastError != null)) {
                    break
                }

                val event = withTimeoutOrNull(350) { events.receive() }
                if (event == null) {
                    continue
                }
                if (firstEventAtMs == null) {
                    firstEventAtMs = SystemClock.elapsedRealtime()
                    Log.d("DeadlinerCoreBridge", "timing: first_event=${firstEventAtMs - startMs}ms")
                }

                when (event) {
                    is CoreEvent.OnThinking -> {
                        Log.d("DeadlinerCoreBridge", "event OnThinking(agent=${event.agentName}, phase=${event.phase})")
                        onThinking?.invoke(event.agentName, event.phase, event.message)
                    }
                    is CoreEvent.OnTextStream -> {
                        Log.d("DeadlinerCoreBridge", "event OnTextStream(len=${event.chunk.length})")
                        onTextStream?.invoke(event.chunk)
                    }
                    is CoreEvent.OnFinish -> {
                        Log.d("DeadlinerCoreBridge", "event OnFinish(intent=${event.primaryIntent})")
                        Log.d("DeadlinerCoreBridge", "timing: finish_event_at=${SystemClock.elapsedRealtime() - startMs}ms")
                        lastFinish = event
                    }
                    is CoreEvent.OnError -> {
                        Log.e("DeadlinerCoreBridge", "event OnError(message=${event.message})")
                        lastError = event.message
                    }
                    is CoreEvent.OnMemoryCommitted -> {
                        Log.d("DeadlinerCoreBridge", "event OnMemoryCommitted(count=${event.addedMemories.size})")
                        sawMemoryCommitted = true
                        committedMemories += event.addedMemories
                        onLifecycle?.invoke(
                            AgentLifecycleEvent(
                                stage = "memory_commit",
                                status = "completed",
                                state = AgentLifecycleState.MemorySynced,
                            )
                        )
                    }
                    is CoreEvent.OnToolRequest -> {
                        Log.d("DeadlinerCoreBridge", "event OnToolRequest(id=${event.id}, tool=${event.toolName})")
                        onLifecycle?.invoke(
                            AgentLifecycleEvent(
                                requestId = event.id,
                                stage = "tool_request",
                                status = "waiting",
                                state = AgentLifecycleState.WaitingTool,
                            )
                        )
                        val request = parseToolRequest(event)
                        toolRequests += request
                        toolCalls += AIToolCall(
                            tool = request.tool,
                            args = request.args,
                            addToCalendarArgs = request.addToCalendarArgs,
                            reason = request.reason,
                        )

                        val decision = onToolRequest?.invoke(request) ?: ToolDecision.Approve
                        val toolPayload = when (decision) {
                            is ToolDecision.Approve -> {
                                onLifecycle?.invoke(
                                    AgentLifecycleEvent(
                                        requestId = request.id,
                                        stage = "submit_tool_result",
                                        status = "started",
                                        state = AgentLifecycleState.Running,
                                    )
                                )
                                runCatching {
                                    toolCallAdapter.execute(
                                        ToolCallExecution(
                                            id = request.id,
                                            toolName = normalizeToolName(request.tool),
                                            argsJson = request.rawArgsJson ?: "{}",
                                        )
                                    ).resultJson
                                }.getOrElse { e ->
                                    gson.toJson(
                                        mapOf(
                                            "ok" to false,
                                            "errorCode" to "TOOL_EXECUTION_FAILED",
                                            "message" to (e.message ?: "tool execution failed"),
                                        )
                                    )
                                }
                            }
                            is ToolDecision.Reject -> gson.toJson(
                                mapOf(
                                    "denied" to true,
                                    "reason" to (decision.reason ?: "user_denied"),
                                )
                            )
                        }
                        runtime.submitToolResult(request.id, toolPayload)
                    }
                    else -> {
                        // Keep consuming lifecycle events without requiring generated binding updates.
                        if (tryHandleLifecycleEvent(event, onLifecycle)) {
                            continue
                        }
                    }
                }

                if (lastError != null) {
                    break
                }
                if (lastFinish != null && processJob.isCompleted) {
                    break
                }
            }

            processJob.await()
        }

        val err = lastError
        if (!err.isNullOrBlank()) {
            Log.e("DeadlinerCoreBridge", "processInput failed: $err")
            onLifecycle?.invoke(
                AgentLifecycleEvent(
                    stage = "process_input",
                    status = "failed",
                    message = err,
                    state = AgentLifecycleState.Failed,
                )
            )
            error(err)
        }

        val finish = lastFinish ?: error("core returned without finish event")
        Log.d("DeadlinerCoreBridge", "processInput finished successfully")
        Log.d("DeadlinerCoreBridge", "timing: total=${SystemClock.elapsedRealtime() - startMs}ms")
        onLifecycle?.invoke(
            AgentLifecycleEvent(
                stage = "orchestration",
                status = "completed",
                state = AgentLifecycleState.Finished,
            )
        )

        val mergedMemories = (finish.memorySyncJson?.takeIf { it.isNotBlank() } ?: "")
        val memorySyncJson = when {
            mergedMemories.isNotBlank() -> mergedMemories
            sawMemoryCommitted -> runtime.getLastMemorySyncJson()
            else -> null
        }

        CoreRunOutput(
            mixed = mapFinishToMixed(finish, runtime, committedMemories, toolCalls),
            memorySyncJson = memorySyncJson,
            toolRequests = toolRequests,
        )
    }}

    private fun parseToolRequest(event: CoreEvent.OnToolRequest): AIToolRequest {
        val obj = runCatching { gson.fromJson(event.argsJson, JsonObject::class.java) }.getOrNull()
        val normalizedTool = normalizeToolName(event.toolName)
        val reason = obj?.get("reason")?.asString
        val meta = obj?.getAsJsonObject("_meta")
        val executionMode = meta?.get("executionMode")?.asString
            ?: meta?.get("execution_mode")?.asString
        val retryCount = runCatching {
            meta?.get("retryCount")?.asLong ?: meta?.get("retry_count")?.asLong
        }.getOrNull()
        val retryFromErrorCode = meta?.get("retryFromErrorCode")?.asString
            ?: meta?.get("retry_from_error_code")?.asString

        return when (normalizedTool) {
            TOOL_READ_TASKS -> {
                val args = parseReadTasksArgs(event.argsJson, obj)
                    ?: ReadTasksArgs()
                AIToolRequest(
                    id = event.id,
                    tool = normalizedTool,
                    args = normalizeReadTasksArgs(args),
                    reason = reason,
                    executionMode = executionMode,
                    retryCount = retryCount,
                    retryFromErrorCode = retryFromErrorCode,
                    rawArgsJson = event.argsJson,
                )
            }

            TOOL_ADD_TO_CALENDAR -> {
                val args = parseAddToCalendarArgs(event.argsJson, obj)
                AIToolRequest(
                    id = event.id,
                    tool = normalizedTool,
                    args = ReadTasksArgs(),
                    addToCalendarArgs = args,
                    reason = reason,
                    executionMode = executionMode,
                    retryCount = retryCount,
                    retryFromErrorCode = retryFromErrorCode,
                    rawArgsJson = event.argsJson,
                )
            }
            TOOL_CREATE_TASK,
            TOOL_CREATE_HABIT,
            TOOL_READ_HABITS -> {
                AIToolRequest(
                    id = event.id,
                    tool = normalizedTool,
                    args = ReadTasksArgs(),
                    reason = reason,
                    executionMode = executionMode,
                    retryCount = retryCount,
                    retryFromErrorCode = retryFromErrorCode,
                    rawArgsJson = event.argsJson,
                )
            }

            else -> {
                Log.w("DeadlinerCoreBridge", "unsupported tool from core: raw=${event.toolName}, normalized=$normalizedTool")
                AIToolRequest(
                    id = event.id,
                    tool = normalizedTool,
                    args = ReadTasksArgs(),
                    reason = reason,
                    executionMode = executionMode,
                    retryCount = retryCount,
                    retryFromErrorCode = retryFromErrorCode,
                    rawArgsJson = event.argsJson,
                )
            }
        }
    }

    private fun parseReadTasksArgs(argsJson: String, obj: JsonObject?): ReadTasksArgs? {
        return runCatching { gson.fromJson(argsJson, ReadTasksArgs::class.java) }.getOrNull()
            ?: runCatching {
                if (obj?.has("args") == true && obj.get("args").isJsonObject) {
                    gson.fromJson(obj.getAsJsonObject("args"), ReadTasksArgs::class.java)
                } else null
            }.getOrNull()
    }

    private fun parseAddToCalendarArgs(argsJson: String, obj: JsonObject?): AddToCalendarArgs? {
        return runCatching { gson.fromJson(argsJson, AddToCalendarArgs::class.java) }.getOrNull()
            ?: runCatching {
                if (obj?.has("args") == true && obj.get("args").isJsonObject) {
                    gson.fromJson(obj.getAsJsonObject("args"), AddToCalendarArgs::class.java)
                } else null
            }.getOrNull()
    }

    private fun normalizeToolName(raw: String): String {
        val key = raw.trim().replace("-", "").replace("_", "").lowercase()
        return when (key) {
            "readtasks", "readtask", "taskread", "tasksread", "listtasks", "gettasks" -> TOOL_READ_TASKS
            "readhabits", "readhabit", "listhabits", "gethabits" -> TOOL_READ_HABITS
            "createtask", "addtask", "newtask", "inserttask" -> TOOL_CREATE_TASK
            "createhabit", "addhabit", "newhabit", "inserthabit" -> TOOL_CREATE_HABIT
            "addtocalendar", "calendaradd", "createcalendar", "createevent", "addevent" -> TOOL_ADD_TO_CALENDAR
            else -> raw
        }
    }

    private suspend fun tryHandleLifecycleEvent(
        event: CoreEvent,
        onLifecycle: (suspend (AgentLifecycleEvent) -> Unit)?
    ): Boolean {
        if (onLifecycle == null) return false
        val simple = event::class.java.simpleName
        if (simple != "OnLifecycle") return false
        val requestId = readStringField(event, "requestId", "request_id")
        val stage = readStringField(event, "stage") ?: return false
        val status = readStringField(event, "status") ?: return false
        val message = readStringField(event, "message")
        val state = toLifecycleState(stage, status)
        onLifecycle(
            AgentLifecycleEvent(
                requestId = requestId,
                stage = stage,
                status = status,
                message = message,
                state = state,
            )
        )
        Log.d("DeadlinerCoreBridge", "event OnLifecycle(requestId=$requestId, stage=$stage, status=$status)")
        return true
    }

    private fun readStringField(target: Any, vararg names: String): String? {
        names.forEach { n ->
            runCatching {
                val m = target::class.java.methods.firstOrNull { it.parameterCount == 0 && it.name.equals("get${n.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}", ignoreCase = true) }
                val v = m?.invoke(target) as? String
                if (!v.isNullOrBlank()) return v
            }
            runCatching {
                val f = target::class.java.declaredFields.firstOrNull { it.name.equals(n, ignoreCase = true) } ?: return@runCatching
                f.isAccessible = true
                val v = f.get(target) as? String
                if (!v.isNullOrBlank()) return v
            }
        }
        return null
    }

    private fun toLifecycleState(stage: String, status: String): AgentLifecycleState {
        val key = "${stage.lowercase()}.${status.lowercase()}"
        return when (key) {
            "process_input.started" -> AgentLifecycleState.Running
            "tool_request.waiting" -> AgentLifecycleState.WaitingTool
            "orchestration.completed" -> AgentLifecycleState.Finished
            "memory_commit.completed" -> AgentLifecycleState.MemorySynced
            "process_input.failed", "submit_tool_result.failed", "memory_commit.failed" -> AgentLifecycleState.Failed
            else -> AgentLifecycleState.Unknown
        }
    }

    private fun mapFinishToMixed(
        finish: CoreEvent.OnFinish,
        runtime: DeadlinerCore,
        committedMemories: List<String>,
        toolCalls: List<AIToolCall>,
    ): MixedResult {
        return MixedResult(
            primaryIntent = finish.primaryIntent,
            tasks = finish.tasks.orEmpty().map {
                AITask(
                    name = it.name,
                    dueTime = it.dueTime.orEmpty(),
                    note = it.note.orEmpty(),
                )
            },
            habits = finish.habits.orEmpty().map {
                AIHabit(
                    name = it.name,
                    period = it.period,
                    timesPerPeriod = it.timesPerPeriod,
                    goalType = it.goalType,
                    totalTarget = it.totalTarget,
                )
            },
            retrievedTasks = finish.retrievedTasks.orEmpty().map {
                AITask(it.name, it.dueTime.orEmpty(), it.note.orEmpty())
            },
            retrievedHabits = finish.retrievedHabits.orEmpty().map {
                AIHabit(it.name, it.period, it.timesPerPeriod, it.goalType, it.totalTarget)
            },
            newMemories = committedMemories.distinct(),
            chatResponse = finish.chatResponse,
            sessionSummary = finish.sessionSummary,
            userProfile = runCatching { runtime.getUserProfile() }.getOrNull(),
            toolCalls = toolCalls,
            planBlocks = emptyList(),
            steps = emptyList(),
        )
    }

    private fun ensureCore(
        apiKey: String,
        baseUrl: String,
        modelId: String,
        platform: String,
    ): DeadlinerCore {
        val storagePath = "${appContext.filesDir.absolutePath}/deadliner_core"
        val existing = core
        if (existing != null) {
            Log.d("DeadlinerCoreBridge", "reuse core instance")
            return existing
        }

        Log.d("DeadlinerCoreBridge", "create core instance: baseUrl=$baseUrl model=$modelId storagePath=$storagePath")
        return DeadlinerCore(
            apiKey = apiKey,
            baseUrl = baseUrl,
            modelId = modelId,
            storagePath = storagePath,
            platform = platform,
        ).also { core = it }
    }

    private fun normalizeReadTasksArgs(args: ReadTasksArgs): ReadTasksArgs {
        val limit = (args.limit ?: DEFAULT_LIMIT).coerceIn(1, MAX_LIMIT)
        val days = (args.timeRangeDays ?: DEFAULT_RANGE_DAYS).coerceIn(0, 365)
        val status = (args.status ?: "OPEN").uppercase().let {
            if (it in setOf("OPEN", "DONE", "ALL")) it else "OPEN"
        }
        val sort = (args.sort ?: "DUE_ASC").uppercase().let {
            if (it in setOf("DUE_ASC", "UPDATED_DESC")) it else "DUE_ASC"
        }
        val keywords = args.keywords.orEmpty().map { it.trim() }.filter { it.isNotEmpty() }.take(3)

        return ReadTasksArgs(
            timeRangeDays = days,
            status = status,
            keywords = keywords,
            limit = limit,
            sort = sort,
        )
    }

    companion object {
        const val TOOL_READ_TASKS = "read_tasks"
        const val TOOL_READ_HABITS = "read_habits"
        const val TOOL_CREATE_TASK = "create_task"
        const val TOOL_CREATE_HABIT = "create_habit"
        const val TOOL_ADD_TO_CALENDAR = "addToCalendar"

        private const val DEFAULT_RANGE_DAYS = 7
        private const val DEFAULT_LIMIT = 20
        private const val MAX_LIMIT = 50
    }
}
