package com.bottlecast.app.common.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * The few shadows and glows the design uses, read through [BottlingTheme.effects]. Night is tonal
 * only (background → surface → surfaceHigh); its one exception is the parchment's moonlit rim and
 * glow. On Dawn cards get a 1dp shadow and the parchment a 2dp one (spec §1.4).
 */
@Immutable
data class BottlingEffects(
    /** Elevation for cards on surfaces. */
    val cardElevation: Dp,
    /** The glow drawn behind the parchment card, outside its edge. */
    val parchmentGlow: Shadow,
    /** The moon's halo layers, listed like CSS box-shadows: the first is drawn on top. */
    val moonHalo: List<Shadow>,
)

/**
 * The boards give blurs as CSS box-shadow radii (a Gaussian with σ = blur / 2), while Compose's
 * shadows blur by radius (σ ≈ 0.577 × radius on Android), so a CSS blur maps to a smaller radius.
 */
internal fun cssBlur(blur: Dp): Dp = blur * CSS_BLUR_TO_RADIUS

private const val CSS_BLUR_TO_RADIUS = 0.866f

// box-shadow values from TEMPLATE.md (parchment, moon) and spec §1.6
internal val NightBottlingEffects = BottlingEffects(
    cardElevation = 0.dp,
    parchmentGlow = Shadow(radius = cssBlur(24.dp), color = NightBottlingColors.glass.copy(alpha = 0.10f)),
    moonHalo = listOf(
        Shadow(radius = 0.dp, color = NightBottlingColors.moon.copy(alpha = 0.06f), spread = 10.dp),
        Shadow(radius = cssBlur(48.dp), color = NightBottlingColors.moon.copy(alpha = 0.18f), spread = 12.dp),
    ),
)

internal val DawnBottlingEffects = BottlingEffects(
    cardElevation = 1.dp,
    parchmentGlow = Shadow(
        radius = cssBlur(6.dp),
        color = DawnBottlingColors.ink.copy(alpha = 0.12f),
        offset = DpOffset(0.dp, 2.dp),
    ),
    moonHalo = listOf(
        Shadow(radius = cssBlur(40.dp), color = DawnBottlingColors.moon.copy(alpha = 0.12f), spread = 10.dp),
    ),
)

internal val LocalBottlingEffects = staticCompositionLocalOf<BottlingEffects> {
    error("No BottlingEffects provided: wrap the content in BottlingTheme")
}
