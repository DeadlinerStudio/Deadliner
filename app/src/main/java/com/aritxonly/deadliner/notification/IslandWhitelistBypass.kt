package com.aritxonly.deadliner.notification

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku

internal enum class IslandBypassMode { OFF, SHIZUKU, ROOT, SYSTEM_HOOK }

/** All notifications share one short window, preventing overlapping restore/block operations. */
internal object IslandWhitelistBypass {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val mutableStatus = MutableStateFlow<String?>(null)
    val status = mutableStatus.asStateFlow()
    private var connection: ServiceConnection? = null
    private var firewall: IIslandFirewall? = null
    private var connectedMode = IslandBypassMode.OFF
    private var pendingUid: Int? = null
    private val pendingReplays = linkedMapOf<String, (Boolean) -> Unit>()
    private val replaySignal = Channel<Context>(Channel.CONFLATED)
    private var retryAfterMillis = 0L

    init {
        scope.launch {
            for (context in replaySignal) {
                val callbacks = synchronized(pendingReplays) {
                    pendingReplays.values.toList().also { pendingReplays.clear() }
                }
                if (callbacks.isNotEmpty()) replay(context) { accepted -> callbacks.forEach { it(accepted) } }
            }
        }
    }

    fun mode(context: Context): IslandBypassMode = runCatching {
        IslandBypassMode.valueOf(context.getSharedPreferences("island_bypass", 0).getString("mode", "OFF")!!)
    }.getOrDefault(IslandBypassMode.OFF)

    fun setMode(context: Context, mode: IslandBypassMode) {
        NotificationDiagnostics.event("island.mode", "mode" to mode)
        context.getSharedPreferences("island_bypass", 0).edit().putString("mode", mode.name).apply()
        restore(context, disconnect = true)
    }

