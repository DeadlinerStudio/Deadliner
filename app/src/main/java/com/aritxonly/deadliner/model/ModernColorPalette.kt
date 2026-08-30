package com.aritxonly.deadliner.model

data class ModernColorSeedSet(
    val surfaceHex: String,
    val surfaceContainerHex: String,
    val searchBarHex: String,
    val textHex: String,
    val textSecondaryHex: String,
)

enum class ModernColorPalette(
    val key: String,
    val light: ModernColorSeedSet? = null,
    val dark: ModernColorSeedSet? = null,
    val previewLight: ModernColorSeedSet,
) {
    HyperOs(
        key = "hyperos",
        previewLight = ModernColorSeedSet(
            surfaceHex = "ffffff",
            surfaceContainerHex = "ffffff",
            searchBarHex = "ececec",
            textHex = "191919",
            textSecondaryHex = "6f6f6f",
        ),
    ),
    Honor(
        key = "honor",
        light = ModernColorSeedSet(
            surfaceHex = "f2f3f7",
            surfaceContainerHex = "ffffff",
            searchBarHex = "e5e7e9",
            textHex = "191919",
            textSecondaryHex = "676767",
        ),
        dark = ModernColorSeedSet(
            surfaceHex = "000000",
            surfaceContainerHex = "202022",
            searchBarHex = "333333",
            textHex = "e0e0e0",
            textSecondaryHex = "a6a6a6",
        ),
        previewLight = ModernColorSeedSet(
            surfaceHex = "f2f3f7",
            surfaceContainerHex = "ffffff",
            searchBarHex = "e5e7e9",
            textHex = "191919",
            textSecondaryHex = "676767",
        ),
    ),
    Oppo(
        key = "oppo",
        light = ModernColorSeedSet(
            surfaceHex = "eef2f3",
            surfaceContainerHex = "ffffff",
            searchBarHex = "dbdfe0",
            textHex = "191919",
            textSecondaryHex = "767676",
        ),
        dark = ModernColorSeedSet(
            surfaceHex = "000000",
            surfaceContainerHex = "1a1a1a",
            searchBarHex = "1a1a1a",
            textHex = "e9e9e9",
            textSecondaryHex = "969696",
        ),
        previewLight = ModernColorSeedSet(
            surfaceHex = "eef2f3",
            surfaceContainerHex = "ffffff",
            searchBarHex = "dbdfe0",
            textHex = "191919",
            textSecondaryHex = "767676",
        ),
    ),
    Vivo(
        key = "vivo",
        light = ModernColorSeedSet(
            surfaceHex = "f2f2f4",
            surfaceContainerHex = "ffffff",
            searchBarHex = "e6e6e8",
            textHex = "191919",
            textSecondaryHex = "767676",
        ),
        dark = ModernColorSeedSet(
            surfaceHex = "000000",
            surfaceContainerHex = "202020",
            searchBarHex = "262626",
            textHex = "e9e9e9",
            textSecondaryHex = "afafaf",
        ),
        previewLight = ModernColorSeedSet(
            surfaceHex = "f2f2f4",
            surfaceContainerHex = "ffffff",
            searchBarHex = "e6e6e8",
            textHex = "191919",
            textSecondaryHex = "767676",
        ),
    );

    companion object {
        fun fromKey(value: String?): ModernColorPalette = entries.firstOrNull { it.key == value } ?: HyperOs
    }
}
