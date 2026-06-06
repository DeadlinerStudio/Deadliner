package com.aritxonly.deadliner.ui.settings

import android.graphics.BitmapFactory
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.SettingsRoute
import com.aritxonly.deadliner.data.UserProfileRepository
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.UserProfile
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier
import com.materialkolor.ktx.harmonizeWithPrimary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsScreen(
    nav: NavController,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val appearance by GlobalUtils.appearanceFlow.collectAsState()
    val profile by UserProfileRepository.profile.collectAsState(initial = UserProfile())
    val avatarPainter: Painter? by remember(profile.avatarFileName) {
        mutableStateOf(
            profile.avatarFileName?.let { name ->
                val file = File(context.filesDir, "avatars/$name")
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.let { BitmapPainter(it.asImageBitmap()) } else null
            }
        )
    }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_title),
        navigationIcon = {
            IconButton(
                onClick = onClose,
                modifier = navIconPaddingModifier
            ) {
                Icon(
                    painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.close),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = expressiveTypeModifier
                )
            }
        }
    ) { innerPadding ->
        SettingsLazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier,
        ) {
            item {
                SettingsFeaturePromoCarousel(
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    onOpenRoute = { route ->
                        GlobalUtils.seenSettingsFeaturePromos =
                            GlobalUtils.seenSettingsFeaturePromos + route.route
                        nav.navigate(route.route)
                    }
                )
            }
            item {
                SettingsSection {
                    SettingItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { nav.navigate(SettingsRoute.Profile.route) },
                        headlineText = if (profile.nickname.isBlank()) stringResource(R.string.edit_profile) else profile.nickname,
                        supportingText = stringResource(R.string.settings_support_profile),
                        leadingContent = {
                            if (avatarPainter != null) {
                                Image(
                                    painter = avatarPainter!!,
                                    contentDescription = stringResource(R.string.avatar),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                SettingsHomeLeadingIcon(
                                    route = SettingsRoute.Profile,
                                    iconRes = R.drawable.ic_person,
                                    enabled = appearance.useSettingsHomepageColoredIcons,
                                )
                            }
                        }
                    )
                }
            }

//            item { ShapeShowcase() }

            SettingsRoute.allSubRoutes.forEach { group ->
                item {
                    SettingsSection {
                        group.forEachIndexed { index, route ->
                            val supportText = (if (route.route == "about") "v${context.getAppVersion()} " else "") +
                                    stringResource(route.supportRes!!)
                            SettingItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { nav.navigate(route.route) },
                                headlineText = stringResource(route.titleRes),
                                supportingText = supportText,
                                leadingContent = {
                                    SettingsHomeLeadingIcon(
                                        route = route,
                                        iconRes = route.iconRes ?: R.drawable.ic_package,
                                        enabled = appearance.useSettingsHomepageColoredIcons,
                                    )
                                }
                            )

                            if (index != group.lastIndex) {
                                SettingsSectionDivider()
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun SettingsHomeLeadingIcon(
    route: SettingsRoute,
    enabled: Boolean,
    iconRes: Int,
) {
    if (!enabled) {
        Icon(
            imageVector = ImageVector.vectorResource(iconRes),
            contentDescription = null,
            tint = LocalContentColor.current,
        )
        return
    }

    val accent = settingsHomeIconTint(route)
    val isDark = isSystemInDarkTheme()
    val containerBase = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surface
    }
    val containerColor = lerp(
        start = containerBase,
        stop = accent,
        fraction = if (isDark) 0.38f else 0.32f,
    )
    val iconTint = lerp(
        start = accent,
        stop = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
        fraction = if (isDark) 0.28f else 0.58f,
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(containerColor)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun settingsHomeIconTint(
    route: SettingsRoute,
): Color {
    val palette = when (route) {
        SettingsRoute.Profile -> SettingsHomeIconPalette(
            seedColor = Color.Blue,
            seedRecovery = 0.28f,
            softenWithWhite = 0.14f,
            settleWithNeutral = 0.02f,
            lightThemeBoost = 0.14f,
        )
        SettingsRoute.Appearance -> SettingsHomeIconPalette(
            seedColor = Color.Magenta,
            seedRecovery = 0.24f,
            softenWithWhite = 0.18f,
            settleWithNeutral = 0.04f,
            lightThemeBoost = 0.04f,
        )
        SettingsRoute.Behavior -> SettingsHomeIconPalette(
            seedColor = Color.Cyan,
            seedRecovery = 0.22f,
            softenWithWhite = 0.12f,
            settleWithNeutral = 0.02f,
            lightThemeBoost = 0.12f,
        )
        SettingsRoute.Notification -> SettingsHomeIconPalette(
            seedColor = lerp(Color.Red, Color.Yellow, 0.62f),
            seedRecovery = 0.32f,
            softenWithWhite = 0.22f,
            settleWithNeutral = 0.02f,
            lightThemeBoost = 0.03f,
        )
        SettingsRoute.Backup -> SettingsHomeIconPalette(
            seedColor = Color.Green,
            seedRecovery = 0.10f,
            softenWithWhite = 0.08f,
            settleWithNeutral = 0.24f,
            lightThemeBoost = 0.02f,
        )
        SettingsRoute.Widget -> SettingsHomeIconPalette(
            seedColor = lerp(Color.Red, Color.Yellow, 0.42f),
            seedRecovery = 0.36f,
            softenWithWhite = 0.20f,
            settleWithNeutral = 0.02f,
            lightThemeBoost = 0.03f,
        )
        SettingsRoute.AI -> SettingsHomeIconPalette(
            seedColor = lerp(Color.Blue, Color.Magenta, 0.34f),
            seedRecovery = 0.26f,
            softenWithWhite = 0.16f,
            settleWithNeutral = 0f,
            lightThemeBoost = 0.08f,
        )
        SettingsRoute.WebDAV -> SettingsHomeIconPalette(
            seedColor = Color.Cyan,
            seedRecovery = 0.24f,
            softenWithWhite = 0.16f,
            settleWithNeutral = 0.04f,
            lightThemeBoost = 0.13f,
        )
        SettingsRoute.Lab -> SettingsHomeIconPalette(
            seedColor = lerp(Color.Red, Color.Magenta, 0.18f),
            seedRecovery = 0.30f,
            softenWithWhite = 0.18f,
            settleWithNeutral = 0.02f,
            lightThemeBoost = 0.03f,
        )
        SettingsRoute.Wiki -> SettingsHomeIconPalette(
            seedColor = Color.Green,
            seedRecovery = 0.12f,
            softenWithWhite = 0.10f,
            settleWithNeutral = 0.20f,
            lightThemeBoost = 0.02f,
        )
        SettingsRoute.Feedback -> SettingsHomeIconPalette(
            seedColor = lerp(Color.Red, Color.Yellow, 0.28f),
            seedRecovery = 0.34f,
            softenWithWhite = 0.24f,
            settleWithNeutral = 0.04f,
            lightThemeBoost = 0.03f,
        )
        SettingsRoute.About -> SettingsHomeIconPalette(
            seedColor = Color.Gray,
            seedRecovery = 0.10f,
            softenWithWhite = 0.08f,
            settleWithNeutral = 0.16f,
            lightThemeBoost = 0f,
        )
        else -> SettingsHomeIconPalette(
            seedColor = Color.Gray,
            seedRecovery = 0.12f,
            softenWithWhite = 0.10f,
            settleWithNeutral = 0.14f,
            lightThemeBoost = 0f,
        )
    }
    val harmonized = MaterialTheme.colorScheme.harmonizeWithPrimary(palette.seedColor)
    val recovered = lerp(harmonized, palette.seedColor, palette.seedRecovery)
    val themeSofteningAdjustment = if (isSystemInDarkTheme()) 0.04f else 0f
    val softened = lerp(
        start = recovered,
        stop = Color.White,
        fraction = (palette.softenWithWhite + themeSofteningAdjustment).coerceIn(0f, 1f),
    )
    val neutralTarget = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val globalSettle = if (isSystemInDarkTheme()) 0.10f else 0.10f
    val globalAlpha = if (isSystemInDarkTheme()) 0.88f else 0.90f

    val settled = lerp(
        start = softened,
        stop = neutralTarget,
        fraction = (palette.settleWithNeutral + globalSettle).coerceIn(0f, 1f),
    )
    val lightAdjusted = if (isSystemInDarkTheme()) {
        settled
    } else {
        lerp(
            start = settled,
            stop = palette.seedColor,
            fraction = palette.lightThemeBoost,
        )
    }
    return lightAdjusted.copy(alpha = globalAlpha)
}

private data class SettingsHomeIconPalette(
    val seedColor: Color,
    val seedRecovery: Float,
    val softenWithWhite: Float,
    val settleWithNeutral: Float,
    val lightThemeBoost: Float,
)

private data class SettingsFeaturePromo(
    val route: SettingsRoute,
    val summaryRes: Int,
)

@Composable
private fun SettingsFeaturePromoCarousel(
    modifier: Modifier = Modifier,
    onOpenRoute: (SettingsRoute) -> Unit,
) {
    val allPromos = remember {
        listOf(
            SettingsFeaturePromo(
                route = SettingsRoute.AppearanceDesign,
                summaryRes = R.string.settings_try_design_color_summary,
            ),
            SettingsFeaturePromo(
                route = SettingsRoute.AppearanceMaterial,
                summaryRes = R.string.settings_try_advanced_material_summary,
            ),
            SettingsFeaturePromo(
                route = SettingsRoute.AppIcon,
                summaryRes = R.string.settings_try_app_icon_summary,
            ),
        )
    }
    val visiblePromos = allPromos.filterNot { promo ->
        promo.route.route in GlobalUtils.seenSettingsFeaturePromos
    }

    if (visiblePromos.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { visiblePromos.size })

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val promo = visiblePromos[page]
            SettingsFeaturePromoCard(
                title = stringResource(
                    R.string.settings_try_feature,
                    stringResource(promo.route.titleRes)
                ),
                summary = stringResource(promo.summaryRes),
                onClick = { onOpenRoute(promo.route) }
            )
        }

        if (visiblePromos.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 12.dp)
            ) {
                repeat(visiblePromos.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(width = if (selected) 18.dp else 8.dp, height = 8.dp)
                            .background(
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsFeaturePromoCard(
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun Context.getAppVersion(): String {
    return try {
        val pInfo = packageManager.getPackageInfo(packageName, 0)
        pInfo.versionName ?: "unknown"
    } catch (e: PackageManager.NameNotFoundException) {
        "unknown"
    }
}
