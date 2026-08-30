package com.aritxonly.deadliner.ui.category

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.aritxonly.deadliner.R
import com.deadliner.domain.category.Category
import com.deadliner.domain.category.CategoryMutation

/** Android rendering adapter for the cross-platform category business primitives. */
data class CategoryPresentation(
    val uid: String,
    val name: String,
    val iconSemanticId: String,
    val colorToken: String,
    val isPreset: Boolean,
    val sortOrder: Int,
)

fun Category.toPresentation() = CategoryPresentation(
    uid = uid,
    name = name,
    // Render older locally cached snapshots correctly before their next normalized sync write.
    iconSemanticId = CategoryMutation.normalizedIconKey(iconKey),
    colorToken = colorHex,
    isPreset = isPreset,
    sortOrder = sortOrder,
)

object CategoryPresentationRegistry {
    private const val FallbackIcon = "tag"
    private const val FallbackColor = "blue_500"

    private val colors = mapOf(
        "blue_500" to 0xFF3B82F6, "blue_600" to 0xFF2563EB,
        "indigo_500" to 0xFF6366F1, "violet_500" to 0xFF8B5CF6,
        "purple_500" to 0xFFA855F7, "pink_500" to 0xFFEC4899,
        "rose_500" to 0xFFF43F5E, "red_500" to 0xFFEF4444,
        "orange_500" to 0xFFF97316, "amber_500" to 0xFFF59E0B,
        "yellow_500" to 0xFFEAB308, "lime_500" to 0xFF84CC16,
        "green_500" to 0xFF22C55E, "emerald_500" to 0xFF10B981,
        "teal_500" to 0xFF14B8A6, "cyan_500" to 0xFF06B6D4,
        "sky_500" to 0xFF0EA5E9, "slate_500" to 0xFF64748B,
        "gray_500" to 0xFF8E8E93, "gray_900" to 0xFF111827,
    )

    // Each entry mirrors the documented semantic primitive. Resource names remain Android-only.
    private val icons = mapOf(
        "tag" to R.drawable.msr_bookmark_wght400,
        "folder" to R.drawable.msr_folder_wght400,
        "inbox_tray" to R.drawable.msr_inbox_wght400,
        "briefcase" to R.drawable.msr_work_wght400,
        "office_building" to R.drawable.msr_apartment_wght400,
        "book" to R.drawable.msr_book_2_wght400,
        "education" to R.drawable.msr_school_wght400,
        "edit" to R.drawable.msr_border_color_wght400,
        "home" to R.drawable.msr_house_wght400,
        "shopping" to R.drawable.msr_shopping_cart_wght400,
        "dining" to R.drawable.msr_restaurant_wght400,
        "coffee" to R.drawable.msr_coffee_wght400,
        "health" to R.drawable.msr_favorite_wght400,
        "medical" to R.drawable.msr_medical_services_wght400,
        "running" to R.drawable.msr_directions_run_wght400,
        "fitness" to R.drawable.msr_exercise_wght400,
        "payment_card" to R.drawable.msr_credit_card_wght400,
        "cash" to R.drawable.msr_payments_wght400,
        "analytics" to R.drawable.msr_chart_data_wght400,
        "group" to R.drawable.msr_group_wght400,
        "family" to R.drawable.msr_family_group_wght400,
        "creativity" to R.drawable.msr_brush_wght400,
        "camera" to R.drawable.msr_photo_camera_wght400,
        "music" to R.drawable.msr_music_note_2_wght400,
        "reading" to R.drawable.msr_book_wght400,
        "inspiration" to R.drawable.msr_stars_2_wght400,
        "calendar" to R.drawable.msr_calendar_month_wght400,
        "time" to R.drawable.msr_schedule_wght400,
        "flag" to R.drawable.msr_flag_2_wght400,
        "idea" to R.drawable.msr_lightbulb_wght400,
        "travel" to R.drawable.msr_globe_wght400,
        "flight" to R.drawable.msr_flight_land_wght400,
        "gaming" to R.drawable.msr_stadia_controller_wght400,
        "tools" to R.drawable.msr_handyman_wght400,
        "construction" to R.drawable.msr_hardware_wght400,
        "nature" to R.drawable.msr_eco_wght400,
    )

    @DrawableRes fun icon(iconSemanticId: String): Int = icons[iconSemanticId] ?: icons.getValue(FallbackIcon)

    fun color(colorTokenOrHex: String): Color {
        val normalized = colorTokenOrHex.trim().lowercase()
        colors[normalized]?.let { return Color(it) }
        return normalized.removePrefix("#").toLongOrNull(16)
            ?.takeIf { it <= 0xFFFFFF }
            ?.let { Color(0xFF000000 or it) }
            ?: Color(colors.getValue(FallbackColor))
    }
}

sealed interface CategoryFilter {
    data object All : CategoryFilter
    data class Selected(
        val categoryUids: Set<String>,
        val includesUncategorized: Boolean,
    ) : CategoryFilter
}

object CategoryFilterResolver {
    fun matches(filter: CategoryFilter, categoryUid: String?, availableCategoryUids: Set<String>): Boolean = when (filter) {
        CategoryFilter.All -> true
        is CategoryFilter.Selected -> when {
            categoryUid.isNullOrBlank() -> filter.includesUncategorized
            categoryUid !in availableCategoryUids -> filter.includesUncategorized
            else -> categoryUid in filter.categoryUids
        }
    }

    fun selected(categoryUids: Set<String>, includesUncategorized: Boolean): CategoryFilter =
        if (categoryUids.isEmpty() && !includesUncategorized) CategoryFilter.All
        else CategoryFilter.Selected(categoryUids, includesUncategorized)
}
