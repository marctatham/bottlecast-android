package com.bottlecast.app.feature.me.components

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import com.bottlecast.app.R
import com.bottlecast.app.common.components.OverlineText
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.common.ui.isLargeText
import com.bottlecast.app.feature.me.MeDimens
import com.bottlecast.app.feature.me.MeStats

/**
 * The Me tab's numbers (spec §3.12): a surfaceHigh card of four columns, each a statNumeral over its
 * caption, the streak's with its best and its shields beneath. A guest has only Found and Streak. A
 * number not known yet, such as offline, shows a dash. At large text the columns go two to a row.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MeStatsCard(stats: MeStats, modifier: Modifier = Modifier) {
    val perRow = if (isLargeText() || stats.guest) 2 else 4
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .clip(BottlingTheme.shapes.card)
            .background(BottlingTheme.colors.surfaceHigh)
            .padding(
                start = MeDimens.cardPaddingHorizontal,
                end = MeDimens.cardPaddingHorizontal,
                top = MeDimens.cardPaddingTop,
                bottom = MeDimens.cardPaddingBottom,
            ),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.m),
        maxItemsInEachRow = perRow,
    ) {
        val column = Modifier.weight(1f)
        if (!stats.guest) Stat(stats.bottled, R.string.shore_stat_bottled, R.plurals.shore_stat_bottled_description, column)
        Stat(stats.found, R.string.shore_stat_found, R.plurals.shore_stat_found_description, column)
        if (!stats.guest) Stat(stats.kept, R.string.shore_stat_kept, R.plurals.shore_stat_kept_description, column)
        StreakStat(stats, column)
    }
}

@Composable
private fun Stat(count: Int?, @StringRes caption: Int, @PluralsRes description: Int, modifier: Modifier) {
    val label = stringResource(caption)
    val spoken = count?.let { pluralStringResource(description, it, it) } ?: stringResource(R.string.me_stat_unknown_description, label)
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = spoken },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Numeral(count)
        OverlineText(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StreakStat(stats: MeStats, modifier: Modifier) {
    val label = stringResource(R.string.me_stat_streak)
    val days = stats.streak
    val best = stats.best
    val shields = stats.shields
    val spoken = if (days != null && best != null && shields != null) {
        stringResource(
            R.string.me_streak_description,
            pluralStringResource(R.plurals.me_streak_days, days, days),
            best,
            pluralStringResource(R.plurals.me_stat_shields, shields, shields),
        )
    } else {
        stringResource(R.string.me_stat_unknown_description, label)
    }
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = spoken },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Numeral(days)
        OverlineText(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (best != null && shields != null) {
            Text(
                text = stringResource(R.string.me_stat_best, best),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = pluralStringResource(R.plurals.me_stat_shields, shields, shields),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Numeral(count: Int?) {
    Text(
        text = count?.toString() ?: stringResource(R.string.me_stat_unknown),
        style = BottlingTheme.typography.statNumeral,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
    )
}

@NightDawnPreviews
@Composable
private fun MeStatsCardPreview() = BottlingPreview {
    MeStatsCard(stats = MeStats(guest = false, bottled = 12, found = 30, kept = 9, streak = 6, best = 14, shields = 1))
}
