package com.aritxonly.deadliner.model

/** Stable local preference keys; shader/material parameters are independent of these experiments. */
enum class GlassShadowPreset(val key: String) {
    BalancedDiffuse("balanced_diffuse"),
    Clean("clean"),
    Original("original"),
    Diffuse("diffuse"),
    Compact("compact"),
    Crisp("crisp"),
    Airy("airy"),
    CloseFloat("close_float"),
    Elevated("elevated"),
    SoftContact("soft_contact"),
    Minimal("minimal");

    companion object {
        val Default = BalancedDiffuse
        fun fromKey(key: String?): GlassShadowPreset = entries.firstOrNull { it.key == key } ?: Default
    }
}
