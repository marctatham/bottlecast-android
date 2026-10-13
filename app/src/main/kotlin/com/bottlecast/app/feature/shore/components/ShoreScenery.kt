package com.bottlecast.app.feature.shore.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.components.DomeEdge
import com.bottlecast.app.common.components.BOARD_WIDTH
import com.bottlecast.app.common.components.SceneryGlint
import com.bottlecast.app.common.components.SceneryStar
import com.bottlecast.app.common.components.drawGlint
import com.bottlecast.app.common.components.drawStars
import com.bottlecast.app.common.components.DomeGlowTopEnd
import com.bottlecast.app.common.components.DomeScaffold
import com.bottlecast.app.common.components.MoonDisc
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.BreathingTide
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.feature.shore.ShoreDimens

/**
 * Which of the Shore's two scenes to draw (spec §3.3).
 *
 * @param glowCenter where the dome's moonlight is brightest.
 */
internal enum class ShoreScene(val glowCenter: Offset) {
    /** State A: the moon sits top-end, with the moonlight behind it, and glints drift on the sea. */
    BottleWaiting(DomeGlowTopEnd),

    /**
     * State B: the moon moves into the tide ring, so the scenery leaves it out, and the moonlight
     * sits behind the ring (Shore-B board).
     */
    FoundToday(Offset(0.5f, 0.6f)),
}

/**
 * The Shore's scenery on the dome (spec §1.5, §1.6): a few stars, and in state A the moon and
 * three drifting glints. It's decorative, so TalkBack skips it.
 *
 * @param insets the status bar and cutouts; the boards place the moon from below the status bar.
 * @param largeText leaves the glints out, since they'd cross the stats once large text pushes them
 *   up to the arc.
 * @param moon false when the hero brings a moon of its own.
 */
@Composable
internal fun ShoreScenery(
    scene: ShoreScene,
    tide: BreathingTide,
    insets: PaddingValues,
    largeText: Boolean,
    modifier: Modifier = Modifier,
    moon: Boolean = true,
) {
    val colors = BottlingTheme.colors
    val layoutDirection = LocalLayoutDirection.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics {}
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawStars(if (scene == ShoreScene.BottleWaiting) WaitingStars else FoundStars, colors)
            if (scene == ShoreScene.BottleWaiting && !largeText) {
                val arcY = size.height * DomeEdge.Full.fraction
                Glints.forEach { glint -> drawGlint(glint, arcY, tide.glintPhase(), colors.sceneryAccent) }
            }
        }
        if (scene == ShoreScene.BottleWaiting && moon) {
            MoonDisc(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = insets.calculateTopPadding() + ShoreDimens.moonInset,
                        end = insets.calculateEndPadding(layoutDirection) + ShoreDimens.moonInset,
                    ),
                haloAlpha = tide.haloAlpha,
            )
        }
    }
}

private val WaitingStars = listOf(
    SceneryStar(49f / BOARD_WIDTH, 139.dp, 0.45f),
    SceneryStar(113f / BOARD_WIDTH, 105.dp, 0.30f),
    SceneryStar(345f / BOARD_WIDTH, 221.dp, 0.35f),
    SceneryStar(31f / BOARD_WIDTH, 263.dp, 0.30f),
)

private val FoundStars = listOf(
    SceneryStar(49f / BOARD_WIDTH, 139.dp, 0.45f),
    SceneryStar(97f / BOARD_WIDTH, 101.dp, 0.30f),
    SceneryStar(319f / BOARD_WIDTH, 125.dp, 0.35f),
    SceneryStar(345f / BOARD_WIDTH, 221.dp, 0.35f),
    SceneryStar(31f / BOARD_WIDTH, 263.dp, 0.30f),
    SceneryStar(357f / BOARD_WIDTH, 331.dp, 0.25f),
)

private val Glints = listOf(
    SceneryGlint(182f / BOARD_WIDTH, 12.dp),
    SceneryGlint(60f / BOARD_WIDTH, 16.dp),
    SceneryGlint(300f / BOARD_WIDTH, 22.dp),
)


@NightDawnPreviews
@Composable
private fun ShoreSceneryPreview() = BottlingPreview {
    DomeScaffold(
        edge = DomeEdge.Full,
        contentPadding = PaddingValues(),
        scenery = { insets ->
            ShoreScenery(scene = ShoreScene.BottleWaiting, tide = BreathingTide.Still, insets = insets, largeText = false)
        },
    ) {}
}
