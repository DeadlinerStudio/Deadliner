package com.aritxonly.deadliner.ui.main.modern

import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.ui.main.shared.MainSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModernListChromePolicyTest {
    @Test
    fun onlyTheNormalTaskListCanCollapseTheTitleBar() {
        assertTrue(
            canCollapseModernListTopBar(
                MainSection.LIST,
                DeadlineType.TASK,
                selectionMode = false,
                searchActive = false,
            ),
        )
        assertFalse(
            canCollapseModernListTopBar(
                MainSection.LIST,
                DeadlineType.HABIT,
                selectionMode = false,
                searchActive = false,
            ),
        )
        assertFalse(
            canCollapseModernListTopBar(
                MainSection.LIST,
                DeadlineType.TASK,
                selectionMode = true,
                searchActive = false,
            ),
        )
    }

    @Test
    fun searchEntryRequiresModernImmersiveCompactTaskList() {
        assertTrue(
            shouldShowImmersiveListSearchEntry(
                usesMiuixTheme = true,
                useImmersiveExperience = true,
                isCompactFloatingNavigation = true,
                selectedSection = MainSection.LIST,
                selectedPage = DeadlineType.TASK,
                selectionMode = false,
            ),
        )
        assertFalse(
            shouldShowImmersiveListSearchEntry(
                usesMiuixTheme = false,
                useImmersiveExperience = true,
                isCompactFloatingNavigation = true,
                selectedSection = MainSection.LIST,
                selectedPage = DeadlineType.TASK,
                selectionMode = false,
            ),
        )
        assertFalse(
            shouldShowImmersiveListSearchEntry(
                usesMiuixTheme = true,
                useImmersiveExperience = true,
                isCompactFloatingNavigation = true,
                selectedSection = MainSection.LIST,
                selectedPage = DeadlineType.HABIT,
                selectionMode = false,
            ),
        )
    }

    @Test
    fun activeSearchClearsThePrimaryCapsuleSelection() {
        assertEquals(
            "search",
            modernNavigationSelectedKey(
                section = MainSection.LIST,
                searchActive = true,
            ),
        )
    }
}
