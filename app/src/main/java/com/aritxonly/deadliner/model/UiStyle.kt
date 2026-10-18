package com.aritxonly.deadliner.model

/** Legacy style keys, including "simplified", migrate to the single supported home. */
enum class UiStyle(val key: String) {
    Miuix("miuix");

    companion object {
        @Suppress("UNUSED_PARAMETER")
        fun fromKey(k: String?): UiStyle = Miuix
    }
}
