package com.bottlecast.app.feature.keepsake.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.common.ui.shortDate
import com.bottlecast.app.feature.keepsake.JourneyStep
import com.bottlecast.app.feature.keepsake.JourneyStop
import com.bottlecast.app.feature.keepsake.KeepsakeDimens
import java.time.LocalDate

/**
 * A bottle's journey (spec §3.11): a dot for each stop, joined by a line. A stop already reached has a
 * primary dot and its date; one still to come has an outlineVariant dot and "Not yet".
 */
@Composable
internal fun JourneyTimeline(stops: List<JourneyStop>, today: LocalDate, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        stops.forEachIndexed { index, stop ->
            JourneyRow(stop = stop, today = today, first = index == 0, last = index == stops.lastIndex)
        }
    }
}

@Composable
private fun JourneyRow(stop: JourneyStop, today: LocalDate, first: Boolean, last: Boolean) {
    val colorScheme = MaterialTheme.colorScheme
    val reached = stop.date != null
    val dotColor = if (reached) colorScheme.primary else colorScheme.outlineVariant
    val lineColor = colorScheme.outlineVariant
    val hairline = BottlingTheme.sizes.hairline
    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.s),
    ) {
        // the dot sits level with the label, and the line runs on to the next stop's dot
        Box(modifier = Modifier.width(KeepsakeDimens.journeyDot).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            Canvas(modifier = Modifier.width(KeepsakeDimens.journeyDot).fillMaxHeight()) {
                val dotCenterY = KeepsakeDimens.journeyDotTop.toPx() + size.width / 2
                if (!first) drawLine(lineColor, Offset(center.x, 0f), Offset(center.x, dotCenterY), hairline.toPx())
                if (!last) drawLine(lineColor, Offset(center.x, dotCenterY), Offset(center.x, size.height), hairline.toPx())
                drawCircle(dotColor, radius = size.width / 2, center = Offset(center.x, dotCenterY))
            }
        }
        Column(modifier = Modifier.padding(bottom = if (last) 0.dp else BottlingTheme.spacing.s)) {
            Text(
                text = stringResource(stop.step.label()),
                style = MaterialTheme.typography.labelLarge,
                color = if (reached) colorScheme.onSurface else colorScheme.onSurfaceVariant,
            )
            Text(
                text = stop.date?.let { shortDate(it, today) } ?: stringResource(R.string.keepsake_journey_not_yet),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun JourneyStep.label(): Int = when (this) {
    JourneyStep.Cast -> R.string.keepsake_journey_cast
    JourneyStep.Found -> R.string.keepsake_journey_found
    JourneyStep.FoundBySomeone -> R.string.keepsake_journey_found_by_someone
    JourneyStep.Kept -> R.string.keepsake_journey_kept
}

@NightDawnPreviews
@Composable
private fun JourneyTimelinePreview() = BottlingPreview {
    val today = LocalDate.of(2026, 10, 12)
    JourneyTimeline(
        stops = listOf(
            JourneyStop(JourneyStep.Cast, today.minusDays(9)),
            JourneyStop(JourneyStep.Found, today.minusDays(7)),
            JourneyStop(JourneyStep.Kept, null),
        ),
        today = today,
        modifier = Modifier.padding(BottlingTheme.spacing.m),
    )
}
