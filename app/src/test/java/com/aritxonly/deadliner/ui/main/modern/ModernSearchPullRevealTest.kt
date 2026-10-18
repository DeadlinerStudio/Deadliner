package com.aritxonly.deadliner.ui.main.modern

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ModernSearchPullRevealTest {
    @get:Rule val compose = createComposeRule()

    @Test fun searchStaysHiddenUntilUserPullsPastTheListTop() {
        lateinit var connection: NestedScrollConnection
        val search = ModernListSearchCollapseState(60f)
        compose.setContent {
            val bar = MiuixScrollBehavior(rememberTopAppBarState(-80f, 0f, 0f),
                snapAnimationSpec = null, flingAnimationSpec = null)
            connection = rememberModernListChromeScrollConnection(search, bar, true, true)
        }
        compose.runOnIdle {
            assertTrue(search.isCollapsed)
            // A downward drag still consumed by the list must never reveal search.
            connection.onPreScroll(Offset(0f, 100f), NestedScrollSource.UserInput)
            connection.onPostScroll(Offset(0f, 100f), Offset.Zero, NestedScrollSource.UserInput)
            assertTrue(search.isCollapsed)
            // Inertial overscroll at the boundary must not reveal it either.
            connection.onPostScroll(Offset.Zero, Offset(0f, 100f), NestedScrollSource.SideEffect)
            assertTrue(search.isCollapsed)
            // Only leftover touch pull at the top can expose the slot.
            connection.onPostScroll(Offset.Zero, Offset(0f, 30f), NestedScrollSource.UserInput)
            assertEquals(.5f, search.expandedFraction, .001f)
            connection.onPostScroll(Offset.Zero, Offset(0f, 40f), NestedScrollSource.UserInput)
            assertTrue(search.isExpanded)
            connection.onPreScroll(Offset(0f, -60f), NestedScrollSource.UserInput)
            assertTrue(search.isCollapsed)
        }
    }
    @Test fun resizingDoesNotExposeAHiddenSearchSlot() {
        val search = ModernListSearchCollapseState(60f)
        search.updateMaximumCollapsePx(90f)
        assertTrue(search.isCollapsed)
        assertEquals(0f, search.expandedFraction, .001f)
        search.dispatchScroll(30f)
        search.updateMaximumCollapsePx(120f)
        assertEquals(1f / 3f, search.expandedFraction, .001f)
        search.snapToCollapsed()
        assertTrue(search.isCollapsed)
    }
}
