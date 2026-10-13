package com.bottlecast.app.feature.shore.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.feature.shore.ShoreDimens
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Shore B's reminder chip (spec §3.3, §6): a glassContainer pill in the dome, shown only while
 * notifications are off. It opens the reminder sheet (slice 4).
 */
@Composable
internal fun ReminderChip(
    time: LocalTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BottlingTheme.colors
    val sizes = BottlingTheme.sizes
    Box(
        modifier = modifier
            .heightIn(min = sizes.touchTarget)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = ShoreDimens.reminderChipHeight)
                .clip(BottlingTheme.shapes.pill)
                .background(colors.glassContainer)
                .padding(start = ShoreDimens.reminderChipPaddingStart, end = ShoreDimens.reminderChipPaddingEnd),
            horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_symbol_notifications),
                contentDescription = null,
                tint = colors.onGlassContainer,
                modifier = Modifier.size(sizes.iconSmall),
            )
            Text(
                text = stringResource(R.string.shore_reminder, time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onGlassContainer,
            )
        }
    }
}

@NightDawnPreviews
@Composable
private fun ReminderChipPreview() = BottlingPreview {
    ReminderChip(time = LocalTime.of(9, 0), onClick = {})
}
