package com.aritxonly.deadliner.ui.quickadd

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.serialization.json.Json
import androidx.compose.ui.geometry.Rect
import com.aritxonly.deadliner.ai.AIUtils
import com.aritxonly.deadliner.model.CalendarEvent
import com.deadliner.domain.quickadd.QuickAddParser
import com.deadliner.domain.quickadd.QuickAddProposal
import com.deadliner.domain.quickadd.model.QuickAddRecognitionPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Stable
class QuickAddState(private val scope: CoroutineScope,
    private val aiRecognizer: suspend (String, String) -> QuickAddProposal = AIUtils::extractQuickAdd) {
    var visible by mutableStateOf(false)
        private set
    var anchor by mutableStateOf(Rect.Zero)
    var text by mutableStateOf("")
        private set
    var proposal by mutableStateOf(QuickAddProposal())
        private set
    var busy by mutableStateOf(false)
        private set
    var recognizingAI by mutableStateOf(false)
        private set
    private var recognizedStart by mutableStateOf<LocalDateTime?>(null)
    var message by mutableStateOf("")
        private set
    var startAt by mutableStateOf<LocalDateTime?>(null)
    var dueAt by mutableStateOf<LocalDateTime?>(null)
    var categoryUid by mutableStateOf<String?>(null)
    var calendarEventId by mutableStateOf<Long?>(null)
    private var defaultStart by mutableStateOf(LocalDateTime.now())
    private var initialKind = "TASK"
    private var operation: Job? = null
    private var generation = 0
    private val parser = QuickAddParser()
    private var localOperation: Job? = null
    private var localRevision = 0
    private fun cancelLocalRecognition() { localRevision++; localOperation?.cancel() }
    internal var modelAvailable: () -> Boolean = { false }
    internal var modelRecognizer: (suspend (String, String) -> QuickAddProposal?)? = null

    internal fun refreshWithModel() {
        val recognize = modelRecognizer ?: return
        localOperation?.cancel()
        if (!visible || busy || text.isBlank() || proposal.source !in listOf("LOCAL", "MODEL")) return
        if (modelAvailable()) {
            proposal = QuickAddRecognitionPolicy().pending(text, initialKind)
            recognizedStart = null
        }
        val token = generation
        val localToken = ++localRevision
        val captured = text
        val kind = initialKind
        val before = proposal
        val beforeStart = startAt
        val beforeDue = dueAt
        val beforeRecognizedStart = recognizedStart
        localOperation = scope.launch {
            try {
                delay(100)
                val result = recognize(captured, kind) ?: return@launch
                if (generation == token && localRevision == localToken && visible && !busy && text == captured &&
                    proposal == before && startAt == beforeStart && dueAt == beforeDue && recognizedStart == beforeRecognizedStart) {
                    proposal = result
                    message = result.message
                    recognizedStart = null
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* The immediate rule proposal remains visible. */ }
        }
    }
    val effectiveProposal: QuickAddProposal get() = if (proposal.kind == "TASK") proposal.copy(
        dueAt = dueAt?.toString() ?: proposal.dueAt ?: (explicitStartAt ?: defaultStart).plusHours(1).toString()) else proposal
    val explicitStartAt: LocalDateTime? get() = startAt ?: recognizedStart
    val explicitDueAt: String? get() = dueAt?.toString() ?: proposal.dueAt
    val effectiveStartAt: LocalDateTime get() = explicitStartAt ?: effectiveProposal.dueAt
        ?.let { runCatching { LocalDateTime.parse(it.replace(' ', 'T')) }.getOrNull() }
        ?.takeIf { it.isBefore(defaultStart) } ?: defaultStart
    val needsAI: Boolean get() = text.isNotBlank() &&
        (proposal.needsAI || (proposal.source == "LOCAL" &&
            (!proposal.ready || !proposal.title.any(Char::isLetter))))
    val canEditTaskTimes: Boolean get() = !busy && proposal.kind == "TASK" && proposal.ready && !needsAI
    val canReinterpret: Boolean get() = visible && !busy && proposal.ready && !needsAI && text.isNotBlank()
    val canClose: Boolean get() = !busy || recognizingAI
    val canRecognize: Boolean get() = visible && !busy && text.isNotBlank()
    val canSubmit: Boolean get() = visible && !busy && text.isNotBlank() && !needsAI && effectiveProposal.ready

    fun open(kind: String) {
        if (initialKind != kind || text.isBlank()) {
            val result = parser.recognizeLocal(text, kind)
            proposal = result.proposal
            recognizedStart = result.startAt
        }
        initialKind = kind
        if (text.isBlank() && startAt == null && dueAt == null) defaultStart = LocalDateTime.now()
        visible = true
        refreshWithModel()
    }
    fun dismiss() {
        if (busy && !recognizingAI) return
        generation++
        localOperation?.cancel()
        operation?.cancel()
        busy = false
        recognizingAI = false
        visible = false
    }
    fun edit(value: String) {
        if (calendarEventId != null) { startAt = null; dueAt = null }
        generation++
        localOperation?.cancel()
        operation?.cancel()
        busy = false
        recognizingAI = false
        text = value
        val result = parser.recognizeLocal(value, initialKind)
        proposal = result.proposal
        recognizedStart = result.startAt
        calendarEventId = null
        message = ""
        refreshWithModel()
    }
    fun setReminder(time: String?) { cancelLocalRecognition(); proposal = proposal.copy(reminderTime = time) }
    fun removeStart() { cancelLocalRecognition(); startAt = null; recognizedStart = null }
    fun removeDue() { cancelLocalRecognition(); dueAt = null; proposal = proposal.copy(dueAt = null) }
    fun setNote(note: String) { cancelLocalRecognition(); proposal = proposal.copy(note = note) }
    fun removeSchedule() { cancelLocalRecognition(); proposal = proposal.copy(kind = "TASK", reminderTime = null) }
    fun setSchedule(text: String): Boolean {
        val parsed = parser.parse("$text ${proposal.title}", "HABIT")
        if (!parsed.ready || parsed.needsAI || parsed.title != proposal.title) return false
        cancelLocalRecognition()
        proposal = proposal.copy(period = parsed.period, targetCount = parsed.targetCount, weekdaysMask = parsed.weekdaysMask)
        return true
    }
    fun recognizeWithAI() {
        if (!canRecognize) return
        localOperation?.cancel()
        message = ""
        busy = true
        recognizingAI = true
        val token = ++generation
        operation = scope.launch {
            try {
                val before = proposal
                val result = withTimeout(60_000) { aiRecognizer(text, proposal.kind) }
                if (generation == token) {
                    if (result.title.isNotBlank()) proposal = result
                    message = when {
                        result.message.isNotBlank() -> result.message
                        !result.ready -> "AI 未返回可添加草案，请调整输入后重试"
                        result.copy(source = before.source, message = before.message, recognitionState = before.recognitionState) == before -> "AI 已确认草案"
                        else -> "AI 已更新草案"
                    }
                }
            } catch (error: TimeoutCancellationException) {
                if (generation == token) message = quickAddAIError(error)
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { if (generation == token) message = quickAddAIError(error) }
            finally { if (generation == token) { busy = false; recognizingAI = false } }
        }
    }
    fun importEvent(event: CalendarEvent) {
        edit(event.title)
        fun local(millis: Long) = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()
        startAt = local(event.startMillis)
        dueAt = local(event.endMillis)
        proposal = QuickAddProposal(title = event.title, note = event.description,
            dueAt = dueAt.toString(), ready = event.title.isNotBlank(), source = "CALENDAR")
        calendarEventId = event.id
    }
    fun submit(onSaved: () -> Unit) {
        if (!canSubmit) return
        localOperation?.cancel()
        message = ""
        busy = true
        val token = ++generation
        operation = scope.launch {
            try {
                val draft = effectiveProposal
                val end = draft.dueAt?.let { LocalDateTime.parse(it.replace(' ', 'T')) }
                if (draft.kind == "TASK" && end != null && end.isBefore(effectiveStartAt)) {
                    message = "截止时间不能早于开始时间"
                } else {
                    QuickAddPersistence.save(draft, effectiveStartAt.takeIf { draft.kind == "TASK" }, categoryUid, calendarEventId)
                    visible = false
                    text = ""
                    startAt = null; dueAt = null; recognizedStart = null; categoryUid = null; calendarEventId = null
                    onSaved()
                }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { if (generation == token) message = "保存失败，请重试" }
            finally { if (generation == token) busy = false }
        }
    }
    companion object {
        fun saver(scope: CoroutineScope) = Saver<QuickAddState, List<String>>(
            save = { listOf(it.text, it.initialKind, it.proposal.toJson(), it.visible.toString(),
                it.startAt?.toString().orEmpty(), it.dueAt?.toString().orEmpty(),
                it.categoryUid.orEmpty(), it.calendarEventId?.toString().orEmpty(), it.defaultStart.toString(), it.recognizedStart?.toString().orEmpty()) },
            restore = { saved -> QuickAddState(scope).apply {
                text = saved[0]; initialKind = saved[1]
                proposal = Json.decodeFromString<QuickAddProposal>(saved[2])
                visible = saved[3].toBoolean()
                startAt = saved[4].takeIf(String::isNotEmpty)?.let(LocalDateTime::parse)
                dueAt = saved[5].takeIf(String::isNotEmpty)?.let(LocalDateTime::parse)
                categoryUid = saved[6].takeIf(String::isNotEmpty)
                calendarEventId = saved[7].toLongOrNull()
                saved.getOrNull(8)?.let { defaultStart = LocalDateTime.parse(it) }
                recognizedStart = saved.getOrNull(9)?.takeIf(String::isNotEmpty)?.let(LocalDateTime::parse)
            } })
    }
}

@Composable
fun rememberQuickAddState(): QuickAddState {
    val scope = rememberCoroutineScope()
    val state = rememberSaveable(saver = QuickAddState.saver(scope)) { QuickAddState(scope) }
    val context = LocalContext.current.applicationContext
    val modelReady by QuickAddModelService.ready.collectAsState()
    LaunchedEffect(state) {
        state.modelRecognizer = QuickAddModelService::recognize
        state.modelAvailable = { QuickAddModelService.ready.value }
    }
    LaunchedEffect(state.visible) { if (state.visible) QuickAddModelService.prepare(context) }
    LaunchedEffect(modelReady) { if (modelReady) state.refreshWithModel() }
    return state
}
