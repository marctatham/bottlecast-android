package com.bottlecast.app.feature.castsuccess.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.feature.castsuccess.CastSuccessDimens

/** A step on the bottle's journey (spec §3.5). */
internal enum class JourneyStep(@param:StringRes val label: Int) {
    Corked(R.string.journey_corked),
    Drifting(R.string.journey_drifting),
    Found(R.string.journey_found),
    Kept(R.string.journey_kept),
    Sealed(R.string.journey_sealed),
}

/**
 * The bottle's journey (spec §3.5): Corked, done; Drifting, now, with a pulsing dot ("Waiting for
 * connection" while it's queued); then Found, Kept and Sealed to come. Sealed is left out when the
 * writer switched sealing off. Five chips don't fit one line, so they wrap, centred, joined by 1dp
 * lines only between chips on the same line.
 */
@Composable
internal fun JourneyRow(
    queued: Boolean,
    sealable: Boolean,
    pulse: () -> Float,
    modifier: Modifier = Modifier,
) {
    val steps = JourneyStep.entries.filter { it != JourneyStep.Sealed || sealable }
    val description = stringResource(R.string.journey_description)
    val gap = CastSuccessDimens.connectorGap
    val connector = CastSuccessDimens.connector
    val rowGap = CastSuccessDimens.journeyRowGap
    Layout(
        modifier = modifier.semantics { contentDescription = description },
        content = {
            steps.forEachIndexed { index, step ->
                if (index > 0) Connector()
                JourneyChip(step = step, queued = queued, pulse = pulse)
            }
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(loose) }
        // children alternate: chip, connector, chip, connector, …
        val chips = placeables.filterIndexed { index, _ -> index % 2 == 0 }
        val connectors = placeables.filterIndexed { index, _ -> index % 2 == 1 }
        val joinWidth = (connector + gap * 2).roundToPx()
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Constraints.Infinity

        // lines of chip indices, each joined chip taking its connector's room
        val lines = mutableListOf(mutableListOf(0))
        var lineWidth = chips[0].width
        for (index in 1 until chips.size) {
            val needed = joinWidth + chips[index].width
            if (lineWidth + needed <= maxWidth) {
                lines.last() += index
                lineWidth += needed
            } else {
                lines += mutableListOf(index)
                lineWidth = chips[index].width
            }
        }
        val lineWidths = lines.map { line -> line.sumOf { chips[it].width } + (line.size - 1) * joinWidth }
        val lineHeights = lines.map { line -> line.maxOf { chips[it].height } }
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else lineWidths.max()
        val height = lineHeights.sum() + (lines.size - 1) * rowGap.roundToPx()

        layout(width, height) {
            var y = 0
            lines.forEachIndexed { lineIndex, line ->
                var x = (width - lineWidths[lineIndex]) / 2
                val lineHeight = lineHeights[lineIndex]
                line.forEachIndexed { position, chipIndex ->
                    if (position > 0) {
                        val join = connectors[chipIndex - 1]
                        join.placeRelative(x + gap.roundToPx(), y + (lineHeight - join.height) / 2)
                        x += joinWidth
                    }
                    val chip = chips[chipIndex]
                    chip.placeRelative(x, y + (lineHeight - chip.height) / 2)
                    x += chip.width
                }
                y += lineHeight + rowGap.roundToPx()
            }
            // a connector whose chip starts a new line is never placed, so it isn't drawn
        }
    }
}

@Composable
private fun Connector() {
    Box(
        modifier = Modifier
            .size(width = CastSuccessDimens.connector, height = BottlingTheme.sizes.hairline)
            .background(MaterialTheme.colorScheme.outline)
            .clearAndSetSemantics {},
    )
}

@Composable
private fun JourneyChip(step: JourneyStep, queued: Boolean, pulse: () -> Float) {
    val colors = BottlingTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val sizes = BottlingTheme.sizes
    val label = stringResource(if (step == JourneyStep.Drifting && queued) R.string.journey_waiting else step.label)
    val state = when (step) {
        JourneyStep.Corked -> stringResource(R.string.journey_done, label)
        JourneyStep.Drifting -> stringResource(R.string.journey_now, label)
        else -> stringResource(R.string.journey_to_come, label)
    }
    val (container, content) = when (step) {
        JourneyStep.Corked -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
        JourneyStep.Drifting -> colors.glassContainer to colors.onGlassContainer
        else -> null to colorScheme.onSurfaceVariant
    }
    val shape = BottlingTheme.shapes.pill
    Row(
        modifier = Modifier
            .heightIn(min = sizes.chipMinHeight)
            .then(if (container != null) Modifier.background(container, shape) else Modifier.border(sizes.hairline, colorScheme.outline, shape))
            .padding(start = if (container != null) sizes.chipPaddingStart else sizes.chipPaddingEnd, end = sizes.chipPaddingEnd)
            .clearAndSetSemantics { contentDescription = state },
        horizontalArrangement = Arrangement.spacedBy(sizes.chipIconGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (step) {
            JourneyStep.Corked -> Icon(
                painter = painterResource(R.drawable.ic_symbol_check),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(sizes.iconSmall),
            )
            JourneyStep.Drifting -> Box(
                modifier = Modifier
                    .padding(horizontal = (sizes.iconSmall - CastSuccessDimens.pulseDot) / 2)
                    .size(CastSuccessDimens.pulseDot)
                    .graphicsLayer { alpha = PULSE_LOW + (1f - PULSE_LOW) * pulse() }
                    .background(colors.glass, CircleShape),
            )
            else -> Unit
        }
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

private const val PULSE_LOW = 0.35f
