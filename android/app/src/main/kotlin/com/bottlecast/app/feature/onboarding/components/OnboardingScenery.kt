package com.bottlecast.app.feature.onboarding.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.components.BOARD_WIDTH
import com.bottlecast.app.common.components.MoonDisc
import com.bottlecast.app.common.components.SceneryGlint
import com.bottlecast.app.common.components.SceneryStar
import com.bottlecast.app.common.components.drawGlint
import com.bottlecast.app.common.components.drawStars
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.feature.onboarding.OnboardingDimens

/**
 * Onboarding's sky (spec §3.1, the Onboarding boards): stars, the moon at (72%, 12%) and two still
 * glints, the same behind every page. Page 2's tide ring brings its own moon, so the sky's fades
 * away towards it, by [moonAlpha]. Decorative.
 */
@Composable
internal fun OnboardingScenery(
    haloAlpha: () -> Float,
    moonAlpha: () -> Float,
    modifier: Modifier = Modifier,
) {
    val colors = BottlingTheme.colors
    val moonSize = BottlingTheme.sizes.moonDisc
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics {}
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawStars(Stars, colors)
            val arcY = size.height * OnboardingDimens.dome.fraction
            Glints.forEach { glint -> drawGlint(glint, arcY, phase = 0f, color = colors.sceneryAccent) }
        }
        // measured from the start edge, which offset mirrors, so the moon stays top-end (spec §4)
        Box(
            modifier = Modifier
                .offset(
                    x = maxWidth * OnboardingDimens.MOON_X - moonSize / 2,
                    y = maxHeight * OnboardingDimens.MOON_Y - moonSize / 2,
                )
                .graphicsLayer { alpha = moonAlpha() },
        ) {
            MoonDisc(haloAlpha = haloAlpha)
        }
    }
}

private val Stars = listOf(
    SceneryStar(49f / BOARD_WIDTH, 139.dp, 0.45f),
    SceneryStar(113f / BOARD_WIDTH, 105.dp, 0.30f),
    SceneryStar(229f / BOARD_WIDTH, 71.dp, 0.30f),
    SceneryStar(345f / BOARD_WIDTH, 221.dp, 0.35f),
    SceneryStar(31f / BOARD_WIDTH, 263.dp, 0.30f),
    SceneryStar(319f / BOARD_WIDTH, 331.dp, 0.25f),
)

private val Glints = listOf(
    SceneryGlint(60f / BOARD_WIDTH, 12.dp),
    SceneryGlint(300f / BOARD_WIDTH, 16.dp),
)
