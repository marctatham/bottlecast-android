package com.bottlecast.app.feature.shore.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.feature.shore.ShoreStreak

/**
 * The Shore's top row (spec §3.3): the wordmark at the start; the streak pill at the end, when
 * there's a streak to show; then, for admins only, an overflow menu with "Moderation".
 */
@Composable
internal fun ShoreTopRow(
    streak: ShoreStreak?,
    isAdmin: Boolean,
    onStreakHandler: () -> Unit,
    onModerationHandler: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // at large text sizes the streak pill moves under the wordmark rather than squeezing it
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BottlingTheme.sizes.touchTarget),
        horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xs, Alignment.End),
        verticalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.shore_wordmark),
            style = BottlingTheme.typography.wordmark,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (streak != null) {
            StreakPill(streak = streak, onClick = onStreakHandler)
        }
        if (isAdmin) {
            AdminMenu(onModerationHandler = onModerationHandler)
        }
    }
}

@Composable
private fun AdminMenu(onModerationHandler: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_symbol_more_vert),
                contentDescription = stringResource(R.string.shore_more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = BottlingTheme.colors.surfaceHigh,
            shape = BottlingTheme.shapes.field,
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.shore_moderation), style = MaterialTheme.typography.bodyLarge) },
                onClick = {
                    expanded = false
                    onModerationHandler()
                },
            )
        }
    }
}

@NightDawnPreviews
@Composable
private fun ShoreTopRowPreview() = BottlingPreview {
    ShoreTopRow(
        streak = ShoreStreak(days = 6, shields = 1),
        isAdmin = true,
        onStreakHandler = {},
        onModerationHandler = {},
    )
}
