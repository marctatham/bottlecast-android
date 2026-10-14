package com.bottlecast.app.common.components.bottle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.bottlecast.app.common.ui.BottleRigColors
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

/**
 * Where the rig's moving parts are, each read at draw time so an animation redraws without
 * recomposing. Screens hoist these, and screenshot tests pass fixed values, so no capture ever
 * catches a running animation.
 */
@Stable
class BottleRigPose(
    /** The cork's flight off the neck, from 0 (in) to 1 (gone), up and away to the start. */
    val corkFlight: () -> Float = { 0f },
    /** The scroll, from 0 (in the bottle) through 0.5 (half out of the neck) to 1 (clear of it). */
    val scrollOut: () -> Float = { 0f },
    /** A sheen sweeping across the scroll while its message is still on the way, or null. */
    val shimmer: () -> Float? = { null },
) {
    companion object {
        val Still = BottleRigPose()
    }
}

/**
 * The bottle (spec §1.6): soft glass under moonlight, with a cream scroll inside and a tan cork,
 * drawn from TEMPLATE.md's rig. It's decorative; whatever holds it says what it is.
 *
 * The scroll and the cork can travel outside the bottle's bounds, so don't clip its parent.
 * Callers tilt, bob and wobble it with a graphics layer.
 *
 * @param corked false once the cork has gone.
 * @param showScroll false for an empty bottle.
 */
@Composable
fun BottleRig(
    look: BottleLook,
    modifier: Modifier = Modifier,
    pose: BottleRigPose = BottleRigPose.Still,
    corked: Boolean = true,
    showScroll: Boolean = true,
) {
    val colors = BottlingTheme.colors
    val rig = colors.rig
    val hairline = BottlingTheme.sizes.hairline
    val glassFill = remember(look, colors) { look.tint.fill(colors).rotateHue(look.hueJitter).copy(alpha = GLASS_ALPHA) }
    val glassStroke = rig.outline ?: look.tint.stroke(rig)
    Spacer(
        modifier = modifier
            .aspectRatio(BottleRigPaths.WIDTH / BottleRigPaths.HEIGHT)
            .clearAndSetSemantics {}
            .drawWithCache {
                val scale = size.width / BottleRigPaths.WIDTH
                val body = BottleRigPaths.body(look.silhouette)
                val highlight = BottleRigPaths.highlight(look.silhouette)
                // Dawn's outline is 1dp at any size; the tint's own edge scales with the bottle
                val glassStrokeStyle = Stroke(width = if (rig.outline != null) hairline.toPx() / scale else GLASS_STROKE)
                val bodyHighlightStyle = Stroke(width = BODY_HIGHLIGHT_WIDTH, cap = StrokeCap.Round)
                val neckHighlightStyle = Stroke(width = NECK_HIGHLIGHT_WIDTH, cap = StrokeCap.Round)
                val corkLineStyle = Stroke(width = CORK_LINE_WIDTH, cap = StrokeCap.Round)

                onDrawBehind {
                    val scrollOut = if (showScroll) pose.scrollOut() else 0f
                    val shimmer = pose.shimmer()
                    scale(scale, scale, pivot = Offset.Zero) {
                        // the scroll inside the glass shows through it
                        if (showScroll) clipPath(body) { drawScroll(rig, scrollOut, shimmer) }
                        drawPath(body, glassFill)
                        drawPath(body, glassStroke, style = glassStrokeStyle)
                        drawPath(highlight, rig.highlight.copy(alpha = BODY_HIGHLIGHT_ALPHA), style = bodyHighlightStyle)
                        drawPath(BottleRigPaths.neckHighlight, rig.highlight.copy(alpha = NECK_HIGHLIGHT_ALPHA), style = neckHighlightStyle)
                        drawCircle(rig.highlight.copy(alpha = GLINT_ALPHA), radius = GLINT_RADIUS, center = GLINT_CENTER)
                        drawRoundRect(
                            color = rig.neckLip.copy(alpha = NECK_LIP_ALPHA),
                            topLeft = Offset(56f, 30f),
                            size = Size(28f, 8f),
                            cornerRadius = CornerRadius(4f),
                        )
                        // the part of the scroll that has left the neck sits in front of the glass
                        if (scrollOut > 0f) {
                            clipRect(
                                left = -BottleRigPaths.WIDTH,
                                top = -BottleRigPaths.HEIGHT * 2,
                                right = BottleRigPaths.WIDTH * 2,
                                bottom = BottleRigPaths.NECK_MOUTH_Y,
                            ) { drawScroll(rig, scrollOut, shimmer) }
                        }
                        if (corked) drawCork(rig, pose.corkFlight(), corkLineStyle)
                    }
                }
            }
    )
}

