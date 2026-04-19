package com.aritxonly.deadliner.ai

import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.data.HabitRepository
import com.aritxonly.deadliner.model.DeadlineFrequency
import com.aritxonly.deadliner.model.DDLState
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.HabitGoalType
import com.aritxonly.deadliner.model.HabitMetaData
import com.aritxonly.deadliner.model.HabitPeriod
import com.aritxonly.deadliner.model.toJson
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal data class ToolCallExecution(
    val id: String,
    val toolName: String,
    val argsJson: String,
)

internal data class ToolExecutionResult(
    val resultJson: String,
)

internal interface ToolCallAdapter {
    suspend fun execute(call: ToolCallExecution): ToolExecutionResult
}

internal class AndroidToolCallAdapter(
    private val gson: Gson,
) : ToolCallAdapter {
    override suspend fun execute(call: ToolCallExecution): ToolExecutionResult {
        val resultJson = when (call.toolName) {
            DeadlinerCoreBridge.TOOL_READ_TASKS -> {
                val args = parseReadTasksArgs(call.argsJson) ?: ReadTasksArgs()
                gson.toJson(executeReadTasks(args))
            }
            DeadlinerCoreBridge.TOOL_CREATE_TASK -> {
                gson.toJson(executeCreateTask(call.argsJson))
            }
            DeadlinerCoreBridge.TOOL_READ_HABITS -> {
                gson.toJson(executeReadHabits(call.argsJson))
            }
            DeadlinerCoreBridge.TOOL_CREATE_HABIT -> {
                gson.toJson(executeCreateHabit(call.argsJson))
            }

            DeadlinerCoreBridge.TOOL_ADD_TO_CALENDAR -> gson.toJson(
                mapOf(
                    "ok" to true,
                    "reserved" to true,
                    "status" to "pending_skill",
                    "message" to "addToCalendar will be handled by app skill later",
                )
            )

            else -> gson.toJson(
                mapOf(
                    "ok" to false,
                    "errorCode" to "UNSUPPORTED_TOOL",
                    "message" to "unsupported tool: ${call.toolName}",
                )
            )
        }
        return ToolExecutionResult(resultJson)
    }

    private fun executeCreateTask(argsJson: String): Map<String, Any?> {
        val repo = DDLRepository()
        val argsObj = extractArgsObject(argsJson)
        val name = argsObj?.get("name")?.asString?.trim().orEmpty()
        if (name.isBlank()) {
            return mapOf(
                "ok" to false,
                "errorCode" to "INVALID_ARGUMENT",
                "message" to "create_task.name is required",
            )
        }
        val note = runCatching {
            if (argsObj?.has("note") == true && !argsObj.get("note").isJsonNull) argsObj.get("note").asString else ""
        }.getOrDefault("")

        val dueText = runCatching {
            if (argsObj?.has("dueTime") == true && !argsObj.get("dueTime").isJsonNull) argsObj.get("dueTime").asString else ""
        }.getOrDefault("")

        val due = parseFlexibleDue(dueText)
            ?: LocalDateTime.now().plusDays(1).withHour(20).withMinute(0).withSecond(0).withNano(0)
        val start = LocalDateTime.now()

        val id = repo.insertDDL(
            name = name,
            startTime = start.toString(),
            endTime = due.toString(),
            note = note,
            type = DeadlineType.TASK,
        )

        return mapOf(
            "ok" to true,
            "task" to mapOf(
                "id" to id,
                "name" to name,
                "dueTime" to due.format(DUE_FMT),
                "note" to note,
            )
        )
    }

    private fun executeReadHabits(argsJson: String): Map<String, Any> {
        val habitRepo = HabitRepository()
        val obj = runCatching { gson.fromJson(argsJson, JsonObject::class.java) }.getOrNull()
        val limit = runCatching {
            if (obj?.has("limit") == true) obj.get("limit").asInt
            else if (obj?.has("args") == true && obj.getAsJsonObject("args").has("limit")) obj.getAsJsonObject("args").get("limit").asInt
            else 20
        }.getOrDefault(20).coerceIn(1, 100)
        val keyword = runCatching {
            if (obj?.has("keyword") == true) obj.get("keyword").asString
            else if (obj?.has("args") == true && obj.getAsJsonObject("args").has("keyword")) obj.getAsJsonObject("args").get("keyword").asString
            else ""
        }.getOrDefault("").trim()

        val habits = habitRepo.getAllHabits()
            .asSequence()
            .filter { it.status.name == "ACTIVE" }
            .filter { keyword.isBlank() || it.name.contains(keyword, ignoreCase = true) }
            .take(limit)
            .map {
                mapOf(
                    "id" to it.id,
                    "ddlId" to it.ddlId,
                    "name" to it.name,
                    "period" to it.period.name.lowercase(),
                    "timesPerPeriod" to it.timesPerPeriod,
                    "goalType" to if (it.goalType == HabitGoalType.TOTAL) "total" else "frequency",
                    "totalTarget" to it.totalTarget,
                )
            }.toList()

        return mapOf(
            "habits" to habits,
            "summary" to mapOf("count" to habits.size),
        )
    }

    private fun executeCreateHabit(argsJson: String): Map<String, Any?> {
        val ddlRepo = DDLRepository()
        val habitRepo = HabitRepository()
        val argsObj = extractArgsObject(argsJson)
        val name = argsObj?.get("name")?.asString?.trim().orEmpty()
        if (name.isBlank()) {
            return mapOf(
                "ok" to false,
                "errorCode" to "INVALID_ARGUMENT",
                "message" to "create_habit.name is required",
            )
        }
        val periodRaw = argsObj?.get("period")?.asString.orEmpty()
        val period = toHabitPeriod(periodRaw)
        val timesPerPeriod = runCatching { argsObj?.get("timesPerPeriod")?.asInt ?: 1 }
            .getOrDefault(1)
            .coerceAtLeast(1)
        val goalTypeRaw = argsObj?.get("goalType")?.asString.orEmpty()
        val goalType = toHabitGoalType(goalTypeRaw)
        val totalTarget = runCatching {
            if (argsObj?.has("totalTarget") == true && !argsObj.get("totalTarget").isJsonNull) {
                argsObj.get("totalTarget").asInt
            } else null
        }.getOrNull()

        val description = runCatching {
            if (argsObj?.has("description") == true && !argsObj.get("description").isJsonNull) {
                argsObj.get("description").asString
            } else null
        }.getOrNull()

        val now = LocalDateTime.now()
        val habitFrequencyType = when {
            goalType == HabitGoalType.TOTAL -> DeadlineFrequency.TOTAL
            period == HabitPeriod.WEEKLY -> DeadlineFrequency.WEEKLY
            period == HabitPeriod.MONTHLY -> DeadlineFrequency.MONTHLY
            else -> DeadlineFrequency.DAILY
        }
        val meta = HabitMetaData(
            completedDates = emptySet(),
            frequencyType = habitFrequencyType,
            frequency = if (goalType == HabitGoalType.TOTAL) 1 else timesPerPeriod,
            total = totalTarget ?: 0,
            refreshDate = LocalDate.now().toString(),
        )

        val ddlId = ddlRepo.insertDDL(
            name = name,
            startTime = now.toString(),
            endTime = now.toString(),
            note = meta.toJson(),
            type = DeadlineType.HABIT,
        )
        val habitId = habitRepo.createHabitForDdl(
            ddlId = ddlId,
            name = name,
            period = period,
            timesPerPeriod = if (goalType == HabitGoalType.TOTAL) 1 else timesPerPeriod,
            goalType = goalType,
            totalTarget = if (goalType == HabitGoalType.TOTAL) (totalTarget ?: 1) else null,
            description = description,
        )
        val created = habitRepo.getHabitById(habitId)

        return mapOf(
            "ok" to true,
            "habit" to mapOf(
                "id" to habitId,
                "ddlId" to ddlId,
                "name" to (created?.name ?: name),
                "period" to (created?.period?.name?.lowercase() ?: period.name.lowercase()),
                "timesPerPeriod" to (created?.timesPerPeriod ?: timesPerPeriod),
                "goalType" to if ((created?.goalType ?: goalType) == HabitGoalType.TOTAL) "total" else "frequency",
                "totalTarget" to (created?.totalTarget ?: if (goalType == HabitGoalType.TOTAL) (totalTarget ?: 1) else null),
            ),
        )
    }

    private fun extractArgsObject(argsJson: String): JsonObject? {
        val obj = runCatching { gson.fromJson(argsJson, JsonObject::class.java) }.getOrNull() ?: return null
        if (obj.has("args") && obj.get("args").isJsonObject) {
            return obj.getAsJsonObject("args")
        }
        return obj
    }

    private fun toHabitPeriod(raw: String): HabitPeriod {
        return when (raw.trim().lowercase()) {
            "day", "daily" -> HabitPeriod.DAILY
            "week", "weekly" -> HabitPeriod.WEEKLY
            "month", "monthly" -> HabitPeriod.MONTHLY
            else -> HabitPeriod.DAILY
        }
    }

    private fun toHabitGoalType(raw: String): HabitGoalType {
        return when (raw.trim().lowercase()) {
            "total" -> HabitGoalType.TOTAL
            "frequency", "completion", "per_period", "perperiod" -> HabitGoalType.PER_PERIOD
            else -> HabitGoalType.PER_PERIOD
        }
    }

    private fun parseReadTasksArgs(argsJson: String): ReadTasksArgs? {
        val obj = runCatching { gson.fromJson(argsJson, JsonObject::class.java) }.getOrNull()
        return runCatching { gson.fromJson(argsJson, ReadTasksArgs::class.java) }.getOrNull()
            ?: runCatching {
                if (obj?.has("args") == true && obj.get("args").isJsonObject) {
                    gson.fromJson(obj.getAsJsonObject("args"), ReadTasksArgs::class.java)
                } else null
            }.getOrNull()
    }

    private fun executeReadTasks(rawArgs: ReadTasksArgs): ReadTasksResultPayload {
        val args = normalizeReadTasksArgs(rawArgs)
        val now = LocalDateTime.now()
        val dueEnd = now.plusDays((args.timeRangeDays ?: DEFAULT_RANGE_DAYS).toLong())

        val items = DDLRepository()
            .getAllDDLs()
            .asSequence()
            .filter { it.type == DeadlineType.TASK }
            .filter { !it.isArchived && it.state != DDLState.ARCHIVED && it.state != DDLState.ABANDONED_ARCHIVED }
            .filter { item ->
                when ((args.status ?: "OPEN").uppercase()) {
                    "DONE" -> item.state == DDLState.COMPLETED
                    "ALL" -> true
                    else -> item.state == DDLState.ACTIVE || item.state == DDLState.ABANDONED
                }
            }
            .filter { item ->
                val kws = args.keywords.orEmpty().map { it.trim() }.filter { it.isNotEmpty() }
                if (kws.isEmpty()) return@filter true
                val hay = (item.name + "\n" + item.note).lowercase()
                kws.any { hay.contains(it.lowercase()) }
            }
            .map { item ->
                val due = parseDue(item.endTime)
                item to due
            }
            .filter { (_, due) ->
                val days = args.timeRangeDays ?: DEFAULT_RANGE_DAYS
                if (days <= 0) return@filter true
                if (due == null) return@filter true
                due <= dueEnd
            }
            .sortedWith(
                when ((args.sort ?: "DUE_ASC").uppercase()) {
                    "UPDATED_DESC" -> compareByDescending<Pair<com.aritxonly.deadliner.model.DDLItem, LocalDateTime?>> { it.first.timeStamp }
                    else -> compareBy<Pair<com.aritxonly.deadliner.model.DDLItem, LocalDateTime?>> { it.second ?: LocalDateTime.MAX }
                }
            )
            .take((args.limit ?: DEFAULT_LIMIT).coerceAtMost(MAX_LIMIT))
            .toList()

        val digests = items.map { (item, due) ->
            TaskDigestItem(
                id = item.id,
                name = item.name,
                due = due?.format(DUE_FMT) ?: "",
                status = if (item.state == DDLState.COMPLETED) "DONE" else "OPEN",
                notePreview = item.note.trim().replace("\n", " ").take(40)
            )
        }

        val overdue = items.count { (item, due) ->
            due != null && due.isBefore(now) && item.state != DDLState.COMPLETED
        }
        val dueSoon24h = items.count { (item, due) ->
            due != null && !due.isBefore(now) && due.isBefore(now.plusHours(24)) && item.state != DDLState.COMPLETED
        }

        return ReadTasksResultPayload(
            tasks = digests,
            summary = TaskSummary(
                count = digests.size,
                overdue = overdue,
                dueSoon24h = dueSoon24h,
            )
        )
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

    private fun parseDue(raw: String?): LocalDateTime? {
        if (raw.isNullOrBlank()) return null
        val text = raw.trim()
        return runCatching { LocalDateTime.parse(text) }.getOrNull()
            ?: runCatching { LocalDateTime.parse(text, DUE_FMT) }.getOrNull()
    }

    private fun parseFlexibleDue(raw: String?): LocalDateTime? {
        if (raw.isNullOrBlank()) return null
        val text = raw.trim()
        return runCatching { LocalDateTime.parse(text) }.getOrNull()
            ?: runCatching { LocalDateTime.parse(text, DUE_FMT) }.getOrNull()
            ?: runCatching { LocalDate.parse(text).atTime(20, 0) }.getOrNull()
    }

    private companion object {
        private const val DEFAULT_RANGE_DAYS = 7
        private const val DEFAULT_LIMIT = 20
        private const val MAX_LIMIT = 50
        private val DUE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
