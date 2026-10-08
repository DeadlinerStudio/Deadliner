package com.aritxonly.deadliner.notification

import android.content.Intent
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService

/** Public no-argument constructor is used by Shizuku's app_process launcher. */
class IslandShizukuService : IIslandFirewall.Stub() {
    private val window = IslandFirewallWindow()
    override fun setBlocked(uid: Int, blocked: Boolean) = window.setBlocked(uid, blocked)
    override fun diagnostics() = window.diagnostics()
    override fun destroy() {
        if (window.restore()) { window.close(); kotlin.system.exitProcess(0) }
    }
}

class IslandRootService : RootService() {
    private val window = IslandFirewallWindow()
    private val binder = object : IIslandFirewall.Stub() {
        override fun setBlocked(uid: Int, blocked: Boolean) = window.setBlocked(uid, blocked)
        override fun diagnostics() = window.diagnostics()
        override fun destroy() { window.close() }
    }
    override fun onBind(intent: Intent): IBinder = binder
    override fun onDestroy() { window.close() }
}
