package com.bottlecast.app.feature.castsuccess.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.lerp
import com.bottlecast.app.common.components.bottle.BottleLook
import com.bottlecast.app.common.components.bottle.BottleRig
import com.bottlecast.app.common.components.bottle.BottleRigPose
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.feature.castsuccess.CastSuccessDimens

/**
 * The cast itself (spec §1.5): the message rolls up and slips into the bottle, the cork goes on, and
 * the bottle is tossed in an arc out to the horizon, where three ripples spread and stay. Once it's
 * landed, it bobs there. Everything is read from [motion] at draw time.
 *
 * @param arcY where the settled dome's edge is, from the top of the screen.
 */
@Composable
internal fun CastStage(
    look: BottleLook,
    motion: CastMotion,
    arcY: Dp,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = BottlingTheme.colors
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val scrollShown by remember(motion) { derivedStateOf { castPose(motion.timeline()).rolled >= 1f } }
    // positions are worked out from the left, mirrored by hand in right-to-left layouts
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val bottleHeight = CastSuccessDimens.bottle
        val bottleWidth = bottleHeight * RIG_ASPECT
        val width = with(density) { maxWidth.toPx() }
        val height = with(density) { maxHeight.toPx() }
        val start = Offset(width / 2f, height * CastSuccessDimens.START_Y)
        val end = Offset(width / 2f, with(density) { (arcY - CastSuccessDimens.horizonAboveArc).toPx() })
        // the toss rises out towards the end edge, then drops back to the middle of the horizon
        val control = Offset(
            if (rtl) width * (1f - TOSS_REACH) else width * TOSS_REACH,
            minOf(start.y, end.y) - height * CastSuccessDimens.TOSS_ARC,
        )
        val bobHeight = with(density) { BOB_HEIGHT.toPx() }

        // the ripples where it lands: each spreads to its resting width and stays
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pose = castPose(motion.timeline())
            val base = CastSuccessDimens.ripplesWidth.toPx()
            val stroke = Stroke(width = RING_STROKE.toPx())
            pose.rings.forEachIndexed { ring, spread ->
                if (spread <= 0f) return@forEachIndexed
                val ringWidth = base * RING_SIZES[ring] * spread
                val ringHeight = ringWidth * RING_FLATNESS
                drawOval(
                    color = colors.glass.copy(alpha = RING_ALPHAS[ring] * spread),
                    topLeft = Offset(end.x - ringWidth / 2f, end.y + bottleFoot(bottleHeight.toPx()) - ringHeight / 2f),
                    size = Size(ringWidth, ringHeight),
                    style = stroke,
                )
            }
        }

        // the message, rolling up into a scroll just above the neck
        Canvas(
            modifier = Modifier
                .align(AbsoluteAlignment.TopLeft)
                .requiredSize(CastSuccessDimens.parchmentWidth, CastSuccessDimens.parchmentHeight)
                .graphicsLayer {
                    val pose = castPose(motion.timeline())
                    translationX = start.x - size.width / 2f
                    translationY = start.y - bottleHeight.toPx() / 2f - CastSuccessDimens.parchmentAboveBottle.toPx() - size.height
                    scaleY = lerp(1f, ROLLED_HEIGHT, pose.rolled)
                    alpha = if (pose.rolled < 1f) 1f else 0f
                    transformOrigin = TransformOrigin(0.5f, 1f)
                },
        ) {
            drawRoundRect(color = colors.parchment, cornerRadius = CornerRadius(PARCHMENT_CORNER.toPx()))
            // three lines of ink, the message seen from afar
            val inset = size.width * LINE_INSET
            repeat(LINES) { line ->
                val y = size.height * (0.3f + line * 0.2f)
                val lineEnd = if (line == LINES - 1) size.width * 0.6f else size.width - inset
                drawLine(colors.inkMuted, Offset(inset, y), Offset(lineEnd, y), strokeWidth = LINE_STROKE.toPx())
            }
        }

        Box(
            modifier = Modifier
                .align(AbsoluteAlignment.TopLeft)
                .size(bottleWidth, bottleHeight)
                .semantics { contentDescription = description }
                .graphicsLayer {
                    val millis = motion.timeline()
                    val pose = castPose(millis)
                    val t = pose.tossed
                    // a quadratic Bézier from the start, through the arc, to the horizon
                    val x = (1 - t) * (1 - t) * start.x + 2 * (1 - t) * t * control.x + t * t * end.x
                    val landed = millis >= CastTimeline.END
                    val bob = if (landed) motion.bob() else 0f
                    val y = (1 - t) * (1 - t) * start.y + 2 * (1 - t) * t * control.y + t * t * end.y - bobHeight * bob * CastSuccessDimens.FAR_SCALE * BOB_GAIN
                    translationX = x - size.width / 2f
                    translationY = y - size.height / 2f
                    val scale = lerp(1f, CastSuccessDimens.FAR_SCALE, t)
                    scaleX = if (rtl) -scale else scale
                    scaleY = scale
                    val tilt = lerp(0f, CastSuccessDimens.TOSS_TILT, t) + BOB_TILT * bob
                    rotationZ = if (rtl) -tilt else tilt
                },
        ) {
            BottleRig(
                look = look,
                modifier = Modifier.fillMaxSize(),
                pose = BottleRigPose(
                    // off until the scroll is in, then it springs back on
                    corkFlight = { 1f - castPose(motion.timeline()).corked },
                    scrollOut = { 1f - castPose(motion.timeline()).slipped },
                ),
                // the rolled parchment becomes the rig's scroll, so it shows once rolling is done
                showScroll = scrollShown,
            )
        }
    }
}

// where the bottle meets the water, from its middle, once it's far out
private fun bottleFoot(bottleHeight: Float): Float = bottleHeight * CastSuccessDimens.FAR_SCALE * 0.4f

private const val RIG_ASPECT = 140f / 220f
private const val TOSS_REACH = 0.78f
private const val ROLLED_HEIGHT = 0.08f
private const val LINES = 3
private const val LINE_INSET = 0.14f
private val PARCHMENT_CORNER = CastSuccessDimens.parchmentCorner
private val LINE_STROKE = CastSuccessDimens.inkLine
private val RING_STROKE = CastSuccessDimens.ringStroke
private val BOB_HEIGHT = CastSuccessDimens.bobHeight

// far out, its bob shows as a few pixels; this keeps it visible at 12%
private const val BOB_GAIN = 4f
private const val BOB_TILT = 5f
private const val RING_FLATNESS = 0.14f
private val RING_SIZES = listOf(1f, 0.66f, 0.33f)
private val RING_ALPHAS = listOf(0.2f, 0.35f, 0.55f)
