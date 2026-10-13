package com.bottlecast.app.feature.shore.components

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.feature.shore.ShoreDimens
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
    // the touch target reaches 48dp around the chip, while the ripple and focus ring follow it
    val shape = BottlingTheme.shapes.pill
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = ShoreDimens.reminderChipHeight)
            .clip(shape)
            .clickable(
                interactionSource = null,
                indication = ripple(focusRingShape = shape),
                role = Role.Button,
                onClick = onClick,
            )
            .background(colors.glassContainer)
            .padding(start = ShoreDimens.reminderChipPaddingStart, end = ShoreDimens.reminderChipPaddingEnd),
        horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_symbol_notifications),
            contentDescription = null,
            tint = colors.onGlassContainer,
            modifier = Modifier.size(BottlingTheme.sizes.iconSmall),
        )
        Text(
            text = stringResource(R.string.shore_reminder, clockTime(time)),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onGlassContainer,
        )
    }
}

/** [time] as the user reads clock times: in their locale, and 24-hour if the device is set to it. */
@Composable
private fun clockTime(time: LocalTime): String {
    val locale = LocalConfiguration.current.locales[0]
    val skeleton = if (DateFormat.is24HourFormat(LocalContext.current)) "Hm" else "hm"
    return time.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale))
}

@NightDawnPreviews
@Composable
private fun ReminderChipPreview() = BottlingPreview {
    ReminderChip(time = LocalTime.of(9, 0), onClick = {})
}
