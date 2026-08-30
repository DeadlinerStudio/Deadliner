package com.aritxonly.deadliner.ui.category

import androidx.compose.ui.graphics.Color
import com.aritxonly.deadliner.R
import com.deadliner.domain.category.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryPresentationRegistryTest {
    @Test
    fun documented_color_token_has_stable_android_color() {
        assertEquals(Color(0xFF3B82F6), CategoryPresentationRegistry.color("blue_500"))
        assertEquals(Color(0xFF8B5CF6), CategoryPresentationRegistry.color("#8B5CF6"))
    }

    @Test
    fun unknown_business_primitives_fall_back_to_displayable_values() {
        assertEquals(R.drawable.msr_bookmark_wght400, CategoryPresentationRegistry.icon("new-platform-icon"))
        assertEquals(Color(0xFF3B82F6), CategoryPresentationRegistry.color("not-a-color"))
    }

    @Test
    fun historical_ios_icon_keys_render_as_their_shared_semantic_icons() {
        val category = Category(
            uid = "preset.work",
            name = "工作",
            iconKey = "briefcase.fill",
            colorHex = "#3B82F6",
            isPreset = true,
            sortOrder = 10,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
        )

        assertEquals("briefcase", category.toPresentation().iconSemanticId)
        assertEquals(R.drawable.msr_work_wght400, CategoryPresentationRegistry.icon(category.toPresentation().iconSemanticId))
    }
}
