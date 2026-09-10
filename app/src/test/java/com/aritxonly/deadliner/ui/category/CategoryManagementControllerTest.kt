package com.aritxonly.deadliner.ui.category

import com.deadliner.domain.category.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryManagementControllerTest {
    @Test
    fun `create normalizes input and appends after active categories`() {
        val stored = mutableListOf(
            category("first", 10),
            category("deleted", 90, deleted = true),
        )
        val controller = CategoryManagementController(
            loadCategories = { stored.toList() },
            createCategory = { stored += it },
            updateCategory = { error("unexpected update") },
            nowProvider = { "2026-09-02T00:00:00Z" },
            uidProvider = { "category.generated" },
        )

        val result = controller.save(
            editingUid = null,
            draft = CategoryEditorDraft("  项目  ", "briefcase", "#3481ff"),
        ).getOrThrow()

        assertEquals("category.generated", result.uid)
        assertEquals("项目", result.name)
        assertEquals(20, result.sortOrder)
        assertEquals("#3481FF", result.colorToken)
        assertFalse(result.isPreset)
    }

    @Test
    fun `edit preserves identity preset and ordering metadata`() {
        val stored = mutableListOf(category("preset.work", 40, preset = true))
        val controller = CategoryManagementController(
            loadCategories = { stored.toList() },
            createCategory = { error("unexpected create") },
            updateCategory = { updated -> stored[0] = updated },
            nowProvider = { "2026-09-02T00:00:00Z" },
            uidProvider = { error("unexpected uid") },
        )

        val result = controller.save(
            editingUid = "preset.work",
            draft = CategoryEditorDraft("新工作", "office_building", "#10B981"),
        ).getOrThrow()

        assertEquals("preset.work", result.uid)
        assertEquals(40, result.sortOrder)
        assertTrue(result.isPreset)
        assertEquals("新工作", stored.single().name)
        assertEquals("2020-01-01T00:00:00Z", stored.single().createdAt)
    }

    @Test
    fun `blank category name fails without writing`() {
        var writes = 0
        val controller = CategoryManagementController(
            loadCategories = { emptyList() },
            createCategory = { writes++ },
            updateCategory = { writes++ },
        )

        assertTrue(controller.save(null, CategoryEditorDraft(name = "   ")).isFailure)
        assertEquals(0, writes)
    }

    @Test
    fun `delete delegates to repository soft delete with current timestamp`() {
        val stored = mutableListOf(category("category.work", 10))
        var deletedUid: String? = null
        var deletedAt: String? = null
        val controller = CategoryManagementController(
            loadCategories = { stored.toList() },
            deleteCategory = { uid, updatedAt ->
                deletedUid = uid
                deletedAt = updatedAt
            },
            nowProvider = { "2026-09-03T08:00:00Z" },
        )

        controller.delete("category.work").getOrThrow()

        assertEquals("category.work", deletedUid)
        assertEquals("2026-09-03T08:00:00Z", deletedAt)
    }

    @Test
    fun `delete rejects a missing or already deleted category`() {
        var deletes = 0
        val controller = CategoryManagementController(
            loadCategories = { listOf(category("category.deleted", 10, deleted = true)) },
            deleteCategory = { _, _ -> deletes++ },
        )

        assertTrue(controller.delete("category.missing").isFailure)
        assertTrue(controller.delete("category.deleted").isFailure)
        assertEquals(0, deletes)
    }

    private fun category(
        uid: String,
        order: Int,
        preset: Boolean = false,
        deleted: Boolean = false,
    ) = Category(
        uid = uid,
        name = uid,
        iconKey = "folder",
        colorHex = "#3B82F6",
        isPreset = preset,
        sortOrder = order,
        createdAt = "2020-01-01T00:00:00Z",
        updatedAt = "2020-01-01T00:00:00Z",
        isDeleted = deleted,
    )
}
