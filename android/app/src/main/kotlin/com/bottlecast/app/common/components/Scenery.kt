package com.bottlecast.app.common.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.ui.BottlingColors

/** The boards' frame width, for turning their x positions into shares of the width. */
const val BOARD_WIDTH = 390f

/** A star: [x] as a share of the width, [y] from the top of the screen, as on the boards. */
class SceneryStar(val x: Float, val y: Dp, val nightAlpha: Float)

/** A glint's resting place on the sea: [x] as a share of the width, [belowArc] under the dome's edge. */
class SceneryGlint(val x: Float, val belowArc: Dp)

/** Draws the dome's stars (spec §1.6), mirrored in right-to-left layouts. */
fun DrawScope.drawStars(stars: List<SceneryStar>, colors: BottlingColors) {
    stars.forEach { star ->
        val alpha = if (colors.isNight) star.nightAlpha else star.nightAlpha * DAWN_STAR_DIMMING
        drawCircle(
            color = colors.sceneryAccent.copy(alpha = alpha),
            radius = STAR_SIZE.toPx() / 2f,
            center = Offset(startX(star.x * size.width), star.y.toPx()),
        )
    }
}

/**
 * Draws a moon glint on the sea, [phase] of the way round its drift (0 for a still glint), fading
 * out at the screen's edges (spec §1.5).
 */
fun DrawScope.drawGlint(glint: SceneryGlint, arcY: Float, phase: Float, color: Color) {
    val width = GLINT_WIDTH.toPx()
    val height = GLINT_HEIGHT.toPx()
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

/** Measures [x] from the start edge, so scenery mirrors in right-to-left layouts. */
fun DrawScope.startX(x: Float, width: Float = 0f): Float =
    if (layoutDirection == LayoutDirection.Ltr) x else size.width - x - width

private val STAR_SIZE = 2.dp
private val GLINT_WIDTH = 24.dp
private val GLINT_HEIGHT = 2.dp

// Dawn's stars are glass, a little fainter than Night's moonlit ones (Shore-A-Dawn board)
private const val DAWN_STAR_DIMMING = 0.56f

// the glints are moon at 40% (spec §1.5)
private const val GLINT_ALPHA = 0.4f
