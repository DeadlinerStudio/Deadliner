package com.aritxonly.deadliner.notification

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35], application = android.app.Application::class)
class IslandFirewallWindowTest {
    class Backend(private val failBlock: Boolean = false) {
        val restored = CountDownLatch(1)
        val chainEnables = mutableListOf<Pair<Int, Boolean>>()
        @Volatile var lastRule = -1
        @Volatile var lastUid = -1
        fun setFirewallChainEnabled(chain: Int, enabled: Boolean) { chainEnables.add(chain to enabled) }
        fun setUidFirewallRule(chain: Int, uid: Int, rule: Int) {
            require(chain == 9)
            lastRule = rule; lastUid = uid
            if (rule == 2 && failBlock) throw IllegalStateException("Rejected")
            if (rule == 0) restored.countDown()
        }
    }

    @Test fun watchdogRestoresNetworkingWithoutAClientCallback() {
        val backend = Backend()
        val window = IslandFirewallWindow({ backend }, watchdogMillis = 20)
        try {
            assertTrue(window.setBlocked(10042, true))
            assertTrue(backend.restored.await(2, TimeUnit.SECONDS))
            assertEquals(0, backend.lastRule)
            assertEquals(10042, backend.lastUid)
            assertEquals(listOf(9 to true), backend.chainEnables)
        } finally { window.close() }
    }

    @Test fun failedBlockStillHasAnArmedRecovery() {
        val backend = Backend(failBlock = true)
        val window = IslandFirewallWindow({ backend }, watchdogMillis = 20)
        try {
            assertFalse(window.setBlocked(10042, true))
            assertTrue(backend.restored.await(2, TimeUnit.SECONDS))
            assertEquals(0, backend.lastRule)
        } finally { window.close() }
    }

    @Test fun closingPrivilegedServiceRestoresOnlyTargetUid() {
        val backend = Backend()
        val window = IslandFirewallWindow({ backend })
        assertTrue(window.setBlocked(10042, true))
        window.close()
        assertEquals(0, backend.lastRule)
        assertEquals(10042, backend.lastUid)
        assertEquals(listOf(9 to true), backend.chainEnables)
    }
}
