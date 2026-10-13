package com.bottlecast.app.feature.shore.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.feature.shore.ShoreDimens
import com.bottlecast.app.feature.shore.ShoreStreak

/**
 * The tide streak (spec §3.3): a 36dp primaryContainer pill in a 48dp target, with a wave, the
 * streak and up to two calm-sea shields (G2). At 0 it invites the user to start one.
 */
@Composable
internal fun StreakPill(
    streak: ShoreStreak,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val sizes = BottlingTheme.sizes
    val label = if (streak.days == 0) {
        stringResource(R.string.shore_streak_start)
    } else {
        pluralStringResource(R.plurals.shore_streak_days, streak.days, streak.days)
    }
    val description = if (streak.days == 0) {
        label
    } else {
        listOfNotNull(
            pluralStringResource(R.plurals.shore_streak_description, streak.days, streak.days),
            if (streak.shields > 0) {
                pluralStringResource(R.plurals.shore_streak_shields_description, streak.shields, streak.shields)
            } else {
                null
            },
        ).joinToString(separator = ", ")
    }

    Box(
        modifier = modifier
            .heightIn(min = sizes.touchTarget)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick { onClick(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = ShoreDimens.streakPillHeight)
                .clip(BottlingTheme.shapes.pill)
                .background(colorScheme.primaryContainer)
                .padding(start = ShoreDimens.streakPillPaddingStart, end = ShoreDimens.streakPillPaddingEnd),
            horizontalArrangement = Arrangement.spacedBy(sizes.chipIconGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_symbol_waves),
                contentDescription = null,
                tint = colorScheme.onPrimaryContainer,
                modifier = Modifier.size(sizes.iconSmall),
            )
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = colorScheme.onPrimaryContainer)
            repeat(streak.shields.coerceAtMost(MAX_SHIELDS)) {
                Icon(
                    painter = painterResource(R.drawable.ic_symbol_shield),
                    contentDescription = null,
                    tint = BottlingTheme.colors.moon,
                    modifier = Modifier.size(ShoreDimens.shieldIcon),
                )
            }
        }
    }
}

// a user holds at most two calm-sea shields (G2)
private const val MAX_SHIELDS = 2

@NightDawnPreviews
@Composable
private fun StreakPillPreview() = BottlingPreview {
    Column(modifier = Modifier.padding(BottlingTheme.spacing.m)) {
        StreakPill(streak = ShoreStreak(days = 6, shields = 1), onClick = {})
        StreakPill(streak = ShoreStreak(days = 0, shields = 0), onClick = {})
    }
}
