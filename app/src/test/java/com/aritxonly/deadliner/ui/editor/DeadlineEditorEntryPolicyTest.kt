package com.aritxonly.deadliner.ui.editor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeadlineEditorEntryPolicyTest {
    @Test fun onlyExplicitExternalEmptyAddMayUseQuickAdd() {
        assertTrue(useQuickEditorEntry(true, true, false, false, false, false))
        assertFalse(useQuickEditorEntry(true, false, false, false, false, false)) // In-page full editor.
        assertFalse(useQuickEditorEntry(false, true, false, false, false, false))
    }

    @Test fun editingGeneratedPrefilledAndExpandedRoutesAlwaysUseFullSheet() {
        assertFalse(useQuickEditorEntry(true, true, true, false, false, false))
        assertFalse(useQuickEditorEntry(true, true, false, true, false, false))
        assertFalse(useQuickEditorEntry(true, true, false, false, true, false))
        assertFalse(useQuickEditorEntry(true, true, false, false, false, true)) // Even an empty raw-text proposal.
    }
}
