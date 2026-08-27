package com.aritxonly.deadliner.ui.main.modern

import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.ui.main.shared.MainSection

internal fun canCollapseModernListTopBar(
    selectedSection: MainSection,
    selectedPage: DeadlineType,
    selectionMode: Boolean,
    searchActive: Boolean,
): Boolean = selectedSection == MainSection.LIST &&
    selectedPage == DeadlineType.TASK &&
    !selectionMode &&
    !searchActive

internal fun shouldShowImmersiveListSearchEntry(
    usesMiuixTheme: Boolean,
    useImmersiveExperience: Boolean,
    isCompactFloatingNavigation: Boolean,
    selectedSection: MainSection,
    selectedPage: DeadlineType,
    selectionMode: Boolean,
): Boolean = usesMiuixTheme &&
    useImmersiveExperience &&
    isCompactFloatingNavigation &&
    selectedSection == MainSection.LIST &&
    selectedPage == DeadlineType.TASK &&
    !selectionMode

internal fun modernNavigationSelectedKey(
    section: MainSection,
    searchActive: Boolean,
): String = if (searchActive) {
    "search"
} else {
    mainSectionNavigationKey(section)
}
