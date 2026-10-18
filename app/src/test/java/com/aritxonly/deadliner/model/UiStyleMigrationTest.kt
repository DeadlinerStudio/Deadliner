package com.aritxonly.deadliner.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UiStyleMigrationTest {
    @Test fun existingAndUnknownPreferencesAlwaysResolveToModernHome() {
        for (key in listOf(null, "simplified", "miuix", "classic", "unknown")) {
            assertEquals(UiStyle.Miuix, UiStyle.fromKey(key))
        }
    }
}
