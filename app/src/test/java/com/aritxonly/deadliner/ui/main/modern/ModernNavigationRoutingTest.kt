package com.aritxonly.deadliner.ui.main.modern

import com.aritxonly.deadliner.ui.main.shared.MainSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModernNavigationRoutingTest {
    @Test
    fun lifiHasARealRoundTripNavigationRoute() {
        assertEquals("lifi", mainSectionNavigationKey(MainSection.LIFI))
        assertEquals(MainSection.LIFI, mainSectionForNavigationKey("lifi"))
    }

    @Test
    fun detachedSearchIsNotASectionRoute() {
        assertNull(mainSectionForNavigationKey("search"))
    }
}
