package com.bottlecast.app.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bottlecast.app.R
import com.bottlecast.app.TopLevelDestination
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

/**
 * The Shore/Chest/Me bar (spec §2): surfaceHigh, at least 80dp tall so it grows with the text,
 * labels always visible in labelLarge (spec §1.3) and a primaryContainer pill behind the selected
 * tab. Only the tab roots show it; every other route is immersive.
 *
 * @param chestBadge a moon dot on the Chest, for bottles not kept yet or waiting to seal (slice 4).
 * @param windowInsets the system navigation bar the bar extends behind; screenshot tests pass none.
 */
@Composable
fun BottlingNavigationBar(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
    chestBadge: Boolean = false,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
) {
    val colorScheme = MaterialTheme.colorScheme
    NavigationBar(
        modifier = modifier,
        containerColor = BottlingTheme.colors.surfaceHigh,
        contentColor = colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp,
        windowInsets = windowInsets,
    ) {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == selected,
                onClick = { onSelect(destination) },
                icon = {
                    when (destination) {
                        TopLevelDestination.SHORE -> TabIcon(R.drawable.ic_glyph_shore)
                        TopLevelDestination.CHEST -> TabIcon(R.drawable.ic_symbol_inventory_2, badge = chestBadge)
                        TopLevelDestination.ME -> NavAvatar()
                    }
                },
                label = { Text(text = stringResource(destination.label), style = MaterialTheme.typography.labelLarge) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colorScheme.onPrimaryContainer,
                    selectedTextColor = colorScheme.onSurface,
                    indicatorColor = colorScheme.primaryContainer,
                    unselectedIconColor = colorScheme.onSurfaceVariant,
                    unselectedTextColor = colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun TabIcon(icon: Int, badge: Boolean = false) {
    val sizes = BottlingTheme.sizes
    Box {
        // the tab's label names it, so the icon itself is decorative
        Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(sizes.icon))
        if (badge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = BADGE_OFFSET_X, y = BADGE_OFFSET_Y)
                    .size(sizes.badgeDot)
                    .background(BottlingTheme.colors.moon, CircleShape)
            )
        }
    }
}

/**
 * The Me tab's avatar: a plain moonContainer circle (decisions.md, 2026-10-13). It shows a person
 * glyph until slice 4 settles which name its initial comes from.
 */
@Composable
private fun NavAvatar() {
    val colors = BottlingTheme.colors
    Box(
        modifier = Modifier
            .size(BottlingTheme.sizes.navAvatar)
            .background(colors.moonContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_symbol_person),
            contentDescription = null,
            tint = colors.onMoonContainer,
            modifier = Modifier.size(AVATAR_GLYPH),
        )
    }
}

// the Main board's dot sits on the icon's top-end corner
private val BADGE_OFFSET_X = 4.dp
private val BADGE_OFFSET_Y = (-1).dp
private val AVATAR_GLYPH = 16.dp

@NightDawnPreviews
@Composable
private fun BottlingNavigationBarPreview() = BottlingPreview {
    Column {
        BottlingNavigationBar(selected = TopLevelDestination.SHORE, onSelect = {}, chestBadge = true)
        BottlingNavigationBar(selected = TopLevelDestination.ME, onSelect = {})
    }
}
