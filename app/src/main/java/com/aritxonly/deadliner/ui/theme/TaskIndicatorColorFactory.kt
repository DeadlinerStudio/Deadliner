package com.aritxonly.deadliner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.foundation.isSystemInDarkTheme
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.ColorfulIndicatorStyle
import com.aritxonly.deadliner.model.DDLStatus
import com.materialkolor.ktx.harmonizeWithPrimary

data class TaskIndicatorColors(
    val indicatorColor: Color,
    val baseBackgroundColor: Color,
    val overlayBackgroundColor: Color,
)

private data class IndicatorAlphaSpec(
    val indicatorAlpha: Float,
    val backgroundAlpha: Float,
)

@Composable
fun rememberTaskIndicatorColors(
    status: DDLStatus,
    colorfulIndicatorEnabled: Boolean = GlobalUtils.presetIndicatorColor,
    colorfulIndicatorStyle: ColorfulIndicatorStyle = GlobalUtils.colorfulIndicatorStyle,
    useDisabledCompletedStyle: Boolean = false,
): TaskIndicatorColors {
    val pureBackgroundColor = pureIndicatorBackgroundColor()

    if (useDisabledCompletedStyle) {
        val alphaSpec = abandonedAlphaSpec()
        val abandonedIndicator = if (
            colorfulIndicatorEnabled && colorfulIndicatorStyle == ColorfulIndicatorStyle.Harmonize
        ) {
            MaterialTheme.colorScheme.harmonizeWithPrimary(Color.Gray)
        } else {
            Color.Gray
        }
        return TaskIndicatorColors(
            indicatorColor = abandonedIndicator.copy(alpha = alphaSpec.indicatorAlpha),
            baseBackgroundColor = pureBackgroundColor,
            overlayBackgroundColor = abandonedIndicator.copy(alpha = alphaSpec.backgroundAlpha),
        )
    }

    if (!colorfulIndicatorEnabled) {
        val alphaSpec = status.alphaSpec()
        return when (status) {
            DDLStatus.UNDERGO -> TaskIndicatorColors(
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = alphaSpec.indicatorAlpha),
                baseBackgroundColor = pureBackgroundColor,
                overlayBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = alphaSpec.backgroundAlpha),
            )

            DDLStatus.NEAR -> TaskIndicatorColors(
                indicatorColor = MaterialTheme.colorScheme.tertiary.copy(alpha = alphaSpec.indicatorAlpha),
                baseBackgroundColor = pureBackgroundColor,
                overlayBackgroundColor = MaterialTheme.colorScheme.tertiary.copy(alpha = alphaSpec.backgroundAlpha),
            )

            DDLStatus.PASSED -> TaskIndicatorColors(
                indicatorColor = MaterialTheme.colorScheme.error.copy(alpha = alphaSpec.indicatorAlpha),
                baseBackgroundColor = pureBackgroundColor,
                overlayBackgroundColor = MaterialTheme.colorScheme.error.copy(alpha = alphaSpec.backgroundAlpha),
            )

            DDLStatus.COMPLETED -> TaskIndicatorColors(
                indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = alphaSpec.indicatorAlpha),
                baseBackgroundColor = pureBackgroundColor,
                overlayBackgroundColor = MaterialTheme.colorScheme.secondary.copy(alpha = alphaSpec.backgroundAlpha),
            )
        }
    }

    return when (colorfulIndicatorStyle) {
        ColorfulIndicatorStyle.Harmonize -> harmonizedTaskIndicatorColors(status)
        ColorfulIndicatorStyle.Morandi -> morandiTaskIndicatorColors(status)
    }
}

@Composable
private fun harmonizedTaskIndicatorColors(
    status: DDLStatus,
): TaskIndicatorColors {
    val pureBackgroundColor = pureIndicatorBackgroundColor()
    val alphaSpec = status.alphaSpec()
    val seed = when (status) {
        DDLStatus.UNDERGO -> Color(0xFF3B82F6)
        DDLStatus.NEAR -> Color(0xFFF59E0B)
        DDLStatus.PASSED -> Color.Red
        DDLStatus.COMPLETED -> Color(0xFF22C55E)
    }
    val harmonized = MaterialTheme.colorScheme.harmonizeWithPrimary(seed)
    return TaskIndicatorColors(
        indicatorColor = harmonized.copy(alpha = alphaSpec.indicatorAlpha),
        baseBackgroundColor = pureBackgroundColor,
        overlayBackgroundColor = harmonized.copy(alpha = alphaSpec.backgroundAlpha),
    )
}

@Composable
private fun morandiTaskIndicatorColors(
    status: DDLStatus,
): TaskIndicatorColors {
    val pureBackgroundColor = pureIndicatorBackgroundColor()
    val alphaSpec = status.alphaSpec()
    return when (status) {
        DDLStatus.UNDERGO -> TaskIndicatorColors(
            indicatorColor = colorResource(R.color.indicator_morandi_undergo).copy(alpha = alphaSpec.indicatorAlpha),
            baseBackgroundColor = pureBackgroundColor,
            overlayBackgroundColor = colorResource(R.color.bg_morandi_undergo).copy(alpha = alphaSpec.backgroundAlpha),
        )

        DDLStatus.NEAR -> TaskIndicatorColors(
            indicatorColor = colorResource(R.color.indicator_morandi_near).copy(alpha = alphaSpec.indicatorAlpha),
            baseBackgroundColor = pureBackgroundColor,
            overlayBackgroundColor = colorResource(R.color.bg_morandi_near).copy(alpha = alphaSpec.backgroundAlpha),
        )

        DDLStatus.PASSED -> TaskIndicatorColors(
            indicatorColor = colorResource(R.color.indicator_morandi_passed).copy(alpha = alphaSpec.indicatorAlpha),
            baseBackgroundColor = pureBackgroundColor,
            overlayBackgroundColor = colorResource(R.color.bg_morandi_passed).copy(alpha = alphaSpec.backgroundAlpha),
        )

        DDLStatus.COMPLETED -> TaskIndicatorColors(
            indicatorColor = colorResource(R.color.indicator_morandi_completed).copy(alpha = alphaSpec.indicatorAlpha),
            baseBackgroundColor = pureBackgroundColor,
            overlayBackgroundColor = colorResource(R.color.bg_morandi_completed).copy(alpha = alphaSpec.backgroundAlpha),
        )
    }
}

private fun DDLStatus.alphaSpec(): IndicatorAlphaSpec = when (this) {
    DDLStatus.UNDERGO -> IndicatorAlphaSpec(indicatorAlpha = 0.55f, backgroundAlpha = 0.18f)
    DDLStatus.NEAR -> IndicatorAlphaSpec(indicatorAlpha = 0.65f, backgroundAlpha = 0.20f)
    DDLStatus.PASSED -> IndicatorAlphaSpec(indicatorAlpha = 0.65f, backgroundAlpha = 0.20f)
    DDLStatus.COMPLETED -> IndicatorAlphaSpec(indicatorAlpha = 0.65f, backgroundAlpha = 0.20f)
}

private fun abandonedAlphaSpec(): IndicatorAlphaSpec {
    return IndicatorAlphaSpec(indicatorAlpha = 0.65f, backgroundAlpha = 0.18f)
}

@Composable
private fun pureIndicatorBackgroundColor(): Color {
    return if (isSystemInDarkTheme()) Color.Black else Color.White
}
