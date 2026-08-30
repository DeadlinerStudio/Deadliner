package com.aritxonly.deadliner.ui.settings

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.aritxonly.deadliner.BuildConfig
import com.aritxonly.deadliner.R
import androidx.core.net.toUri
import androidx.core.graphics.ColorUtils
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutSettingsScreen(
    nav: NavHostController,
    navigateUp: () -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val collapseDistance = with(LocalDensity.current) { 320.dp.toPx() }
    val scrollProgress by remember(scrollState, collapseDistance) {
        derivedStateOf { (scrollState.value / collapseDistance).coerceIn(0f, 1f) }
    }
    val sectionAlpha = 0.5f + scrollProgress * 0.5f

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_about),
        navigationIcon = {
            IconButton(onClick = navigateUp, modifier = navIconPaddingModifier) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = expressiveTypeModifier,
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AboutFloatingBackground(
                modifier = Modifier.matchParentSize(),
                alpha = 1f - scrollProgress,
            )
            SettingsScrollColumn(
                contentPadding = innerPadding,
                scrollState = scrollState,
            ) {
                AboutAppHeader(
                    appName = stringResource(R.string.app_name),
                    version = context.getAppVersion(),
                )

                SettingsSection(
                    topLabel = stringResource(R.string.settings_highlight),
                    containerAlpha = sectionAlpha,
                ) {
                    SettingItem(
                        headlineText = stringResource(R.string.settings_version),
                        supportingText = "v${context.getAppVersion()}",
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .fillMaxWidth(),
                        trailingContent = null,
                    )

                    SettingsSectionDivider()

                    SettingItem(
                        headlineText = stringResource(R.string.settings_compile_date),
                        supportingText = BuildConfig.BUILD_TIME,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .fillMaxWidth(),
                        trailingContent = null,
                    )
                }

                SettingsSection(containerAlpha = sectionAlpha) {
                    SettingsDetailTextButtonItem(
                        headline = R.string.settings_check_for_updates,
                        supporting = R.string.settings_support_check_for_updates,
                        iconRes = R.drawable.ic_update,
                    ) {
                        nav.navigate("update")
                    }
                }

                SettingsSection(
                    topLabel = stringResource(R.string.settings_donate),
                    containerAlpha = sectionAlpha,
                ) {
                    SettingsDetailTextButtonItem(
                        headline = R.string.settings_donate_author,
                        supporting = R.string.settings_support_donate,
                    ) {
                        nav.navigate("donate")
                    }
                }

                SettingsSection(
                    topLabel = stringResource(R.string.settings_legal),
                    containerAlpha = sectionAlpha,
                ) {
                    SettingsDetailTextButtonItem(
                        headline = R.string.settings_license,
                        supporting = R.string.settings_license_summary,
                        iconRes = R.drawable.ic_license,
                    ) {
                        nav.navigate("license")
                    }
                    SettingsSectionDivider()
                    SettingsDetailTextButtonItem(
                        headline = R.string.settings_privacy_policy,
                        supporting = R.string.settings_privacy_summary,
                        iconRes = R.drawable.ic_privacy,
                    ) {
                        nav.navigate("policy")
                    }
                }

                SettingsSection(
                    topLabel = stringResource(R.string.settings_more),
                    containerAlpha = sectionAlpha,
                ) {
                    SettingsTextButtonItem(
                        text = R.string.settings_homepage,
                        iconRes = R.drawable.ic_author,
                    ) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/AritxOnly".toUri()))
                    }
                    SettingsSectionDivider()
                    SettingsTextButtonItem(
                        text = R.string.settings_github,
                        iconRes = R.drawable.ic_github,
                    ) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, "https://github.com/AritxOnly/Deadliner".toUri()),
                        )
                    }
                    SettingsSectionDivider()
                    SettingsTextButtonItem(
                        text = R.string.settings_playground,
                        iconRes = R.drawable.ic_android,
                    ) {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://www.magicalapk.com/app/share/app?id=55830".toUri(),
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AboutAppHeader(
    appName: String,
    version: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(252.dp)
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp)),
            )
        }
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(top = 2.dp),
        ) {
            Text(
                text = appName,
                style = MaterialTheme.typography.headlineLargeEmphasized,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(1.dp))
            Box(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(3.dp),
            ) {
                Text(
                    text = stringResource(R.string.legacy),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        Text(
            text = "v$version",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun AboutFloatingBackground(
    modifier: Modifier = Modifier,
    alpha: Float,
) {
    val transition = rememberInfiniteTransition(label = "aboutFloatingBackground")
    val horizontalOffset = transition.animateFloat(
        initialValue = -0.12f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7_500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aboutBackgroundHorizontalOffset",
    )
    val verticalOffset = transition.animateFloat(
        initialValue = 0.08f,
        targetValue = -0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5_600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aboutBackgroundVerticalOffset",
    )
    val accentOffset = transition.animateFloat(
        initialValue = -0.08f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6_400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aboutBackgroundAccentOffset",
    )
    val primary = MaterialTheme.colorScheme.primary.toVividGlowColor()
    val secondary = MaterialTheme.colorScheme.secondary.toVividGlowColor()
    val tertiary = MaterialTheme.colorScheme.tertiary.toVividGlowColor()
    val surface = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier) {
        drawRect(color = surface)
        if (alpha > 0f) {
            val glowRadius = maxOf(size.width, size.height) * 0.9f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.22f * alpha), Color.Transparent),
                    center = Offset(
                        x = size.width * (0.18f + horizontalOffset.value),
                        y = size.height * (0.22f + verticalOffset.value),
                    ),
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = Offset(
                    x = size.width * (0.18f + horizontalOffset.value),
                    y = size.height * (0.22f + verticalOffset.value),
                ),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondary.copy(alpha = 0.18f * alpha), Color.Transparent),
                    center = Offset(
                        x = size.width * (0.86f - horizontalOffset.value),
                        y = size.height * (0.66f - verticalOffset.value),
                    ),
                    radius = glowRadius,
                ),
                radius = glowRadius * 0.86f,
                center = Offset(
                    x = size.width * (0.86f - horizontalOffset.value),
                    y = size.height * (0.66f - verticalOffset.value),
                ),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tertiary.copy(alpha = 0.16f * alpha), Color.Transparent),
                    center = Offset(
                        x = size.width * (0.52f + accentOffset.value),
                        y = size.height * (0.96f - accentOffset.value),
                    ),
                    radius = glowRadius * 0.7f,
                ),
                radius = glowRadius * 0.72f,
                center = Offset(
                    x = size.width * (0.52f + accentOffset.value),
                    y = size.height * (0.96f - accentOffset.value),
                ),
            )
        }
    }
}

private fun Color.toVividGlowColor(): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    hsl[1] = (hsl[1] * 1.55f).coerceIn(0.56f, 0.92f)
    hsl[2] = if (hsl[2] < 0.5f) {
        (hsl[2] + 0.14f).coerceAtMost(0.68f)
    } else {
        (hsl[2] - 0.06f).coerceAtLeast(0.36f)
    }
    return Color(ColorUtils.HSLToColor(hsl))
}
