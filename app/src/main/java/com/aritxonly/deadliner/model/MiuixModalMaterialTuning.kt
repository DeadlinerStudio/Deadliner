package com.aritxonly.deadliner.model

enum class MiuixModalKind {
    Dialog,
    BottomSheet,
}

data class MiuixModalMaterialTuning(
    val backgroundAlpha: Float,
    val blurRadiusDp: Float,
) {
    companion object {
        const val MinBackgroundAlpha = 0.20f
        const val MaxBackgroundAlpha = 0.95f
        const val MinBlurRadiusDp = 0f
        const val MaxBlurRadiusDp = 150f

        const val DialogDefaultBackgroundAlpha = 0.65f
        const val DialogDefaultBlurRadiusDp = 80f
        const val BottomSheetDefaultBackgroundAlpha = 0.70f
        const val BottomSheetDefaultBlurRadiusDp = 80f
    }
}

fun AdvancedMaterialFineTuning.miuixModalMaterial(
    kind: MiuixModalKind,
): MiuixModalMaterialTuning {
    val tuning = normalized()
    return when (kind) {
        MiuixModalKind.Dialog -> MiuixModalMaterialTuning(
            backgroundAlpha = tuning.dialogBackgroundAlpha,
            blurRadiusDp = tuning.dialogBlurRadiusDp,
        )

        MiuixModalKind.BottomSheet -> MiuixModalMaterialTuning(
            backgroundAlpha = tuning.bottomSheetBackgroundAlpha,
            blurRadiusDp = tuning.bottomSheetBlurRadiusDp,
        )
    }
}
