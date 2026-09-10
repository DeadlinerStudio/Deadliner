package com.aritxonly.deadliner.model

/**
 * Determines how the MIUIX surface/background roles are generated.
 *
 * This is intentionally a color strategy, not a second component system. Both entries feed
 * the same MIUIX component tree.
 */
enum class BackgroundColorStrategy(val key: String) {
    MiuixHyperOs("miuix_hyperos"),
    MaterialMonet("material_monet"),
    ;

    companion object {
        fun fromKey(value: String?): BackgroundColorStrategy? =
            entries.firstOrNull { it.key == value }

        fun resolvePersisted(
            value: String?,
            legacyUsedMaterialTheme: Boolean,
        ): BackgroundColorStrategy =
            fromKey(value) ?: if (legacyUsedMaterialTheme) MaterialMonet else MiuixHyperOs
    }
}
