package com.bottlecast.app.feature.shore.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.bottlecast.app.common.components.MoonDisc
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.feature.shore.ShoreDimens
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shore B's moon, wrapped by the tide ring (spec §3.3): a 4dp glass arc on an outlineVariant
 * track, filled by the share of the local day that has passed. It starts at 12 o'clock and runs
 * clockwise in either layout direction, as time does, and ends in a small glass marker. It's
 * decorative: the countdown beside it says the same in words.
 *
 * @param dayElapsed from 0 at midnight to 1 at the next.
 * @param haloAlpha the moon's halo opacity, read at draw time (the breathing tide).
 */
@Composable
internal fun TideRing(
    dayElapsed: Float,
    modifier: Modifier = Modifier,
    haloAlpha: () -> Float = { 1f },
) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val glass = BottlingTheme.colors.glass
    Box(
        modifier = modifier
            .size(ShoreDimens.tideRingBox)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = ShoreDimens.tideRingStroke.toPx()
            // the ring is 128dp to the stroke's outer edge, so its centreline is a stroke-width in
            val diameter = ShoreDimens.tideRing.toPx() - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = FULL_CIRCLE,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            val fraction = dayElapsed.coerceIn(0f, 1f)
            drawArc(
                color = glass,
                startAngle = TWELVE_O_CLOCK,
                sweepAngle = FULL_CIRCLE * fraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            // the marker where the tide has reached
            val endAngle = Math.toRadians((TWELVE_O_CLOCK + FULL_CIRCLE * fraction).toDouble())
            val radius = diameter / 2f
            val marker = Offset(
                x = center.x + radius * cos(endAngle).toFloat(),
                y = center.y + radius * sin(endAngle).toFloat(),
            )
            drawCircle(color = glass.copy(alpha = MARKER_GLOW_ALPHA), radius = ShoreDimens.tideMarkerGlow.toPx() / 2f, center = marker)
            drawCircle(color = glass, radius = ShoreDimens.tideMarker.toPx() / 2f, center = marker)
        }
        MoonDisc(diameter = BottlingTheme.sizes.moonDiscLarge, haloAlpha = haloAlpha)
    }
}

private const val FULL_CIRCLE = 360f
private const val TWELVE_O_CLOCK = -90f
private const val MARKER_GLOW_ALPHA = 0.2f


@NightDawnPreviews
@Composable
private fun TideRingPreview() = BottlingPreview {
    TideRing(dayElapsed = 0.7f)
}
