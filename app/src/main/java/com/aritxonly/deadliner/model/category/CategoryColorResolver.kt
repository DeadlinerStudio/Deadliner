package com.aritxonly.deadliner.model.category

/** Android-side parser shared by Compose category UI and Glance RemoteViews. */
object CategoryColorResolver {
    const val MIUIX_BLUE_HEX = "#3481FF"

    private const val FALLBACK_ARGB = 0xFF3B82F6L

    private val namedColors = mapOf(
        "blue_500" to 0xFF3B82F6L, "blue_600" to 0xFF2563EBL,
        "indigo_500" to 0xFF6366F1L, "violet_500" to 0xFF8B5CF6L,
        "purple_500" to 0xFFA855F7L, "pink_500" to 0xFFEC4899L,
        "rose_500" to 0xFFF43F5EL, "red_500" to 0xFFEF4444L,
        "orange_500" to 0xFFF97316L, "amber_500" to 0xFFF59E0BL,
        "yellow_500" to 0xFFEAB308L, "lime_500" to 0xFF84CC16L,
        "green_500" to 0xFF22C55EL, "emerald_500" to 0xFF10B981L,
        "teal_500" to 0xFF14B8A6L, "cyan_500" to 0xFF06B6D4L,
        "sky_500" to 0xFF0EA5E9L, "slate_500" to 0xFF64748BL,
        "gray_500" to 0xFF8E8E93L, "gray_900" to 0xFF111827L,
    )

    val selectableHexColors: List<String> = listOf(
        MIUIX_BLUE_HEX,
        "#3B82F6",
        "#2563EB",
        "#6366F1",
        "#8B5CF6",
        "#A855F7",
        "#EC4899",
        "#F43F5E",
        "#EF4444",
        "#F97316",
        "#F59E0B",
        "#EAB308",
        "#84CC16",
        "#22C55E",
        "#10B981",
        "#14B8A6",
        "#06B6D4",
        "#0EA5E9",
        "#64748B",
        "#8E8E93",
        "#111827",
    )

    fun argb(colorTokenOrHex: String): Int {
        val normalized = colorTokenOrHex.trim().lowercase()
        val resolved = namedColors[normalized]
            ?: normalized.removePrefix("#").toLongOrNull(16)
                ?.takeIf { normalized.startsWith("#") && normalized.length == 7 && it <= 0xFFFFFF }
                ?.let { 0xFF000000L or it }
            ?: FALLBACK_ARGB
        return resolved.toInt()
    }
}
