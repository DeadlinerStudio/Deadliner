package com.aritxonly.deadliner.ui.lifi

import android.content.Context
import com.aritxonly.deadliner.logging.AppLog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aritxonly.deadliner.ai.AIUtils
import com.aritxonly.deadliner.ai.AIToolRequest
import com.aritxonly.deadliner.ai.ToolDecision
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Independent page session. The application scope outlives navigation, never retaining an Activity. */
internal object LifiSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val persistenceGate = Mutex()
    private val gson = Gson()
    private const val STORAGE_KEY = "android_lifi_page_v1"
    val messages = mutableStateListOf<LifiMessage>()
    val history = mutableStateListOf<LifiHistory>()
    var draft by mutableStateOf("")
    var running by mutableStateOf(false); private set
    var ready by mutableStateOf(false); private set
    var storageError by mutableStateOf(""); private set
    val busy: Boolean get() = running
    var viewing by mutableStateOf<String?>(null); private set
    private var initialized = false
    private var job: Job? = null
    private var lastPrompt = ""

    fun initialize() {
        if (initialized) return
        initialized = true
        scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                runCatching { KmpV6Runtime.database.memory.readSession(STORAGE_KEY)?.let { gson.fromJson(it, LifiSavedState::class.java) } }
            }
            if (loaded.isFailure) {
                storageError = "对话记录读取失败，请重试"
                initialized = false
                return@launch
            }
            val saved = loaded.getOrNull()
            storageError = ""
            if (saved != null) {
                messages.addAll(saved.messages.map { it.restored() })
                history.addAll(saved.history.map { item -> item.copy(messages = item.messages.map { it.restored() }) })
                draft = saved.draft
                lastPrompt = messages.lastOrNull { it.kind == LifiKind.USER }?.text.orEmpty()
            }
            ready = true
        }
    }
    fun editDraft(value: String) { draft = value; persist() }
    fun view(id: String?) { viewing = id }
    fun deleteHistory(id: String) { history.removeAll { it.id == id }; if (viewing == id) viewing = null; persist() }
    fun newConversation() {
        if (running || !ready) return
        if (messages.isNotEmpty()) history.add(0, LifiHistory(title = messages.firstOrNull { it.kind == LifiKind.USER }?.text?.take(36) ?: "新的对话", messages = messages.toList()))
        while (history.size > 40) history.removeAt(history.lastIndex)
        messages.clear(); viewing = null; draft = ""; lastPrompt = ""
        AIUtils.newSession()
        persist()
    }
    fun submit(context: Context, retry: Boolean = false) {
        if (running || !ready || viewing != null) return
        val prompt = (if (retry) lastPrompt else draft).trim()
        if (prompt.isEmpty()) return
        val appContext = context.applicationContext
        draft = ""; lastPrompt = prompt; running = true
        messages.add(LifiMessage(kind = LifiKind.USER, text = prompt))
        val process = LifiMessage(kind = LifiKind.PROCESS, text = "正在理解你的想法", state = LifiActionState.EXECUTING)
        val stream = LifiMessage(kind = LifiKind.TEXT)
        messages.add(process); messages.add(stream)
        val requested = mutableSetOf<String>()
        job = scope.launch {
            try {
                val (_, json) = AIUtils.generateAutoInteractive(
                    context = appContext, rawText = prompt,
                    onTextStream = { chunk -> update(stream.id) { it.copy(text = it.text + chunk) } },
                    onThinking = { _, phase, text -> update(process.id) { it.copy(text = "正在处理", detail = (it.detail.lines().takeLast(11) + (text?.takeIf(String::isNotBlank) ?: phase)).joinToString("\n")) } },
                    onToolRequest = { request ->
                        requested.add(request.tool)
                        ToolDecision.Approve
                    },
                    onToolExecuted = { request, payload ->
                        val status = lifiReceiptState(payload)
                        AppLog.i("lifi.tool", "tool=${request.tool} actionId=${request.id} status=$status")
                        recordExecution(request.id, request.tool, request.rawArgsJson ?: "{}", payload)
                    },
                )
                val result = AIUtils.parseMixedResult(json)
                update(stream.id) { it.copy(text = result.chatResponse?.takeIf(String::isNotBlank) ?: it.text) }
                if ("create_task" !in requested) result.tasks.forEach { task ->
                    executeSuggestion(LifiMessage(kind = LifiKind.ACTION, tool = "create_task", arguments = gson.toJson(mapOf("name" to task.name, "dueTime" to task.dueTime, "note" to task.note)), proposal = true))
                }
                if ("create_habit" !in requested) result.habits.forEach { habit ->
                    executeSuggestion(LifiMessage(kind = LifiKind.ACTION, tool = "create_habit", arguments = habit.schedule?.let { schedule -> com.aritxonly.deadliner.ai.serializeToolArguments(com.deadliner.domain.ai.model.ToolCall.CreateHabit(name = habit.name, schedule = schedule, goal = habit.goal ?: com.deadliner.domain.habit.HabitGoal(), tracking = habit.tracking ?: com.deadliner.domain.habit.HabitTracking(), reminder = habit.reminder)) } ?: gson.toJson(mapOf("name" to habit.name, "period" to habit.period, "timesPerPeriod" to habit.timesPerPeriod, "goalType" to habit.goalType, "totalTarget" to habit.totalTarget)), proposal = true))
                }
                if ("read_tasks" !in requested) result.retrievedTasks.forEach { messages.add(LifiMessage(kind = LifiKind.RESULT, text = it.name, detail = listOf(it.dueTime, it.note).filter(String::isNotBlank).joinToString("\n"))) }
                if ("read_habits" !in requested) result.retrievedHabits.forEach { messages.add(LifiMessage(kind = LifiKind.RESULT, text = it.name, detail = "${it.period} · ${it.timesPerPeriod} 次")) }
                result.planBlocks.forEach { messages.add(LifiMessage(kind = LifiKind.RESULT, text = it.title, detail = "${it.start} — ${it.end}")) }
                result.steps.forEach { messages.add(LifiMessage(kind = LifiKind.RESULT, text = it.title, detail = it.checklist.mapIndexed { i, step -> "${i + 1}. $step" }.joinToString("\n"))) }
                update(process.id) { it.copy(text = "处理完成", state = LifiActionState.SUCCEEDED) }
                if (messages.lastOrNull { it.id == stream.id }?.text.isNullOrBlank()) messages.removeAll { it.id == stream.id }
            } catch (error: CancellationException) {
                update(process.id) { it.copy(text = "已停止", state = LifiActionState.EXPIRED) }
                throw error
            } catch (error: Exception) {
                AppLog.w("lifi.run", "failed type=${error.javaClass.simpleName}")
                update(process.id) { it.copy(text = "处理未完成", state = LifiActionState.FAILED) }
                messages.add(LifiMessage(kind = LifiKind.ERROR, text = error.message ?: "连接失败，请重试"))
            } finally {
                messages.replaceAll { if (it.kind == LifiKind.ACTION && it.state in listOf(LifiActionState.PENDING, LifiActionState.EXECUTING) && !it.proposal) it.copy(state = LifiActionState.EXPIRED) else it }
                running = false; job = null; persist()
            }
        }
        persist()
    }
    private suspend fun executeSuggestion(message: LifiMessage) {
        currentCoroutineContext().ensureActive()
        // Once a database transaction begins, publish its durable receipt even if Stop is
        // tapped meanwhile. Cancellation prevents the next suggestion from executing.
        withContext(NonCancellable) {
            val payload = withContext(Dispatchers.IO) {
                KmpV6Runtime.database.executeLifiTool(message.id, message.tool, message.arguments)
            }
            recordExecution(message.id, message.tool, message.arguments, payload)
            persist()
        }
    }

    private fun recordExecution(id: String, tool: String, arguments: String, payload: String) {
        val status = lifiReceiptState(payload)
        if (status == LifiActionState.FAILED) {
            messages.add(LifiMessage(id = id, kind = LifiKind.ACTION, tool = tool, arguments = arguments,
                state = status, resultPayload = payload,
                detail = lifiReceiptMessage(payload).ifBlank { "执行未完成，请重新发起" }))
        }
        // Results are informational only. No confirmation or success popup interrupts the chat.
        val key = when (tool) { "create_task" -> "createdTasks"; "create_habit" -> "createdHabits"; else -> return }
        lifiArguments(payload).get(key)?.takeIf { it.isJsonArray }?.asJsonArray?.forEachIndexed { index, item ->
            if (item.isJsonObject) {
                val summary = lifiActionSummary(tool, item.toString())
                messages.add(LifiMessage(id = "$id-result-$index", kind = LifiKind.RESULT,
                    text = item.asJsonObject.label("name"), detail = "已创建\n" + summary.substringAfter('\n', "")))
            }
        }
    }

    fun stop() { if (running) job?.cancel() }
    private fun update(id: String, transform: (LifiMessage) -> LifiMessage) {
        val index = messages.indexOfFirst { it.id == id }; if (index >= 0) messages[index] = transform(messages[index])
    }
    private var saveJob: Job? = null
    private fun persist() {
        if (!ready) return
        saveJob?.cancel()
        val saved = LifiSavedState(messages.toList(), history.toList(), draft)
        saveJob = scope.launch {
            kotlinx.coroutines.delay(180)
            persistenceGate.withLock {
                val result = withContext(Dispatchers.IO) { runCatching { KmpV6Runtime.database.memory.saveSession(STORAGE_KEY, gson.toJson(saved)) } }
                storageError = if (result.isFailure) "对话记录保存失败，当前内容仍保留在本次会话中" else ""
            }
        }
    }
}
