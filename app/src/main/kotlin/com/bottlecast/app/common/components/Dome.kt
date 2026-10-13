package com.bottlecast.app.common.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingSizes
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import kotlin.math.max

/** Where the dome's edge sits, as a fraction of the screen's height (spec §1.4). */
@JvmInline
value class DomeEdge(val fraction: Float) {
    companion object {
        /** The Shore, onboarding, cast success and gift open. */
        val Full = DomeEdge(0.6f)

        /** Discover. */
        val Expanded = DomeEdge(0.7f)
    }
}

/**
 * The dome: the big arc behind the Shore and its sibling screens (spec §1.4), with radial
 * moonlight towards [glowCenter] and the 1dp moonpath tracing its edge. It draws behind
 * everything, so place it first in a full-screen Box; the circle reaches up under the status bar.
 *
 * The circle's radius is half the screen's height, as on the boards, or wider in landscape so the
 * arc still spans the screen.
 *
 * @param glowCenter where the moonlight is brightest, as fractions of the circle's bounding square
 *   (the boards' `radial-gradient(circle at x y …)`), measured from the start edge.
 * @param edgeOffset moves the edge from its resting place, read at draw time (the breathing tide).
 */
@Composable
fun Dome(
    edge: DomeEdge,
    modifier: Modifier = Modifier,
    glowCenter: Offset = DomeGlowTopEnd,
    edgeOffset: () -> Dp = { 0.dp },
) {
    val colors = BottlingTheme.colors
    Spacer(
        modifier = modifier
            .fillMaxSize()
            // the breathing tide moves the whole dome as a layer, so nothing is redrawn each frame
            .graphicsLayer { translationY = edgeOffset().toPx() }
            .drawWithCache {
                val radius = max(size.height * HEIGHT_TO_RADIUS, size.width * WIDTH_TO_RADIUS)
                val center = Offset(size.width / 2f, size.height * edge.fraction - radius)
                val square = Rect(center, radius)

                // CSS's radial-gradient runs to the farthest corner of the square, and the boards
                // reach plain dome 26% of the way along it
                val glowX = if (layoutDirection == LayoutDirection.Ltr) glowCenter.x else 1f - glowCenter.x
                val glow = Offset(square.left + square.width * glowX, square.top + square.height * glowCenter.y)
                val farthestCorner = maxOf(
                    (square.topLeft - glow).getDistance(),
                    (square.topRight - glow).getDistance(),
                    (square.bottomLeft - glow).getDistance(),
                    (square.bottomRight - glow).getDistance(),
                )
                val fill = Brush.radialGradient(
                    colors = listOf(colors.domeGlow, colors.dome),
                    center = glow,
                    radius = farthestCorner * GLOW_STOP,
                )

                // the moonpath sits just outside the edge and fades out at both ends (spec §1.4)
                val moonpath = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    MOONPATH_FADE to colors.moonpath,
                    1f - MOONPATH_FADE to colors.moonpath,
                    1f to Color.Transparent,
                    startX = 0f,
                    endX = size.width,
                )
                val moonpathRadius = radius + MOONPATH_GAP.toPx()
                val moonpathStroke = Stroke(width = BottlingSizes.hairline.toPx())

                onDrawBehind {
                    drawCircle(brush = fill, radius = radius, center = center)
                    drawCircle(brush = moonpath, radius = moonpathRadius, center = center, style = moonpathStroke)
                }
            }
    )
}

/** The dome's moonlight towards the top end, behind the moon (the Shore and Discover boards). */
val DomeGlowTopEnd = Offset(0.72f, 0.5f)

private const val HEIGHT_TO_RADIUS = 0.5f
private const val WIDTH_TO_RADIUS = 0.75f
private const val GLOW_STOP = 0.26f
private const val MOONPATH_FADE = 0.15f
private val MOONPATH_GAP = 1.5.dp

@NightDawnPreviews
@Composable
private fun DomePreview() = BottlingPreview {
    Box(modifier = Modifier.fillMaxSize()) {
        Dome(edge = DomeEdge.Full)
    }
}
