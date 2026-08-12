package com.aritxonly.deadliner.ai

import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.service.LifiToolExecutor

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

/** Android owns confirmation UI only; persisted-data tool semantics live in KMP. */
internal class AndroidToolCallAdapter(
    private val sharedExecute: ((toolName: String, argumentsJson: String) -> String)? = null,
) : ToolCallAdapter {
    private val sharedExecutor by lazy {
        LifiToolExecutor(
            tasks = KmpV6Runtime.database.tasks,
            habits = KmpV6Runtime.database.habits,
        )
    }

    override suspend fun execute(call: ToolCallExecution): ToolExecutionResult {
        if (call.toolName == DeadlinerCoreBridge.TOOL_ADD_TO_CALENDAR) {
            return ToolExecutionResult(
                """{"ok":true,"reserved":true,"status":"pending_skill","message":"addToCalendar is handled by Android"}""",
            )
        }
        val result = sharedExecute?.invoke(call.toolName, call.argsJson)
            ?: sharedExecutor.execute(call.toolName, call.argsJson)
        return ToolExecutionResult(result)
    }
}
