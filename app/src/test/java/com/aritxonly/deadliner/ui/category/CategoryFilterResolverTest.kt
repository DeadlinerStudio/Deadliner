package com.aritxonly.deadliner.ui.category

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryFilterResolverTest {
    private val catalog = setOf("preset.work", "preset.study")

    @Test
    fun empty_selection_means_all() {
        val filter = CategoryFilterResolver.selected(emptySet(), false)

        assertEquals(CategoryFilter.All, filter)
        assertTrue(CategoryFilterResolver.matches(filter, "preset.work", catalog))
        assertTrue(CategoryFilterResolver.matches(filter, null, catalog))
    }

    @Test
    fun selected_categories_and_uncategorized_follow_shared_contract() {
        val filter = CategoryFilterResolver.selected(setOf("preset.work"), includesUncategorized = true)

        assertTrue(CategoryFilterResolver.matches(filter, "preset.work", catalog))
        assertFalse(CategoryFilterResolver.matches(filter, "preset.study", catalog))
        assertTrue(CategoryFilterResolver.matches(filter, null, catalog))
        assertTrue(CategoryFilterResolver.matches(filter, "deleted.category", catalog))
    }

    @Test
    fun known_category_does_not_match_uncategorized_only() {
        val filter = CategoryFilterResolver.selected(emptySet(), includesUncategorized = true)

        assertFalse(CategoryFilterResolver.matches(filter, "preset.work", catalog))
        assertTrue(CategoryFilterResolver.matches(filter, null, catalog))
    }
}
