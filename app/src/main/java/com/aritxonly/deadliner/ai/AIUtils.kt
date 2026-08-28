package com.aritxonly.deadliner.ai

import android.content.Context
import com.aritxonly.deadliner.logging.AppLog
import com.aritxonly.deadliner.localutils.ApiKeystore
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.overview.MonthlyAnalysisResult
import com.deadliner.domain.ai.model.AIConfig
import com.deadliner.lifi.llm.KtorLlmClient
import com.deadliner.lifi.utility.AiUtilityService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

object AIUtils {
    private var appContext: Context? = null
    private var coreBridge: DeadlinerCoreBridge? = null
    private val gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .create()
    }

    /** 初始化：在 Application 或首次使用时调用 */
    fun init(context: Context) {
        appContext = context.applicationContext
        coreBridge = DeadlinerCoreBridge(gson)
    }

    @Suppress("UNUSED_PARAMETER")
    fun setPreset(preset0: LlmPreset, context: Context) {
        appContext = context.applicationContext
        coreBridge = DeadlinerCoreBridge(gson)
    }

    suspend fun validateDirectConfig(
        apiKey: String,
        baseUrl: String,
        modelId: String,
    ) = withContext(Dispatchers.IO) {
        createAiUtilityService(
            apiKey = apiKey,
            baseUrl = baseUrl,
            modelId = modelId,
        ).validateConfig()
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun generateMixed(
        context: Context,
        rawText: String,
        profile: UserProfile? = null,
        candidatePrimary: IntentType? = null
    ): String = withContext(Dispatchers.IO) {
        val mixed = processByCore(context, rawText)
        gson.toJson(withReservedCalendarToolCall(mixed))
    }

    /**
     * 自动意图识别 + 调用 generateByIntent()
     * @param preferLLM 当启发式置信度 < 0.75 时，自动用 LLM 复核；置 false 则只用本地启发式
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun generateAuto(
        context: Context,
        rawText: String,
        profile: UserProfile? = null,
        preferLLM: Boolean = true
    ): Pair<IntentGuess, String> {
        val mixed = processByCore(context, rawText)
        val intent = mixed.primaryIntent.toIntentTypeOrDefault(IntentType.ExtractTasks)
        val guess = IntentGuess(
            intent = intent,
            confidence = 1.0,
            reason = "core_only"
        )
        return guess to gson.toJson(withReservedCalendarToolCall(mixed))
    }

    suspend fun generateMonthlyAnalysis(
        monthKey: String,
        monthName: String,
        metricsSummary: String,
        completedTaskNames: List<String>
    ): MonthlyAnalysisResult = withContext(Dispatchers.IO) {
        val context = appContext ?: error("AIUtils not initialized")
        val config = readCoreRuntimeConfig(context)
        val sharedResult = createAiUtilityService(
            apiKey = config.apiKey,
            baseUrl = config.baseUrl,
            modelId = config.modelId,
        ).generateMonthlyAnalysis(
            monthName = monthName,
            metricsSummary = metricsSummary,
            completedTaskNames = completedTaskNames,
            preferredLang = currentLangTag(context),
        )

        MonthlyAnalysisResult(
            month = monthKey,
            summary = sharedResult.summary.trim(),
            keywords = sharedResult.keywords
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .take(6),
        )
    }

    @Suppress("UNUSED_PARAMETER")
    internal suspend fun generateAutoInteractive(
        context: Context,
        rawText: String,
        profile: UserProfile? = null,
        preferLLM: Boolean = true,
        onToolRequest: (suspend (AIToolRequest) -> ToolDecision)? = null,
        onTextStream: (suspend (String) -> Unit)? = null,
        onThinking: (suspend (agent: String, phase: String, message: String?) -> Unit)? = null,
        onLifecycle: (suspend (AgentLifecycleEvent) -> Unit)? = null,
    ): Pair<IntentGuess, String> {
        val mixed = processByCore(
            context = context,
            rawText = rawText,
            onToolRequest = onToolRequest,
            onTextStream = onTextStream,
            onThinking = onThinking,
            onLifecycle = onLifecycle,
        )
        val intent = mixed.primaryIntent.toIntentTypeOrDefault(IntentType.ExtractTasks)
        val guess = IntentGuess(
            intent = intent,
            confidence = 1.0,
            reason = "core_interactive_only"
        )
        return guess to gson.toJson(withReservedCalendarToolCall(mixed))
    }

    fun extractJsonFromMarkdown(raw: String): String {
        val idx = raw.indexOf('{')
        if (idx >= 0) {
            var depth = 0
            for (i in idx until raw.length) {
                val c = raw[i]
                if (c == '{') depth++
                if (c == '}') {
                    depth--
                    if (depth == 0) return raw.substring(idx, i + 1).trim()
                }
            }
        }

        val jsonFenceRegex = Regex("```json\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        jsonFenceRegex.find(raw)?.let { return it.groups[1]!!.value.trim() }
        val anyFenceRegex = Regex("```\\s*([\\s\\S]*?)```")
        anyFenceRegex.find(raw)?.let { return it.groups[1]!!.value.trim() }

        return raw.trim()
    }

    fun parseMixedResult(
        json: String,
        gson: Gson = GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .create()
    ): MixedResult = gson.fromJson(json, MixedResult::class.java)

    fun parseAIResult(intent: IntentType, json: String, gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .create()
    ): AIResult = when (intent) {
        IntentType.ExtractTasks -> {
            val r = gson.fromJson(json, ExtractTasksResult::class.java)
            AIResult.ExtractTasks(r)
        }
        IntentType.PlanDay -> {
            data class PlanResp(val blocks: List<PlanBlock>)
            val r = gson.fromJson(json, PlanResp::class.java)
            AIResult.PlanDay(r.blocks)
        }
        IntentType.SplitToSteps -> {
            val r = gson.fromJson(json, SplitStepsResult::class.java)
            AIResult.SplitToSteps(r)
        }
    }

    private fun currentLangTag(context: Context): String {
        val loc = context.resources.configuration.locales[0]
        return loc.toLanguageTag() // e.g., "zh-CN" / "en-US"
    }

    fun normalizeDirectEndpoint(baseUrl: String): String {
        var normalized = baseUrl.trim()
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://$normalized"
        }
        normalized = normalized.trimEnd('/')
        if (!normalized.endsWith("/chat/completions")) {
            normalized += "/chat/completions"
        }
        return normalized
    }

    private suspend fun processByCore(
        context: Context,
        rawText: String,
        onToolRequest: (suspend (AIToolRequest) -> ToolDecision)? = null,
        onTextStream: (suspend (String) -> Unit)? = null,
        onThinking: (suspend (agent: String, phase: String, message: String?) -> Unit)? = null,
        onLifecycle: (suspend (AgentLifecycleEvent) -> Unit)? = null,
    ): MixedResult {
        val bridge = coreBridge ?: DeadlinerCoreBridge(gson).also {
            coreBridge = it
        }
        val config = readCoreRuntimeConfig(context)
        val output = bridge.processInput(
            text = rawText,
            apiKey = config.apiKey,
            baseUrl = config.baseUrl,
            modelId = config.modelId,
            platform = "android",
            onToolRequest = onToolRequest,
            onTextStream = onTextStream,
            onThinking = onThinking,
            onLifecycle = onLifecycle,
        )
        GlobalUtils.incrementLifiModelUsageCount()
        return output.mixed
    }

    private fun withReservedCalendarToolCall(mixed: MixedResult): MixedResult {
        val firstPlan = mixed.planBlocks.firstOrNull() ?: return mixed
        val existing = mixed.toolCalls.any { it.tool == DeadlinerCoreBridge.TOOL_ADD_TO_CALENDAR }
        if (existing) return mixed

        val reserved = AIToolCall(
            tool = DeadlinerCoreBridge.TOOL_ADD_TO_CALENDAR,
            addToCalendarArgs = AddToCalendarArgs(
                title = firstPlan.title,
                start = firstPlan.start,
                end = firstPlan.end,
                location = firstPlan.location,
                description = firstPlan.linkTask?.let { "关联任务: $it" },
            ),
            reason = "保留为后续 Calendar Skill 执行入口",
        )

        return mixed.copy(toolCalls = mixed.toolCalls + reserved)
    }

    private fun readCoreRuntimeConfig(context: Context): CoreRuntimeConfig {
        val apiKey = ApiKeystore.retrieveAndDecrypt(context).orEmpty()
        val preset = GlobalUtils.getDeadlinerAIConfig().getCurrentPreset() ?: defaultLlmPreset
        val baseUrl = normalizeCoreBaseUrl(preset.endpoint)
        if (apiKey.isBlank()) {
            AppLog.w("AIUtils", "core runtime api key is empty")
        }
        AppLog.d(
            "AIUtils",
            "core runtime config: endpoint=${preset.endpoint}, normalizedBaseUrl=$baseUrl, model=${preset.model}"
        )
        return CoreRuntimeConfig(
            apiKey = apiKey,
            baseUrl = baseUrl,
            modelId = preset.model,
        )
    }

    private fun normalizeCoreBaseUrl(endpoint: String): String {
        var e = endpoint.trim().trimEnd('/')
        // Core expects provider-compatible base URL (e.g. .../v1), not app proxy endpoints.
        if (e.contains("deadliner.aritxonly.top/api")) {
            return "https://api.deepseek.com/v1"
        }
        if (e.endsWith("/api")) {
            return "https://api.deepseek.com/v1"
        }
        if (e.endsWith("/chat/completions")) {
            e = e.removeSuffix("/chat/completions")
        }
        if (!e.startsWith("http://") && !e.startsWith("https://")) {
            e = "https://$e"
        }
        return e.trimEnd('/')
    }

    private data class CoreRuntimeConfig(
        val apiKey: String,
        val baseUrl: String,
        val modelId: String,
    )

    private fun createAiUtilityService(
        apiKey: String,
        baseUrl: String,
        modelId: String,
    ): AiUtilityService {
        val config = AIConfig(
            apiKey = apiKey,
            baseUrl = baseUrl,
            model = modelId,
            platform = "android",
            timezone = java.util.TimeZone.getDefault().id,
        )
        return AiUtilityService(config, KtorLlmClient(config))
    }
}