    fun requestShizukuPermission() {
        runCatching {
            check(Shizuku.pingBinder()) { "Shizuku is not running" }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) Shizuku.requestPermission(52002)
        }.onFailure { mutableStatus.value = it.message; NotificationDiagnostics.failure("island.failed", it) }
    }

    fun authorize(context: Context) {
        scope.launch { mutex.withLock {
            runCatching { connect(context.applicationContext, mode(context)) }
                .onSuccess { retryAfterMillis = 0L; mutableStatus.value = "Connected"; NotificationDiagnostics.event("island.connected", "mode" to connectedMode) }
                .onFailure { mutableStatus.value = it.message; NotificationDiagnostics.failure("island.failed", it) }
        } }
    }

    /** Coalesces timer ticks and batches independent cards in one short network window. */
    fun publish(context: Context, key: String, onPublication: ((Boolean) -> Unit)? = null, publish: () -> Unit) {
        val app = context.applicationContext
        if (mode(app) in listOf(IslandBypassMode.OFF, IslandBypassMode.SYSTEM_HOOK) || !LiveUpdateNotificationAdapter.supportsIsland()) {
            publish()
            onPublication?.invoke(true)
            return
        }
        NotificationDiagnostics.event("island.queued", "key" to key, "mode" to mode(app))
        synchronized(pendingReplays) { pendingReplays[key] = { accepted -> publish(); onPublication?.invoke(accepted) } }
        replaySignal.trySend(app)
    }

    private suspend fun replay(app: Context, publish: (Boolean) -> Unit) {
        mutex.withLock {
            if (mode(app) in listOf(IslandBypassMode.OFF, IslandBypassMode.SYSTEM_HOOK) || android.os.SystemClock.elapsedRealtime() < retryAfterMillis) {
                NotificationDiagnostics.event("island.replay.skipped", "reason" to "disabled_or_retry_cooldown")
                withContext(Dispatchers.Main) { publish(mode(app) in listOf(IslandBypassMode.OFF, IslandBypassMode.SYSTEM_HOOK)) }
                return@withLock
            }
            NotificationDiagnostics.event("island.replay.begin", "mode" to mode(app))
            var remote: IIslandFirewall? = null
            var uid: Int? = null
            var posted = false
            var armed = false
            try {
                uid = app.packageManager.getPackageUid("com.xiaomi.xmsf", 0)
                remote = connect(app, mode(app))
                if (mode(app) in listOf(IslandBypassMode.OFF, IslandBypassMode.SYSTEM_HOOK)) {
                    withContext(Dispatchers.Main) { publish(true) }
                    posted = true
                    return@withLock
                }
                pendingUid = uid
                app.getSharedPreferences("island_bypass", 0).edit().putInt("pending_uid", uid)
                    .putString("pending_mode", connectedMode.name).commit()
                armed = true
                NotificationDiagnostics.event("island.block.requested", "xmsfUid" to uid)
                val blocked = remote.setBlocked(uid, true)
                NotificationDiagnostics.event("island.firewall.result", "blocked" to blocked, "details" to remote.diagnostics())
                check(blocked) { "Firewall backend rejected the bypass" }
                withContext(Dispatchers.Main) { publish(true) }
                posted = true
                NotificationDiagnostics.event("island.replay.published", "xmsfUid" to uid)
                delay(100)
            } catch (error: Exception) {
                mutableStatus.value = error.message
                NotificationDiagnostics.failure("island.replay.failed", error)
                retryAfterMillis = android.os.SystemClock.elapsedRealtime() + 10_000L
                if (!posted) withContext(NonCancellable + Dispatchers.Main) { publish(false) }
            } finally {
                withContext(NonCancellable) {
                    if (armed && uid != null && remote != null) {
                        if (runCatching {
                            remote.setBlocked(uid, false).also { restored ->
                                NotificationDiagnostics.event("island.firewall.restore", "restored" to restored, "details" to remote.diagnostics())
                            }
                        }.getOrDefault(false)) {
                            clearPending(app); NotificationDiagnostics.event("island.network.restored", "xmsfUid" to uid)
                        }
                        else { mutableStatus.value = "Restore pending; use Restore network"; NotificationDiagnostics.event("island.network.restore_failed", "xmsfUid" to uid) }
                    }
                }
            }
        }
    }

    fun recoverPending(context: Context) {
        if (context.getSharedPreferences("island_bypass", 0).contains("pending_uid")) restore(context)
    }

    fun restore(context: Context, disconnect: Boolean = false) {
        val app = context.applicationContext
        scope.launch { mutex.withLock {
            runCatching {
                val uid = pendingUid ?: app.getSharedPreferences("island_bypass", 0).getInt("pending_uid", -1)
                if (uid >= 10_000) {
                    val pendingMode = runCatching { IslandBypassMode.valueOf(app.getSharedPreferences("island_bypass", 0)
                        .getString("pending_mode", mode(app).name)!!) }.getOrDefault(mode(app))
                    val remote = firewall ?: connect(app, pendingMode)
                    check(remote.setBlocked(uid, false)) { "Network restore failed" }
                    clearPending(app)
                }
                mutableStatus.value = "Restored"
                if (disconnect) withContext(Dispatchers.Main) {
                    connection?.let {
                        if (connectedMode == IslandBypassMode.ROOT) RootService.unbind(it)
                        else Shizuku.unbindUserService(args(app), it, true)
                    }
                    connection = null; firewall = null; connectedMode = IslandBypassMode.OFF
                }
            }.onFailure { mutableStatus.value = it.message; NotificationDiagnostics.failure("island.failed", it) }
        } }
    }

    private fun clearPending(context: Context) {
        pendingUid = null
        context.getSharedPreferences("island_bypass", 0).edit().remove("pending_uid").remove("pending_mode").apply()
    }

    private fun args(context: Context) = Shizuku.UserServiceArgs(ComponentName(context, IslandShizukuService::class.java))
        .daemon(true).processNameSuffix("island_bypass").version(2)

    private suspend fun connect(context: Context, mode: IslandBypassMode): IIslandFirewall {
        NotificationDiagnostics.event("island.connect.requested", "mode" to mode)
        check(mode == IslandBypassMode.ROOT || mode == IslandBypassMode.SHIZUKU) { "Choose Shizuku or Root first" }
        firewall?.takeIf { connectedMode == mode && it.asBinder().isBinderAlive }?.let { return it }
        if (mode == IslandBypassMode.SHIZUKU) {
            check(Shizuku.pingBinder()) { "Shizuku is not running" }
            check(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) { "Shizuku permission required" }
        }
        val ready = CompletableDeferred<IIslandFirewall>()
        val callback = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                ready.complete(IIslandFirewall.Stub.asInterface(binder))
            }
            override fun onServiceDisconnected(name: ComponentName) { firewall = null; NotificationDiagnostics.event("island.disconnected", "backend" to name.className) }
        }
        withContext(Dispatchers.Main) {
            if (mode == IslandBypassMode.ROOT) RootService.bind(Intent(context, IslandRootService::class.java), callback)
            else Shizuku.bindUserService(args(context), callback)
        }
        try {
            return withTimeout(15_000) { ready.await() }.also {
                firewall = it; connection = callback; connectedMode = mode
            }
        } catch (error: Exception) {
            withContext(NonCancellable + Dispatchers.Main) {
                runCatching {
                    if (mode == IslandBypassMode.ROOT) RootService.unbind(callback)
                    else Shizuku.unbindUserService(args(context), callback, true)
                }
            }
            throw error
        }
    }
}
