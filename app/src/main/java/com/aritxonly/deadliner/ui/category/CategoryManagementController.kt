package com.aritxonly.deadliner.ui.category

import com.aritxonly.deadliner.model.category.CategoryColorResolver
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.domain.category.Category
import com.deadliner.domain.category.CategoryMutation
import com.deadliner.domain.category.CategoryMutationRequest
import java.time.Instant
import java.util.UUID

internal data class CategoryEditorDraft(
    val name: String = "",
    val iconSemanticId: String = CategoryMutation.DEFAULT_ICON_KEY,
    val colorHex: String = CategoryColorResolver.MIUIX_BLUE_HEX,
)

internal fun CategoryPresentation.toEditorDraft() = CategoryEditorDraft(
    name = name,
    iconSemanticId = iconSemanticId,
    colorHex = colorToken,
)

internal class CategoryManagementController(
    private val loadCategories: () -> List<Category> = { KmpV6Runtime.database.categories.list() },
    private val createCategory: (Category) -> Unit = { KmpV6Runtime.database.categories.create(it) },
    private val updateCategory: (Category) -> Unit = { KmpV6Runtime.database.categories.update(it) },
    private val deleteCategory: (String, String) -> Unit = { uid, updatedAt ->
        KmpV6Runtime.database.categories.softDelete(uid, updatedAt)
    },
    private val nowProvider: () -> String = { Instant.now().toString() },
    private val uidProvider: () -> String = { "category.${UUID.randomUUID()}" },
) {
    fun load(): List<CategoryPresentation> = loadCategories()
        .filterNot { it.isDeleted }
        .sortedBy { it.sortOrder }
        .map { it.toPresentation() }

    fun save(editingUid: String?, draft: CategoryEditorDraft): Result<CategoryPresentation> = runCatching {
        val request = CategoryMutationRequest(
            uid = editingUid,
            name = draft.name,
            iconKey = draft.iconSemanticId,
            colorHex = draft.colorHex,
        )
        val now = nowProvider()
        val category = if (editingUid == null) {
            CategoryMutation.create(
                request = request,
                uid = uidProvider(),
                sortOrder = nextSortOrder(loadCategories()),
                now = now,
            ).also(createCategory)
        } else {
            val existing = requireNotNull(loadCategories().firstOrNull { it.uid == editingUid && !it.isDeleted }) {
                "category not found"
            }
            CategoryMutation.update(existing, request, now).also(updateCategory)
        }
        category.toPresentation()
    }

    fun delete(uid: String): Result<Unit> = runCatching {
        require(loadCategories().any { it.uid == uid && !it.isDeleted }) { "category not found" }
        deleteCategory(uid, nowProvider())
    }

    companion object {
        internal fun nextSortOrder(categories: List<Category>): Int =
            (categories.asSequence().filterNot { it.isDeleted }.maxOfOrNull { it.sortOrder } ?: 0) + 10
    }
}
