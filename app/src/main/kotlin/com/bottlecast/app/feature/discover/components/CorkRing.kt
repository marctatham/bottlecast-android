package com.bottlecast.app.feature.discover.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.feature.discover.DiscoverDimens
import com.bottlecast.app.feature.discover.domain.UncorkMachine
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The cork ring around the bottle's neck (spec §3.6): a wavy ring in glass on an outlineVariant
 * track, filling a third with each shake or tap, with ticks at the thirds. The bottle says how loose
 * the cork is, so the ring itself is hidden from TalkBack.
 *
 * @param waveSpeed how fast the wave runs round; 0 holds it still (reduced motion and screenshots).
 */
@Composable
internal fun CorkRing(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    waveSpeed: Dp = WavyProgressIndicatorDefaults.CircularWavelength,
) {
    val tickColor = MaterialTheme.colorScheme.outline
    val stroke = with(LocalDensity.current) {
        Stroke(width = DiscoverDimens.corkRingStroke.toPx(), cap = StrokeCap.Round)
    }
    CircularWavyProgressIndicator(
        progress = progress,
        modifier = modifier
            .size(DiscoverDimens.corkRing)
            .clearAndSetSemantics {}
            .drawWithContent {
                drawContent()
                // a short mark across the track at each third, from the top, clockwise
                val radius = size.minDimension / 2f - stroke.width / 2f
                val half = DiscoverDimens.corkRingTick.toPx() / 2f
                for (third in 1 until UncorkMachine.STEPS) {
                    val angle = (third.toFloat() / UncorkMachine.STEPS * FULL_TURN - QUARTER_TURN).toDouble()
                    val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                    drawLine(
                        color = tickColor,
                        start = center + direction * (radius - half),
                        end = center + direction * (radius + half),
                        strokeWidth = DiscoverDimens.corkRingTickWidth.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            },
        color = BottlingTheme.colors.glass,
        trackColor = MaterialTheme.colorScheme.outlineVariant,
        stroke = stroke,
        trackStroke = stroke,
        waveSpeed = waveSpeed,
    )
}

private const val FULL_TURN = (2 * PI).toFloat()
private const val QUARTER_TURN = (PI / 2).toFloat()
