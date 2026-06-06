package com.aritxonly.deadliner.localutils

import android.os.Build
import com.aritxonly.deadliner.model.ModernColorPalette
import java.util.Locale

object ModernColorPaletteResolver {
    fun resolveCurrentDevice(): ModernColorPalette {
        return resolve(
            brand = Build.BRAND,
            manufacturer = Build.MANUFACTURER,
        )
    }

    fun resolve(
        brand: String?,
        manufacturer: String?,
    ): ModernColorPalette {
        val normalized = buildString {
            append(brand.orEmpty())
            append(' ')
            append(manufacturer.orEmpty())
        }.trim().lowercase(Locale.ROOT)

        return when {
            normalized.contains("honor") ||
                normalized.contains("huawei") -> ModernColorPalette.Honor
            normalized.contains("oppo") ||
                normalized.contains("oneplus") ||
                normalized.contains("realme") ||
                normalized.contains("oplus") -> ModernColorPalette.Oppo
            normalized.contains("vivo") ||
                normalized.contains("iqoo") -> ModernColorPalette.Vivo
            else -> ModernColorPalette.HyperOs
        }
    }
}
