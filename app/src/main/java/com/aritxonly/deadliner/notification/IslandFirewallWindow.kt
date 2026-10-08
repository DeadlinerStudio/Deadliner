package com.aritxonly.deadliner.notification

import android.os.IBinder
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/** Runs only inside the Shizuku/Root process. No app-wide hidden API exemptions are needed. */
internal class IslandFirewallWindow(
    private val backendFactory: (() -> Any)? = null,
    private val watchdogMillis: Long = 5_000L,
) {
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private var watchdog: ScheduledFuture<*>? = null
    private var blockedUid: Int? = null
    private var backend: Any? = null
    private var lastOperation = "none"
    @Synchronized fun diagnostics() = "processUid=${android.os.Process.myUid()} backend=${backend?.javaClass?.name} chain=9 $lastOperation"

    @Synchronized fun setBlocked(uid: Int, blocked: Boolean): Boolean = try {
        require(uid >= 10_000)
        if (blocked) {
            check(blockedUid == null || blockedUid == uid)
            val target = backend ?: (backendFactory?.invoke() ?: connect()).also { backend = it }
            // Arm before mutating so a failed call or dead client cannot leave a permanent block.
            blockedUid = uid
            watchdog?.cancel(false)
            watchdog = executor.schedule({ restore() }, watchdogMillis, TimeUnit.MILLISECONDS)
            target.javaClass.getMethod("setFirewallChainEnabled", Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType).apply { isAccessible = true }.invoke(target, 9, true)
            rule(target, uid, 2)
        } else {
            rule(backend ?: (backendFactory?.invoke() ?: connect()).also { backend = it }, uid, 0)
            blockedUid = null
            watchdog?.cancel(false)
        }
        lastOperation = "uid=$uid rule=${if (blocked) 2 else 0} accepted=true"
        true
    } catch (error: Exception) {
        val cause = (error as? java.lang.reflect.InvocationTargetException)?.targetException ?: error
        lastOperation = "uid=$uid rule=${if (blocked) 2 else 0} accepted=false error=${cause.javaClass.name}:${cause.message}"
        Log.e("IslandFirewall", "Firewall operation failed", error)
        false
    }

    @Synchronized fun restore(): Boolean {
        val uid = blockedUid ?: return true
        val restored = setBlocked(uid, false)
        if (!restored) watchdog = executor.schedule({ restore() }, 1, TimeUnit.SECONDS)
        return restored
    }

    fun close() { if (restore()) executor.shutdown() }

    private fun rule(target: Any, uid: Int, rule: Int) {
        val method = target.javaClass.methods.first {
            it.name in listOf("setUidFirewallRule", "setFirewallUidRule") && it.parameterTypes.contentEquals(
                arrayOf(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType))
        }
        method.isAccessible = true
        method.invoke(target, 9, uid, rule)
    }

    private fun connect(): Any {
        val manager = Class.forName("android.os.ServiceManager")
        val get = manager.getMethod("getService", String::class.java)
        var failure: Exception? = null
        for ((name, stub) in listOf("connectivity" to "android.net.IConnectivityManager\$Stub",
            "network_management" to "android.os.INetworkManagementService\$Stub")) {
            try {
                val binder = get.invoke(null, name) as IBinder
                val target = requireNotNull(Class.forName(stub).getMethod("asInterface", IBinder::class.java).invoke(null, binder))
                check(target.javaClass.methods.any { it.name == "setFirewallChainEnabled" })
                check(target.javaClass.methods.any { it.name in listOf("setUidFirewallRule", "setFirewallUidRule") && it.parameterCount == 3 })
                return target
            } catch (error: Exception) { failure = error }
        }
        throw IllegalStateException("No compatible OEM firewall backend", failure)
    }
}
