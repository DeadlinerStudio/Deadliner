package com.aritxonly.deadliner.model

enum class DynamicPaletteStyle(
    val key: String,
) {
    System("system"),
    TonalSpot("tonal_spot"),
    Neutral("neutral"),
    Vibrant("vibrant"),
    Expressive("expressive"),
    Rainbow("rainbow"),
    FruitSalad("fruit_salad"),
    Monochrome("monochrome"),
    Fidelity("fidelity"),
    Content("content"),
    ;

    companion object {
        fun fromKey(value: String?): DynamicPaletteStyle {
            return entries.firstOrNull { it.key == value } ?: TonalSpot
        }
    }
}
