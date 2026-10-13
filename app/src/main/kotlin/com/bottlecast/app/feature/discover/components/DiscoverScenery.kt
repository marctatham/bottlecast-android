package com.bottlecast.app.feature.discover.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.components.BOARD_WIDTH
import com.bottlecast.app.common.components.DomeEdge
import com.bottlecast.app.common.components.MoonDisc
import com.bottlecast.app.common.components.SceneryGlint
import com.bottlecast.app.common.components.SceneryStar
import com.bottlecast.app.common.components.drawGlint
import com.bottlecast.app.common.components.drawStars
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.feature.discover.DiscoverDimens

/**
 * Discover's scenery on the expanded dome (Discover-Idle board): its stars, the moon at the top end
 * and three glints on the sea below the arc. Decorative, so TalkBack skips it.
 */
@Composable
internal fun DiscoverScenery(
    insets: PaddingValues,
    motion: DiscoverMotion,
    modifier: Modifier = Modifier,
) {
    val colors = BottlingTheme.colors
    val layoutDirection = LocalLayoutDirection.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics {}
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawStars(Stars, colors)
            val arcY = size.height * DomeEdge.Expanded.fraction
            Glints.forEach { glint -> drawGlint(glint, arcY, motion.glintPhase(), colors.sceneryAccent) }
        }
        MoonDisc(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = insets.calculateTopPadding() + DiscoverDimens.moonTop,
                    end = insets.calculateEndPadding(layoutDirection) + DiscoverDimens.moonEnd,
                ),
            haloAlpha = motion.haloAlpha,
        )
    }
}

// the board's 2×2dp stars, by their centres
private val Stars = listOf(
    SceneryStar(35f / BOARD_WIDTH, 133.dp, 0.45f),
    SceneryStar(97f / BOARD_WIDTH, 79.dp, 0.30f),
    SceneryStar(359f / BOARD_WIDTH, 237.dp, 0.35f),
    SceneryStar(25f / BOARD_WIDTH, 353.dp, 0.30f),
    SceneryStar(363f / BOARD_WIDTH, 421.dp, 0.30f),
)

private val Glints = listOf(
    SceneryGlint(64f / BOARD_WIDTH, 12.dp),
    SceneryGlint(180f / BOARD_WIDTH, 8.dp),
    SceneryGlint(296f / BOARD_WIDTH, 16.dp),
)
