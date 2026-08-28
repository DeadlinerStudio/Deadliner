package com.aritxonly.deadliner.logging

import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File
import java.time.Instant
import java.util.UUID

/**
 * Android's file-backed diagnostic log. The record schema and text projection
 * intentionally match iOS AppLog so the two exported files are interchangeable.
 */
object AppLog {
    private const val TAG = "DeadlinerAppLog"
    private const val DIRECTORY = "Diagnostics"
    private const val JSONL_FILE = "deadliner-diagnostics.jsonl"
    private const val STANDARD_FILE = "deadliner-diagnostics.log"
    private const val RETENTION_MILLIS = 14L * 24L * 60L * 60L * 1_000L
    private const val MAXIMUM_FILE_BYTES = 8L * 1_024L * 1_024L

    private val lock = Any()
    private val gson = Gson()
    private val sessionId = UUID.randomUUID().toString().lowercase()
    private var directory: File? = null
    private var lastPrunedAtMillis = 0L

    fun initialize(context: Context) {
        synchronized(lock) {
            directory = File(context.filesDir, DIRECTORY).also { it.mkdirs() }
            pruneLocked(force = true)
            event("app.launch", domain = "lifecycle")
            event("app.environment", domain = "environment", context = environmentContext(context))
        }
    }

    fun d(tag: String, message: String) = write(LogLevel.DEBUG, tag, message)

    fun i(tag: String, message: String) = write(LogLevel.INFO, tag, message)

    fun w(tag: String, message: String, error: Throwable? = null) = write(LogLevel.WARNING, tag, message, error)

    fun e(tag: String, message: String, error: Throwable? = null) = write(LogLevel.ERROR, tag, message, error)

    fun event(
        name: String,
        domain: String,
        level: LogLevel = LogLevel.INFO,
        context: Map<String, String> = emptyMap(),
    ) = write(level, domain, "[$name]", context = context)

    fun exportFiles(): List<File> = synchronized(lock) {
        pruneLocked(force = true)
        val activeDirectory = directory ?: return emptyList()
        listOf(File(activeDirectory, STANDARD_FILE), File(activeDirectory, JSONL_FILE))
    }

    private fun environmentContext(context: Context): Map<String, String> {
        val appVersion = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        }.getOrDefault("unknown")
        return mapOf(
            "app.version" to appVersion,
            "device.brand" to Build.BRAND,
            "device.manufacturer" to Build.MANUFACTURER,
            "device.model" to Build.MODEL,
            "os.apiLevel" to Build.VERSION.SDK_INT.toString(),
            "os.name" to "Android",
            "os.version" to Build.VERSION.RELEASE,
        )
    }

    private fun write(
        level: LogLevel,
        category: String,
        message: String,
        error: Throwable? = null,
        context: Map<String, String> = emptyMap(),
    ) {
        val safeContext = context.toSortedMap().toMutableMap()
        error?.message?.takeIf(String::isNotBlank)?.let { safeContext["error"] = it }
        val record = Record(
            timestamp = System.currentTimeMillis().toDouble() / 1_000.0,
            sessionId = sessionId,
            processId = Process.myPid(),
            level = level.wireValue,
            category = category,
            message = message,
            context = safeContext.takeIf { it.isNotEmpty() },
        )
        synchronized(lock) {
            appendLocked(record)
        }
        mirrorToLogcat(level, category, message, error)
    }

    private fun appendLocked(record: Record) {
        val activeDirectory = directory ?: return
        runCatching {
            File(activeDirectory, JSONL_FILE).appendText(gson.toJson(record) + "\n")
            File(activeDirectory, STANDARD_FILE).appendText(standardLine(record))
            pruneLocked(force = false)
        }.onFailure { failure ->
            Log.w(TAG, "Unable to persist diagnostic log", failure)
        }
    }

    private fun pruneLocked(force: Boolean) {
        val activeDirectory = directory ?: return
        val jsonFile = File(activeDirectory, JSONL_FILE)
        val now = System.currentTimeMillis()
        if (!force && now - lastPrunedAtMillis < 5 * 60 * 1_000L && jsonFile.length() <= MAXIMUM_FILE_BYTES) return
        val cutoff = now.toDouble() / 1_000.0 - RETENTION_MILLIS.toDouble() / 1_000.0
        val chronologicalRecords = jsonFile.takeIf(File::isFile)
            ?.useLines { lines -> lines.mapNotNull { gson.fromJsonOrNull(it) }.filter { it.timestamp >= cutoff }.toList() }
            .orEmpty()
        val newestFirst = mutableListOf<Record>()
        var retainedBytes = 0L
        chronologicalRecords.asReversed().forEach { item ->
            val bytes = (gson.toJson(item) + "\n").toByteArray().size.toLong()
            if (retainedBytes + bytes <= MAXIMUM_FILE_BYTES || newestFirst.isEmpty()) {
                newestFirst += item
                retainedBytes += bytes
            }
        }
        val retained = newestFirst.asReversed()
        runCatching {
            jsonFile.writeText(retained.joinToString(separator = "") { gson.toJson(it) + "\n" })
            File(activeDirectory, STANDARD_FILE).writeText(retained.joinToString(separator = "") { standardLine(it) })
        }
        lastPrunedAtMillis = now
    }

    private fun Gson.fromJsonOrNull(line: String): Record? = runCatching {
        fromJson(line, Record::class.java)
    }.getOrNull()

    private fun standardLine(record: Record): String {
        val source = record.source?.takeIf { it == "KMP" } ?: "Native"
        val event = record.message.takeIf { it.startsWith("[") && it.endsWith("]") }
        val module = event?.removePrefix("[")?.removeSuffix("]") ?: record.category
        val message = if (event == null) normalized(record.message) else ""
        val level = if (record.level == "info") "" else " level=${record.level}"
        val fields = record.context.orEmpty().toSortedMap().entries.joinToString(" ") { (key, value) ->
            key + "=" + normalized(value)
        }
        val suffix = listOf(message, fields).filter(String::isNotEmpty).joinToString(" ")
        return "[${Instant.ofEpochMilli((record.timestamp * 1_000.0).toLong())}][$source][$module]$level" +
            if (suffix.isEmpty()) "\n" else " $suffix\n"
    }

    private fun normalized(value: String): String = value.replace("\n", "\\n").replace("\r", "\\r")

    private fun mirrorToLogcat(level: LogLevel, tag: String, message: String, error: Throwable?) {
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARNING -> Log.w(tag, message, error)
            LogLevel.ERROR -> Log.e(tag, message, error)
        }
    }

    enum class LogLevel(val wireValue: String) {
        DEBUG("debug"), INFO("info"), WARNING("warning"), ERROR("error"),
    }

    private data class Record(
        @SerializedName("schemaVersion") val schemaVersion: Int = 1,
        val timestamp: Double,
        @SerializedName("sessionID") val sessionId: String,
        @SerializedName("processID") val processId: Int,
        val level: String,
        val category: String,
        val message: String,
        val context: Map<String, String>? = null,
        val source: String? = null,
    )
}
