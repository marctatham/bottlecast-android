package com.bottlecast.app.feature.shore.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import com.bottlecast.app.R
import com.bottlecast.app.common.components.OverlineText
import com.bottlecast.app.common.components.ParchmentCard
import com.bottlecast.app.common.components.StatusChip
import com.bottlecast.app.common.components.StatusChipStyle
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.common.ui.isLargeText
import com.bottlecast.app.feature.shore.ShoreDimens
import com.bottlecast.app.feature.shore.TodaysFind

/**
 * Shore B's today's-find card (spec §3.3), straddling the dome's edge: a one-line parchment
 * excerpt, its Kept or Not kept chip in ink, and a chevron. Tapping it reopens today's bottle. At
 * large text the chip moves under the excerpt.
 */
@Composable
internal fun TodaysFindCard(
    find: TodaysFind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BottlingTheme.colors
    ParchmentCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ShoreDimens.findCardMinHeight)
            .semantics(mergeDescendants = true) {},
        shape = BottlingTheme.shapes.card,
        contentPadding = PaddingValues(
            start = ShoreDimens.findCardPaddingStart,
            end = ShoreDimens.findCardPaddingEnd,
            top = ShoreDimens.findCardPaddingVertical,
            bottom = ShoreDimens.findCardPaddingVertical,
        ),
        onClick = onClick,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val largeText = isLargeText()
            Column(modifier = Modifier.weight(1f)) {
                OverlineText(text = stringResource(R.string.shore_todays_find), color = colors.inkMuted)
                Text(
                    text = stringResource(R.string.shore_find_excerpt, find.excerpt),
                    // the message runs in its own script's direction, whichever way the app is laid out
                    style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content),
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (largeText) {
                    Spacer(modifier = Modifier.height(BottlingTheme.spacing.xs))
                    KeepChip(find)
                }
            }
            if (!largeText) KeepChip(find)
            Icon(
                painter = painterResource(R.drawable.ic_symbol_chevron_right),
                contentDescription = null,
                tint = colors.inkMuted,
                modifier = Modifier.size(BottlingTheme.sizes.icon),
            )
        }
    }
}

/** "Kept", or "Not kept · 7d left", in ink on the parchment (spec §3.3, §1.2). */
@Composable
private fun KeepChip(find: TodaysFind) {
    if (find.kept) {
        StatusChip(
            text = stringResource(R.string.chip_kept),
            icon = R.drawable.ic_symbol_inventory_2,
            style = StatusChipStyle.Parchment,
            standalone = false,
        )
    } else if (find.daysLeft != null) {
        val description = pluralStringResource(R.plurals.chip_not_kept_description, find.daysLeft, find.daysLeft)
        StatusChip(
            text = pluralStringResource(R.plurals.chip_not_kept, find.daysLeft, find.daysLeft),
            icon = R.drawable.ic_symbol_waves,
            style = StatusChipStyle.Parchment,
            description = description,
            standalone = false,
        )
    }
}

@NightDawnPreviews
@Composable
private fun TodaysFindCardPreview() = BottlingPreview {
    Column(
        modifier = Modifier.padding(BottlingTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.m),
    ) {
        TodaysFindCard(
            find = TodaysFind("preview1", "Hoy el mar me devolvió algo que creía perdido: la calma.", daysLeft = 7),
            onClick = {},
        )
        TodaysFindCard(find = TodaysFind("preview2", "Hey, that's pretty cool", kept = true), onClick = {})
    }
}
