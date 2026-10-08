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
import java.util.concurrent.Executors
import com.deadliner.platform.PlatformLogSink

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
    private val writer = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "deadliner-diagnostics").apply { isDaemon = true }
    }
    private val gson = Gson()
    private val sessionId = UUID.randomUUID().toString().lowercase()
    @Volatile private var directory: File? = null
    private var lastPrunedAtMillis = 0L
    private var exportDirectory: File? = null

    fun initialize(context: Context) {
        synchronized(lock) {
            if (directory != null) return
            directory = File(context.filesDir, DIRECTORY)
            exportDirectory = File(context.cacheDir, "DiagnosticExports")
            // Core submits to this same queue so pruning never races its appends.
            PlatformLogSink.configureFileWriter { json, text ->
                writer.execute { appendLines(json, text) }
            }
            writer.execute {
                runCatching {
                    directory?.mkdirs()
                    val expiredBefore = System.currentTimeMillis() - 24 * 60 * 60 * 1_000L
                    exportDirectory?.listFiles()?.filter { it.lastModified() < expiredBefore }
                        ?.forEach { it.deleteRecursively() }
                    pruneLocked(force = true)
                }.onFailure { Log.w(TAG, "Unable to clean diagnostic history", it) }
            }
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

    /** Blocking export barrier; UI callers run this on Dispatchers.IO. */
    fun exportFiles(): List<File> = writer.submit<List<File>> {
        val activeDirectory = directory ?: return@submit emptyList()
        pruneLocked(force = false)
        // A unique snapshot remains stable while notifications continue logging.
        val snapshot = File(exportDirectory, UUID.randomUUID().toString())
        snapshot.mkdirs()
        listOf(STANDARD_FILE, JSONL_FILE).map { name ->
            File(activeDirectory, name).copyTo(File(snapshot, name), overwrite = true)
        }
    }.get()

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
        writer.execute { appendLines(gson.toJson(record) + "\n", standardLine(record)) }
        mirrorToLogcat(level, category, message, error)
    }

    private fun appendLines(json: String, text: String) {
        val activeDirectory = directory ?: return
        runCatching {
            File(activeDirectory, JSONL_FILE).appendText(json)
            File(activeDirectory, STANDARD_FILE).appendText(text)
            pruneLocked(force = false)
        }.onFailure { failure ->
            Log.w(TAG, "Unable to persist diagnostic log", failure)
        }
    }

    private fun pruneLocked(force: Boolean) {
        val activeDirectory = directory ?: return
        val jsonFile = File(activeDirectory, JSONL_FILE)
        val textFile = File(activeDirectory, STANDARD_FILE)
        val now = System.currentTimeMillis()
        if (!force && now - lastPrunedAtMillis < 5 * 60 * 1_000L &&
            maxOf(jsonFile.length(), textFile.length()) <= MAXIMUM_FILE_BYTES) return
        val cutoff = (now - RETENTION_MILLIS).toDouble() / 1_000.0
        val targetBytes = MAXIMUM_FILE_BYTES * 3 / 4
        val retained = ArrayDeque<RetainedLine>()
        var retainedJsonBytes = 0L
        var retainedTextBytes = 0L
        // Parse each line once; never materialize or reserialize the entire history.
        jsonFile.takeIf(File::isFile)?.useLines { lines ->
            lines.forEach { line ->
                val record = gson.fromJsonOrNull(line) ?: return@forEach
                if (record.timestamp < cutoff) return@forEach
                val json = line + "\n"
                val text = runCatching { standardLine(record) }.getOrNull() ?: return@forEach
                val entry = RetainedLine(json, text, json.toByteArray().size, text.toByteArray().size)
                if (maxOf(entry.jsonBytes, entry.textBytes) > targetBytes) return@forEach
                retained.addLast(entry)
                retainedJsonBytes += entry.jsonBytes
                retainedTextBytes += entry.textBytes
                while (maxOf(retainedJsonBytes, retainedTextBytes) > targetBytes) {
                    val removed = retained.removeFirst()
                    retainedJsonBytes -= removed.jsonBytes
                    retainedTextBytes -= removed.textBytes
                }
            }
        }
        jsonFile.bufferedWriter().use { output -> retained.forEach { output.write(it.json) } }
        textFile.bufferedWriter().use { output -> retained.forEach { output.write(it.text) } }
        lastPrunedAtMillis = now
    }

    private data class RetainedLine(val json: String, val text: String, val jsonBytes: Int, val textBytes: Int)

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
