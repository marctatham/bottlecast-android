package com.bottlecast.app.feature.chest.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.bottlecast.app.R
import com.bottlecast.app.common.components.StatusChip
import com.bottlecast.app.common.components.StatusChipStyle
import com.bottlecast.app.feature.chest.domain.BottleStanding

/**
 * A bottle's status chip (spec §1.2's chip table): each with its icon, so status is never colour
 * alone. [onParchment] draws it in ink, for the parchment card; [standalone] false reads it as part
 * of the card it sits in.
 */
@Composable
internal fun StandingChip(
    standing: BottleStanding,
    modifier: Modifier = Modifier,
    onParchment: Boolean = false,
    standalone: Boolean = true,
) {
    fun style(style: StatusChipStyle) = if (onParchment) StatusChipStyle.Parchment else style
    when (standing) {
        is BottleStanding.NotKept -> {
            val description = pluralStringResource(R.plurals.chip_not_kept_description, standing.daysLeft, standing.daysLeft)
            StatusChip(
                text = pluralStringResource(R.plurals.chip_not_kept, standing.daysLeft, standing.daysLeft),
                icon = R.drawable.ic_symbol_waves,
                style = style(StatusChipStyle.Outline),
                modifier = modifier,
                description = description,
                standalone = standalone,
            )
        }
        BottleStanding.Kept -> StatusChip(
            text = stringResource(R.string.chip_kept),
            icon = R.drawable.ic_symbol_inventory_2,
            style = style(StatusChipStyle.PrimaryContainer),
            modifier = modifier,
            standalone = standalone,
        )
        BottleStanding.Drifting -> StatusChip(
            text = stringResource(R.string.chip_drifting),
            icon = R.drawable.ic_symbol_waves,
            style = style(StatusChipStyle.Glass),
            modifier = modifier,
            standalone = standalone,
        )
        BottleStanding.Found -> StatusChip(
            text = stringResource(R.string.chip_found),
            icon = R.drawable.ic_glyph_bottle,
            style = style(StatusChipStyle.PrimaryOutline),
            modifier = modifier,
            standalone = standalone,
        )
        BottleStanding.UnderReview -> StatusChip(
            text = stringResource(R.string.chip_under_review),
            icon = R.drawable.ic_symbol_flag,
            style = style(StatusChipStyle.ErrorContainer),
            modifier = modifier,
            standalone = standalone,
        )
        BottleStanding.Removed -> StatusChip(
            text = stringResource(R.string.chip_removed),
            icon = R.drawable.ic_symbol_flag,
            style = style(StatusChipStyle.ErrorContainer),
            modifier = modifier,
            standalone = standalone,
        )
    }
}
