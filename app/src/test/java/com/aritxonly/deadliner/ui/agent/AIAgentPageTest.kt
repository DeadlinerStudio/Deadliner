package com.aritxonly.deadliner.ui.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AIAgentPageTest {
    @Test
    fun overlayOwnsDismissChromeAndBackHandling() {
        val policy = aiAgentChromePolicy(AIAgentPresentation.Overlay)

        assertTrue(policy.showDismissButton)
        assertTrue(policy.handleBackPress)
    }

    @Test
    fun pageLeavesDismissAndBackHandlingToNavigationHost() {
        val policy = aiAgentChromePolicy(AIAgentPresentation.Page)

        assertFalse(policy.showDismissButton)
        assertFalse(policy.handleBackPress)
    }

    @Test
    fun inputOnlyNeedsExtraLiftWhenNavigationOverlaysContent() {
        assertEquals(
            AIAgentPageDefaults.OverlayNavigationInputPadding,
            AIAgentPageDefaults.inputBottomPadding(navigationOverlaysContent = true),
        )
        assertEquals(
            AIAgentPageDefaults.InsetNavigationInputPadding,
            AIAgentPageDefaults.inputBottomPadding(navigationOverlaysContent = false),
        )
    }
}
