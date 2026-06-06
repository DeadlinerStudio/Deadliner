package com.aritxonly.deadliner.model

enum class ColorfulIndicatorStyle(
    val key: String,
) {
    Harmonize("harmonize"),
    Morandi("morandi"),
    ;

    companion object {
        fun fromKey(value: String?): ColorfulIndicatorStyle {
            return entries.firstOrNull { it.key == value } ?: Harmonize
        }
    }
}
