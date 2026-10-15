package com.bottlecast.app.feature.reminders.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.feature.reminders.ReminderDimens

/**
 * The reminder sheet's picture (spec §3.16a): a bottle in a glassContainer disc, with a bell at its
 * lower end and the moon at its upper end, each ringed in the sheet's colour so they seem cut out of
 * the disc (Notify-Rationale board). Decorative: the title beside it says what it's for.
 */
@Composable
internal fun ReminderIllustration(modifier: Modifier = Modifier) {
    val colors = BottlingTheme.colors
    Box(modifier = modifier.size(ReminderDimens.illustration).clearAndSetSemantics {}) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(colors.glassContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_glyph_bottle),
                contentDescription = null,
                tint = colors.glass,
                modifier = Modifier.size(ReminderDimens.illustrationBottle),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(ReminderDimens.bellBadge)
                .border(ReminderDimens.badgeRing, colors.surfaceHigh, CircleShape)
                .background(colors.glassContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_symbol_notifications),
                contentDescription = null,
                tint = colors.glass,
                modifier = Modifier.size(ReminderDimens.bellIcon),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(ReminderDimens.moonDot)
                .border(ReminderDimens.badgeRing, colors.surfaceHigh, CircleShape)
                .background(colors.moon, CircleShape),
        )
    }
}

@NightDawnPreviews
@Composable
private fun ReminderIllustrationPreview() = BottlingPreview {
    ReminderIllustration()
}