// The scroll rolls narrow to fit the neck, rises out of it, then opens back up once it's clear.
private fun DrawScope.drawScroll(rig: BottleRigColors, out: Float, shimmer: Float?) {
    val width = when {
        out <= 0f -> 1f
        out < SCROLL_NARROW_UNTIL -> SCROLL_NARROW
        else -> lerp(SCROLL_NARROW, 1f, (out - SCROLL_NARROW_UNTIL) / (1f - SCROLL_NARROW_UNTIL))
    }
    withTransform({
        translate(top = -out * SCROLL_TRAVEL)
        scale(scaleX = width, scaleY = 1f, pivot = Offset(SCROLL_CENTER_X, 0f))
    }) {
        drawRoundRect(rig.scroll, topLeft = Offset(52f, 104f), size = Size(36f, 84f), cornerRadius = CornerRadius(9f))
        drawRoundRect(rig.scrollEnds, topLeft = Offset(52f, 104f), size = Size(36f, 12f), cornerRadius = CornerRadius(6f))
        drawRoundRect(rig.scrollEnds, topLeft = Offset(52f, 176f), size = Size(36f, 12f), cornerRadius = CornerRadius(6f))
        drawRect(rig.ribbon, topLeft = Offset(52f, 140f), size = Size(36f, 6f))
        if (shimmer != null) {
            clipRect(left = 52f, top = 104f, right = 88f, bottom = 188f) {
                drawRect(
                    color = rig.highlight.copy(alpha = SHIMMER_ALPHA),
                    topLeft = Offset(52f - SHIMMER_WIDTH + shimmer * (36f + SHIMMER_WIDTH), 104f),
                    size = Size(SHIMMER_WIDTH, 84f),
                )
            }
        }
    }
}

private fun DrawScope.drawCork(rig: BottleRigColors, flight: Float, lineStyle: Stroke) {
    val alpha = 1f - ((flight - CORK_FADE_FROM) / (1f - CORK_FADE_FROM)).coerceIn(0f, 1f)
    if (alpha <= 0f) return
    withTransform({
        translate(left = CORK_FLIGHT.x * flight, top = CORK_FLIGHT.y * flight)
        rotate(degrees = CORK_SPIN * flight, pivot = CORK_CENTER)
    }) {
        drawPath(BottleRigPaths.cork, rig.cork, alpha = alpha)
        drawPath(BottleRigPaths.corkLines, rig.corkLines, alpha = alpha, style = lineStyle)
    }
}

// The rig's values, in its viewBox units (TEMPLATE.md)
private const val GLASS_ALPHA = 0.42f
private const val GLASS_STROKE = 2f
private const val BODY_HIGHLIGHT_WIDTH = 5f
private const val BODY_HIGHLIGHT_ALPHA = 0.75f
private const val NECK_HIGHLIGHT_WIDTH = 3f
private const val NECK_HIGHLIGHT_ALPHA = 0.6f
private const val GLINT_ALPHA = 0.7f
private const val GLINT_RADIUS = 3f
private val GLINT_CENTER = Offset(96f, 120f)
private const val NECK_LIP_ALPHA = 0.85f
private const val CORK_LINE_WIDTH = 2f
private val CORK_CENTER = Offset(70f, 20f)

// The pop (spec §1.5): the cork flies up and away, turning 220°, and fades as it goes.
private val CORK_FLIGHT = Offset(-48f, -110f)
private const val CORK_SPIN = -220f
private const val CORK_FADE_FROM = 0.7f

// How far the scroll rises to clear the neck, and how narrow it rolls to fit through it.
private const val SCROLL_TRAVEL = 200f
private const val SCROLL_NARROW = 0.55f
private const val SCROLL_NARROW_UNTIL = 0.8f
private const val SCROLL_CENTER_X = 70f
private const val SHIMMER_ALPHA = 0.35f
private const val SHIMMER_WIDTH = 14f

@NightDawnPreviews
@Composable
private fun BottleRigPreview() = BottlingPreview {
    Row(
        modifier = Modifier.padding(BottlingTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.m),
    ) {
        BottleSilhouette.entries.forEach { silhouette ->
            BottleRig(look = BottleLook(silhouette), modifier = Modifier.height(120.dp))
        }
        BottleRig(
            look = BottleLook.Default,
            modifier = Modifier.height(120.dp),
            pose = BottleRigPose(scrollOut = { 0.5f }),
            corked = false,
        )
    }
}
