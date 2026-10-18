package com.aritxonly.deadliner.ui.settings

import org.junit.Assert.*
import org.junit.Test
import top.yukonga.miuix.kmp.nav.core.navBackStackOf

class SettingsNavigatorTest {
    @Test fun repeatedDestinationsDoNotCreateDuplicateMiuixContentKeys() {
        val stack = navBackStackOf(SettingsPage("main"))
        val nav = SettingsNavigator(stack)
        nav.navigate("appearance")
        nav.navigate("design")
        nav.navigate("appearance")
        assertEquals(2, stack.size)
        assertEquals("appearance", nav.currentRoute)
        nav.navigate("appearance")
        assertEquals(2, stack.size)
        assertTrue(nav.navigateUp())
        assertFalse(nav.navigateUp())
        assertEquals("main", nav.currentRoute)
    }
}
