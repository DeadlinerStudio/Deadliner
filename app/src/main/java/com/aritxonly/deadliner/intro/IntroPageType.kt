package com.aritxonly.deadliner.intro

import com.aritxonly.deadliner.model.UiStyle

sealed interface IntroPageType {
    val stableId: Long

    data object Cover : IntroPageType {
        override val stableId: Long = 1L
    }

    data object Permissions : IntroPageType {
        override val stableId: Long = 2L
    }

    data object Design : IntroPageType {
        override val stableId: Long = 3L
    }

    data object Color : IntroPageType {
        override val stableId: Long = 4L
    }

    data object AdvancedMaterial : IntroPageType {
        override val stableId: Long = 5L
    }

    data object UiStyleChoice : IntroPageType {
        override val stableId: Long = 6L
    }

    data class GuideScene(val sceneId: String) : IntroPageType {
        override val stableId: Long = 1_000L + sceneId.hashCode().toLong()
    }

    data object Final : IntroPageType {
        override val stableId: Long = 9_999L
    }

    companion object {
        fun createPages(style: UiStyle): List<IntroPageType> = buildList {
            add(Cover)
            add(Permissions)
            add(Design)
            add(Color)
            add(AdvancedMaterial)
            add(UiStyleChoice)
            addAll(
                IntroGuideScenes.forStyle(style).map { scene ->
                    GuideScene(scene.id)
                }
            )
            add(Final)
        }
    }
}
