package com.bottlecast.app.common.components.illustrations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.components.MoonDisc
import com.bottlecast.app.common.components.bottle.BottleLook
import com.bottlecast.app.common.components.bottle.BottleRig
import com.bottlecast.app.common.components.bottle.Ripples
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

// The spot illustrations of the edge states (spec §1.6, §3.18), each 160dp square and built from the
// rig and the scenery. They're decorative: the title and body beside them say what happened. Their
// positions are in dp from the illustration's top-start corner, as on the boards, and parts may
// reach past its edges, so don't clip their parent.

/** The sea is empty: the moon over a calm sea, its reflection breaking into short bands (Edge-SeaCalm board). */
@Composable
fun SeaCalmIllustration(modifier: Modifier = Modifier) {
    val accent = BottlingTheme.colors.sceneryAccent
    Box(modifier = modifier.size(BottlingTheme.sizes.spotIllustration).clearAndSetSemantics {}) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // the horizon, fading out at both ends
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.5f to accent.copy(alpha = HORIZON_ALPHA),
                    1f to Color.Transparent,
                    startX = (-70).dp.toPx(),
                    endX = 230.dp.toPx(),
                ),
                topLeft = Offset((-70).dp.toPx(), 90.dp.toPx()),
                size = Size(300.dp.toPx(), 1.dp.toPx()),
            )
            Reflection.forEach { (y, width, alpha) -> drawBand(accent, centerX = 80f, y, width, alpha) }
            CalmGlints.forEach { (x, y, width, alpha) -> drawBand(accent, centerX = x + width / 2f, y, width, alpha) }
        }
        MoonDisc(modifier = Modifier.offset(60.dp, 8.dp))
    }
}

/** Offline: a bottle half hidden behind three soft bands of fog (Edge-Offline board). */
@Composable
fun FogIllustration(modifier: Modifier = Modifier) {
    val colors = BottlingTheme.colors
    val glass = colors.glass
    val fog = MaterialTheme.colorScheme.onSurfaceVariant
    Box(modifier = modifier.size(BottlingTheme.sizes.spotIllustration).clearAndSetSemantics {}) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            // a small dome, lit from the top end, with its moonpath along the bottom
            val glow = Offset(size.width * 0.68f, size.height * 0.3f)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to colors.domeGlow,
                    1f to colors.dome,
                    center = glow,
                    radius = (Offset(0f, size.height) - glow).getDistance() * MINI_DOME_GLOW_STOP,
                ),
                radius = radius,
            )
            drawArc(
                color = colors.moonpath,
                startAngle = 30f,
                sweepAngle = 120f,
                useCenter = false,
                style = Stroke(width = 1.dp.toPx()),
                topLeft = Offset(-0.5.dp.toPx(), -0.5.dp.toPx()),
                size = Size(size.width + 1.dp.toPx(), size.height + 1.dp.toPx()),
            )
            // the ripples under the bottle
            drawOval(
                color = glass.copy(alpha = 0.25f),
                topLeft = Offset(36.dp.toPx(), 134.dp.toPx()),
                size = Size(88.dp.toPx(), 12.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
            drawOval(
                color = glass.copy(alpha = 0.15f),
                topLeft = Offset(58.dp.toPx(), 137.dp.toPx()),
                size = Size(44.dp.toPx(), 6.dp.toPx()),
            )
        }
        BottleRig(
            look = BottleLook.Default,
            modifier = Modifier
                .offset(44.dp, 22.dp)
                .width(72.8.dp)
                .graphicsLayer {
                    rotationZ = FOG_BOTTLE_TILT
                    transformOrigin = TransformOrigin(0.495f, 0.594f)
                    alpha = FOG_BOTTLE_ALPHA
                },
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            FogBands.forEach { (x, y, width, height, alpha) ->
                drawRoundRect(
                    color = fog.copy(alpha = alpha),
                    topLeft = Offset(x.dp.toPx(), y.dp.toPx()),
                    size = Size(width.dp.toPx(), height.dp.toPx()),
                    cornerRadius = CornerRadius(height.dp.toPx() / 2f),
                )
            }
        }
    }
}

/** A find that failed: the bottle tipping back under the water, with ripples where it went in. No board draws it. */
@Composable
fun SlippedBackIllustration(modifier: Modifier = Modifier) {
    val accent = BottlingTheme.colors.sceneryAccent
    Box(modifier = modifier.size(BottlingTheme.sizes.spotIllustration).clearAndSetSemantics {}) {
        // the bottle, cut off at the waterline
        Box(modifier = Modifier.fillMaxWidth().height(WATERLINE).clipToBounds()) {
            BottleRig(
                look = BottleLook.Default,
                modifier = Modifier
                    .offset(52.dp, 40.dp)
                    .width(56.dp)
                    .graphicsLayer {
                        rotationZ = SLIPPED_TILT
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    },
            )
        }
        Ripples(modifier = Modifier.offset(0.dp, WATERLINE - 14.dp).width(160.dp))
        Canvas(modifier = Modifier.fillMaxSize()) {
            SlippedGlints.forEach { (x, y, width, alpha) -> drawBand(accent, centerX = x + width / 2f, y, width, alpha) }
        }
    }
}

// A 2dp band, rounded at its ends, centred on centerX: the moon's reflection and glints.
private fun DrawScope.drawBand(color: Color, centerX: Float, y: Float, width: Float, alpha: Float) {
    drawRoundRect(
        color = color.copy(alpha = alpha),
        topLeft = Offset((centerX - width / 2f).dp.toPx(), y.dp.toPx()),
        size = Size(width.dp.toPx(), 2.dp.toPx()),
        cornerRadius = CornerRadius(1.dp.toPx()),
    )
}

private const val HORIZON_ALPHA = 0.24f

// The moon's reflection: (y, width, alpha), centred under the moon
private val Reflection = listOf(
    Triple(100f, 44f, 0.55f),
    Triple(112f, 28f, 0.45f),
    Triple(124f, 52f, 0.36f),
    Triple(136f, 20f, 0.28f),
    Triple(148f, 36f, 0.20f),
)

private data class Band(val x: Float, val y: Float, val width: Float, val alpha: Float)

private val CalmGlints = listOf(
    Band(2f, 116f, 22f, 0.14f),
    Band(132f, 140f, 26f, 0.12f),
    Band(-38f, 132f, 18f, 0.10f),
    Band(172f, 108f, 20f, 0.10f),
)

private data class FogBand(val x: Float, val y: Float, val width: Float, val height: Float, val alpha: Float)

private val FogBands = listOf(
    FogBand(-26f, 40f, 150f, 22f, 0.10f),
    FogBand(26f, 80f, 160f, 26f, 0.14f),
    FogBand(-12f, 116f, 134f, 18f, 0.08f),
)

private const val MINI_DOME_GLOW_STOP = 0.58f
private const val FOG_BOTTLE_TILT = -10f
private const val FOG_BOTTLE_ALPHA = 0.8f

private val WATERLINE = 112.dp
private const val SLIPPED_TILT = -35f

private val SlippedGlints = listOf(
    Band(14f, 124f, 20f, 0.14f),
    Band(118f, 132f, 24f, 0.12f),
    Band(58f, 146f, 16f, 0.10f),
)

@NightDawnPreviews
@Composable
private fun SpotIllustrationsPreview() = BottlingPreview {
    Row(
        modifier = Modifier.padding(BottlingTheme.spacing.xxl),
        horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xxl),
    ) {
        SeaCalmIllustration()
        FogIllustration()
        SlippedBackIllustration()
    }
}
