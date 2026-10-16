package com.bottlecast.app.feature.chest.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import com.bottlecast.app.R
import com.bottlecast.app.common.components.bottle.BottleGlyph
import com.bottlecast.app.common.components.bottle.bottleLookFor
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.common.ui.foreignLanguageName
import com.bottlecast.app.common.ui.isLargeText
import com.bottlecast.app.common.ui.shortDate
import com.bottlecast.app.feature.chest.ChestEntry
import com.bottlecast.app.feature.chest.ChestMeta
import com.bottlecast.app.feature.chest.domain.BottleStanding
import java.time.LocalDate

/**
 * A bottle in the chest (spec §3.10): a surface card with its 40dp glyph, a two-line excerpt, the meta
 * line and its status chip at the end, or under the text at large text sizes. Tapping it opens
 * Keepsake detail.
 */
@Composable
internal fun ChestCard(entry: ChestEntry, today: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = BottlingTheme.spacing
    val shape = BottlingTheme.shapes.card
    val largeText = isLargeText()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(interactionSource = null, indication = ripple(focusRingShape = shape), role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(spacing.m),
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        verticalAlignment = Alignment.Top,
    ) {
        BottleGlyph(look = bottleLookFor(entry.messageId))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.excerpt ?: stringResource(R.string.keepsake_removed),
                // the message runs in its own script's direction, whichever way the app is laid out
                style = if (entry.excerpt != null) {
                    MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content)
                } else {
                    MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic)
                },
                color = if (entry.excerpt != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(spacing.xxs))
            Text(
                text = metaText(entry.meta, today),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (largeText) {
                Spacer(modifier = Modifier.height(spacing.xs))
                StandingChip(entry.standing, standalone = false)
            }
        }
        if (!largeText) StandingChip(entry.standing, standalone = false)
    }
}

/** "Found 5 Oct · Spanish", "Cast today", or an unkept bottle's last days (spec §3.10, §3.18). */
@Composable
private fun metaText(meta: ChestMeta, today: LocalDate): String = when (meta) {
    is ChestMeta.Found -> {
        val found = when (meta.date) {
            null -> stringResource(R.string.chip_found)
            today -> stringResource(R.string.chest_meta_found_today)
            else -> stringResource(R.string.chest_meta_found, shortDate(meta.date, today))
        }
        foreignLanguageName(meta.lang)?.let { stringResource(R.string.chest_meta_with_language, found, it) } ?: found
    }
    is ChestMeta.Cast -> when (meta.date) {
        null, today -> stringResource(R.string.chest_meta_cast_today)
        else -> stringResource(R.string.chest_meta_cast, shortDate(meta.date, today))
    }
    ChestMeta.DriftsTomorrow -> stringResource(R.string.chest_meta_drifts_tomorrow)
    ChestMeta.DriftsToday -> stringResource(R.string.chest_meta_drifts_today)
}

@NightDawnPreviews
@Composable
private fun ChestCardPreview() = BottlingPreview {
    val today = LocalDate.of(2026, 10, 12)
    Column(
        modifier = Modifier.padding(BottlingTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.s),
    ) {
        ChestCard(
            entry = ChestEntry("preview1", "Hoy el mar me devolvió algo que creía perdido: la calma.", BottleStanding.Kept, ChestMeta.Found(today, "es")),
            today = today,
            onClick = {},
        )
        ChestCard(
            entry = ChestEntry("preview2", "Somewhere a lighthouse is blinking just for you.", BottleStanding.NotKept(5), ChestMeta.Found(today.minusDays(2), "en")),
            today = today,
            onClick = {},
        )
    }
}
