package com.aritxonly.deadliner.ui.lifi

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

internal enum class LifiKind { USER, TEXT, PROCESS, ACTION, RESULT, ERROR }
internal enum class LifiActionState { PENDING, EXECUTING, SUCCEEDED, REJECTED, FAILED, EXPIRED }
internal data class LifiMessage(
    val id: String = UUID.randomUUID().toString(),
    val kind: LifiKind,
    val text: String = "",
    val detail: String = "",
    val tool: String = "",
    val arguments: String = "{}",
    val state: LifiActionState = LifiActionState.PENDING,
    val proposal: Boolean = false,
    val resultPayload: String = "",
)
internal data class LifiHistory(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val messages: List<LifiMessage>,
    val createdAt: Long = System.currentTimeMillis(),
)
internal data class LifiSavedState(
    val messages: List<LifiMessage> = emptyList(),
    val history: List<LifiHistory> = emptyList(),
    val draft: String = "",
)

internal fun lifiToolTitle(tool: String): String = when (tool) {
    "read_tasks" -> "读取任务"
    "read_habits" -> "读取习惯"
    "create_task" -> "创建任务"
    "create_habit" -> "创建习惯"
    "update_deadline" -> "调整截止时间"
    else -> "执行操作"
}
internal fun lifiArguments(raw: String): JsonObject = runCatching {
    JsonParser.parseString(raw).asJsonObject
}.getOrDefault(JsonObject())
internal fun JsonObject.objectValue(key: String): JsonObject? = get(key)?.takeIf { it.isJsonObject }?.asJsonObject
internal fun JsonObject.label(key: String): String = runCatching { get(key)?.asString.orEmpty() }.getOrDefault("")
internal fun lifiActionSummary(tool: String, raw: String): String {
    val args = lifiArguments(raw)
    return when (tool) {
        "create_task" -> listOf(args.label("name"), args.label("dueTime"), args.label("note")).filter(String::isNotBlank).joinToString("\n")
        "create_habit" -> {
            val goal = args.objectValue("goal")
            val count = goal?.label("targetCount") ?: args.label("timesPerPeriod")
            val window = goal?.label("window") ?: args.label("period")
            val period = when (window) { "WEEK", "WEEKLY" -> "每周"; "MONTH", "MONTHLY" -> "每月"; "LIFETIME" -> "累计"; else -> "每天" }
            val tracking = args.objectValue("tracking")
            val unit = if (tracking?.label("mode") == "QUANTITY") tracking.label("unit").ifBlank { "单位" } else "次"
            val schedule = args.objectValue("schedule")
            val reminder = args.objectValue("reminder")
            val timing = when (schedule?.label("kind")) {
                "ONCE" -> "一次性 · ${schedule.label("startOn")}"
                "ALGORITHM" -> "算法计划 · ${schedule.label("algorithmKey")}"
                "WEEKLY_DAYS" -> {
                    val mask = schedule.label("weekdaysMask").toIntOrNull() ?: 127
                    "执行日：" + listOf("一", "二", "三", "四", "五", "六", "日").filterIndexed { i, _ -> mask and (1 shl i) != 0 }.joinToString("、") { "周$it" }
                }
                else -> ""
            }
            listOf("${args.label("name")}\n$period $count $unit", timing, reminder?.takeIf { it.label("isEnabled") != "false" }?.let { "提醒：${it.label("localTime")}" }.orEmpty()).filter(String::isNotBlank).joinToString("\n")
        }
        "read_tasks" -> {
            val range = args.label("timeRangeDays").ifBlank { "7" }
            val limit = args.label("limit").ifBlank { "20" }
            val status = when (args.label("status")) { "DONE" -> "已完成"; "ALL" -> "所有状态"; else -> "未完成" }
            val keywords = args.get("keywords")?.takeUnless { it.isJsonNull }?.toString()
            "未来 $range 天 · $status · 最多 $limit 项" + if (keywords != null) "\n关键词：$keywords" else ""
        }
        "read_habits" -> "查看已有习惯" + (args.get("keywords")?.takeUnless { it.isJsonNull }?.let { "\n关键词：$it" } ?: "")
        "update_deadline" -> "${args.label("taskId")}\n新的截止时间：${args.label("newDueTime")}"
        else -> raw
    }
}
/** Edit only exposed fields, preserving schedule/tracking/reminder and unknown future fields. */
internal fun lifiEditedArguments(tool: String, raw: String, name: String, time: String, note: String, count: String, goalWindow: String? = null): String {
    val args = lifiArguments(raw)
    require(name.isNotBlank() || tool == "update_deadline") { "请填写名称" }
    when (tool) {
        "create_task" -> { validateLifiTime(time); args.addProperty("name", name.trim()); args.addProperty("dueTime", time.trim()); args.addProperty("note", note) }
        "update_deadline" -> { validateLifiTime(time); args.addProperty("newDueTime", time.trim()) }
        "create_habit" -> {
            val target = count.toIntOrNull(); require(target != null && target in 1..10000) { "次数需在 1–10000 之间" }
            args.addProperty("name", name.trim())
            args.objectValue("goal")?.addProperty("targetCount", target) ?: args.addProperty("timesPerPeriod", target)
            if (goalWindow != null) {
                require(goalWindow in listOf("SCHEDULED_DAY", "WEEK", "MONTH", "LIFETIME")) { "请选择目标周期" }
                args.objectValue("goal")?.addProperty("window", goalWindow) ?: run {
                    args.addProperty("period", when (goalWindow) { "WEEK" -> "WEEKLY"; "MONTH" -> "MONTHLY"; else -> "DAILY" })
                    args.addProperty("goalType", if (goalWindow == "LIFETIME") "TOTAL" else "PER_PERIOD")
                    if (goalWindow == "LIFETIME") args.addProperty("totalTarget", target)
                }
            }
        }
    }
    return args.toString()
}
private fun validateLifiTime(value: String) {
    require(runCatching { LocalDateTime.parse(value.trim().replace('T', ' '), DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(java.time.format.ResolverStyle.STRICT)) }.isSuccess) { "时间格式：2026-10-13 15:00" }
}
internal fun lifiReceiptState(payload: String): LifiActionState = runCatching {
    val json = JsonParser.parseString(payload).asJsonObject
    when {
        json.get("denied")?.asBoolean == true -> LifiActionState.REJECTED
        json.get("ok")?.asBoolean == false || json.has("errorCode") -> LifiActionState.FAILED
        json.get("reserved")?.asBoolean == true -> LifiActionState.FAILED
        json.get("ok")?.asBoolean == true || json.get("tasks")?.isJsonArray == true || json.get("habits")?.isJsonArray == true -> LifiActionState.SUCCEEDED
        else -> LifiActionState.FAILED
    }
}.getOrDefault(LifiActionState.FAILED)
internal fun LifiMessage.restored(): LifiMessage = when {
    kind == LifiKind.PROCESS && state == LifiActionState.EXECUTING -> copy(text = "上次处理已中断", state = LifiActionState.EXPIRED)
    kind == LifiKind.ACTION && state in listOf(LifiActionState.PENDING, LifiActionState.EXECUTING) -> copy(state = LifiActionState.EXPIRED, detail = "会话已结束，请重新发起")
    else -> this
}

internal fun lifiReceiptMessage(payload: String): String {
    val json = lifiArguments(payload)
    if (json.label("partialSuccess") == "true") {
        val summary = json.objectValue("summary")
        return "部分完成：${summary?.label("successCount")} 项成功，${summary?.label("failureCount")} 项失败。已成功的内容已保存。"
    }
    return json.label("message").ifBlank {
        json.get("items")?.takeIf { it.isJsonArray }?.asJsonArray?.firstOrNull { it.isJsonObject && it.asJsonObject.label("ok") == "false" }?.asJsonObject?.label("message").orEmpty()
    }
}
