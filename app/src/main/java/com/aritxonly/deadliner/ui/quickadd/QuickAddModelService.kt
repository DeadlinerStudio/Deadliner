package com.aritxonly.deadliner.ui.quickadd

import android.content.Context
import com.deadliner.domain.quickadd.QuickAddProposal
import com.deadliner.domain.quickadd.model.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.ZoneId

/** Process-lifetime model owner. Downloading is driven by opening quick add, never by typing. */
internal object QuickAddModelService {
    private val mutex = Mutex()
    private val mutableReady = MutableStateFlow(false)
    val ready = mutableReady.asStateFlow()
    private val mutableStatus = MutableStateFlow(QuickAddModelStatus(QuickAddModelState.RULES_ONLY))
    val status = mutableStatus.asStateFlow()
    @Volatile private var session: QuickAddModelSession? = null

    suspend fun prepare(context: Context) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val active = session ?: run {
                    val config = try { context.assets.open(if (context.resources.configuration.locales[0].language == "en") "quick-add-model-en.json" else "quick-add-model.json").bufferedReader().use { it.readText() } }
                    catch (_: java.io.FileNotFoundException) { "" }
                    QuickAddModelSession(context.filesDir.absolutePath, "android", config).also { session = it }
                }
                coroutineScope {
                    val polling = launch {
                        while (isActive) {
                            mutableStatus.value = Json.decodeFromString(active.statusJson())
                            delay(200)
                        }
                    }
                    try { mutableReady.value = active.prepare() }
                    finally {
                        polling.cancel()
                        mutableStatus.value = Json.decodeFromString(active.statusJson())
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                mutableReady.value = false
                mutableStatus.value = QuickAddModelStatus(QuickAddModelState.RULES_ONLY, phase = "FAILED")
            }
        }
    }

    suspend fun recognize(text: String, kind: String): QuickAddProposal? {
        val active = session ?: return null
        if (!mutableReady.value) return null
        val result = active.recognize(text, kind, ZoneId.systemDefault().id)
        mutableStatus.value = Json.decodeFromString(active.statusJson())
        return result
    }
}
