package com.aritxonly.deadliner.ui.picker

internal enum class PickerLayout { CombinedRow, Steps }

internal fun pickerLayout(widthDp: Int, heightDp: Int, fontScale: Float): PickerLayout {
    if (fontScale > 1.35f) return PickerLayout.Steps
    val width = widthDp / fontScale
    val height = heightDp / fontScale
    return when {
        width >= 600 && height >= 480 -> PickerLayout.CombinedRow
        else -> PickerLayout.Steps
    }
}

/** Called synchronously by drag and animation frames, never by a delayed snapshot observer. */
internal class PickerValuePublisher(initialValue: Int) {
    private var published = initialValue
    fun reset(value: Int) { published = value }
    fun publish(value: Int, onValueChange: (Int) -> Unit): Boolean {
        if (value == published) return false
        published = value
        onValueChange(value)
        return true
    }
}
