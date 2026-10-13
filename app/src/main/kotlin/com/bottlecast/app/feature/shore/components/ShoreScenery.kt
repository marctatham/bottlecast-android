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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.components.DomeEdge
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
 */
@Composable
internal fun ShoreScenery(
    scene: ShoreScene,
    tide: BreathingTide,
    insets: PaddingValues,
    largeText: Boolean,
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
            val stars = if (scene == ShoreScene.BottleWaiting) WaitingStars else FoundStars
            stars.forEach { star ->
                val alpha = if (colors.isNight) star.nightAlpha else star.nightAlpha * DAWN_STAR_DIMMING
                drawCircle(
                    color = colors.sceneryAccent.copy(alpha = alpha),
                    radius = ShoreDimens.star.toPx() / 2f,
                    center = Offset(startX(star.x * size.width), star.y.toPx()),
                )
            }
            if (scene == ShoreScene.BottleWaiting && !largeText) {
                val arcY = size.height * DomeEdge.Full.fraction
                Glints.forEach { glint -> drawGlint(glint, arcY, tide.glintPhase(), colors.sceneryAccent) }
            }
        }
        if (scene == ShoreScene.BottleWaiting) {
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

/** A glint drifts across the sea and round again, fading out at the screen's edges. */
private fun DrawScope.drawGlint(glint: Glint, arcY: Float, phase: Float, color: Color) {
    val width = ShoreDimens.glintWidth.toPx()
    val height = ShoreDimens.glintHeight.toPx()
    val travel = size.width
    val x = ((glint.x + phase) % 1f) * travel
    val edgeFade = (minOf(x, travel - width - x) / width).coerceIn(0f, 1f)
    drawRoundRect(
        color = color.copy(alpha = GLINT_ALPHA * edgeFade),
        topLeft = Offset(startX(x, width), arcY + glint.belowArc.toPx()),
        size = Size(width, height),
        cornerRadius = CornerRadius(height / 2f),
    )
}

/** Measures [x] from the start edge, so the scenery mirrors in right-to-left layouts. */
private fun DrawScope.startX(x: Float, width: Float = 0f): Float =
    if (layoutDirection == LayoutDirection.Ltr) x else size.width - x - width

/** A star: [x] as a share of the width, [y] from the top of the screen, as on the boards. */
private class Star(val x: Float, val y: Dp, val nightAlpha: Float)

/** A glint's resting place: [x] as a share of the width, [belowArc] under the dome's edge. */
private class Glint(val x: Float, val belowArc: Dp)

// the boards' 390dp frame, for turning their x positions into shares of the width
private const val BOARD_WIDTH = 390f

private val WaitingStars = listOf(
    Star(49f / BOARD_WIDTH, 139.dp, 0.45f),
    Star(113f / BOARD_WIDTH, 105.dp, 0.30f),
    Star(345f / BOARD_WIDTH, 221.dp, 0.35f),
    Star(31f / BOARD_WIDTH, 263.dp, 0.30f),
)

private val FoundStars = listOf(
    Star(49f / BOARD_WIDTH, 139.dp, 0.45f),
    Star(97f / BOARD_WIDTH, 101.dp, 0.30f),
    Star(319f / BOARD_WIDTH, 125.dp, 0.35f),
    Star(345f / BOARD_WIDTH, 221.dp, 0.35f),
    Star(31f / BOARD_WIDTH, 263.dp, 0.30f),
    Star(357f / BOARD_WIDTH, 331.dp, 0.25f),
)

private val Glints = listOf(
    Glint(182f / BOARD_WIDTH, 12.dp),
    Glint(60f / BOARD_WIDTH, 16.dp),
    Glint(300f / BOARD_WIDTH, 22.dp),
)

// Dawn's stars are glass, a little fainter than Night's moonlit ones (Shore-A-Dawn board)
private const val DAWN_STAR_DIMMING = 0.56f

// the glints are moon at 40% (spec §1.5)
private const val GLINT_ALPHA = 0.4f

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
