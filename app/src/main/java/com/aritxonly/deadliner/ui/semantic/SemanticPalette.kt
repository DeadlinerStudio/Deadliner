package com.aritxonly.deadliner.ui.semantic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import com.aritxonly.deadliner.model.DDLStatus
import com.aritxonly.deadliner.model.semantic.SemanticTone
import com.aritxonly.deadliner.ui.theme.rememberTaskIndicatorColors

data class SemanticAtmospherePalette(
    val mainColor: Color,
    val bloomColor: Color,
    val orbColor: Color,
)

enum class SemanticAtmosphereModule {
    OVERVIEW,
    CAPTURE,
}

internal fun resolveSemanticModuleColor(
    module: SemanticAtmosphereModule,
    darkTheme: Boolean,
): Color = when (module) {
    SemanticAtmosphereModule.OVERVIEW -> if (darkTheme) Color(0xFF41D1C7) else Color(0xFF0FA6A6)
    SemanticAtmosphereModule.CAPTURE -> if (darkTheme) Color(0xFF8E8BFF) else Color(0xFF5856D6)
}

internal data class SemanticProgressColorRole(
    val status: DDLStatus,
    val useDisabledStyle: Boolean = false,
)

internal fun SemanticTone.progressColorRole(): SemanticProgressColorRole = when (this) {
    SemanticTone.SUCCESS -> SemanticProgressColorRole(DDLStatus.COMPLETED)
    SemanticTone.WARNING -> SemanticProgressColorRole(DDLStatus.NEAR)
    SemanticTone.DANGER -> SemanticProgressColorRole(DDLStatus.PASSED)
    SemanticTone.NEUTRAL -> SemanticProgressColorRole(
        status = DDLStatus.COMPLETED,
        useDisabledStyle = true,
    )

    SemanticTone.ACCENT,
    SemanticTone.CALM -> SemanticProgressColorRole(DDLStatus.UNDERGO)
}

@Composable
fun rememberSemanticAtmospherePalette(
    tone: SemanticTone,
    module: SemanticAtmosphereModule? = null,
): SemanticAtmospherePalette {
    val darkTheme = isSystemInDarkTheme()
    if (module != null) {
        val moduleColor = resolveSemanticModuleColor(module, darkTheme)
        return remember(moduleColor) {
            SemanticAtmospherePalette(
                mainColor = moduleColor,
                bloomColor = moduleColor,
                orbColor = Color.White,
            )
        }
    }
    val role = tone.progressColorRole()
    val mainColors = rememberTaskIndicatorColors(
        status = role.status,
        useDisabledCompletedStyle = role.useDisabledStyle,
    )
    val successColors = rememberTaskIndicatorColors(status = DDLStatus.COMPLETED)
    val main = mainColors.indicatorColor.copy(alpha = 1f)
    val bloom = if (tone == SemanticTone.CALM) {
        successColors.indicatorColor.copy(alpha = 1f)
    } else {
        main
    }
    return remember(main, bloom) {
        SemanticAtmospherePalette(
            mainColor = main,
            bloomColor = bloom,
            orbColor = Color.White,
        )
    }
}
