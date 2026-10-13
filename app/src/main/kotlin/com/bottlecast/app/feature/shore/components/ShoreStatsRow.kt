package com.bottlecast.app.feature.shore.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import com.bottlecast.app.R
import com.bottlecast.app.common.components.OverlineText
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.feature.shore.ShoreDimens
import com.bottlecast.app.feature.shore.ShoreStats

/**
 * The Shore's stats (spec §3.3): each a numeral over a caption, 24dp apart, the whole row one
 * 48dp target that opens Me. Guests have no casts, so they see Found and Kept only. The items wrap
 * at large text sizes.
 */
@Composable
internal fun ShoreStatsRow(
    stats: ShoreStats,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOfNotNull(
        stats.bottled?.let { Triple(it, R.string.shore_stat_bottled, R.plurals.shore_stat_bottled_description) },
        Triple(stats.found, R.string.shore_stat_found, R.plurals.shore_stat_found_description),
        Triple(stats.kept, R.string.shore_stat_kept, R.plurals.shore_stat_kept_description),
    )
    val description = stringResource(
        R.string.shore_stats_description,
        items.map { (count, _, descriptionRes) -> pluralStringResource(descriptionRes, count, count) }
            .joinToString(separator = ", "),
    )

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BottlingTheme.sizes.touchTarget)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick { onClick(); true }
            },
        horizontalArrangement = Arrangement.spacedBy(ShoreDimens.statsGap, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { (count, captionRes, _) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = count.toString(),
                    style = BottlingTheme.typography.shoreStatNumeral,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                OverlineText(text = stringResource(captionRes), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@NightDawnPreviews
@Composable
private fun ShoreStatsRowPreview() = BottlingPreview {
    ShoreStatsRow(stats = ShoreStats(bottled = 12, found = 30, kept = 9), onClick = {})
}
