package com.bottlecast.app.common.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bottlecast.app.TopLevelDestination

/**
 * A tab root's frame: the screen with the navigation bar at its foot. Each tab root draws its own
 * bar, rather than the app shell, so the bar is already there under the Shore while a predictive
 * back gesture reveals it, and the Shore's content never jumps as the bar comes and goes.
 *
 * [content] draws edge to edge and receives the bar's height (system navigation bar included) as
 * its bottom padding.
 */
@Composable
fun TabFrame(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
    chestBadge: Boolean = false,
    navigationBarInsets: WindowInsets = NavigationBarDefaults.windowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            BottlingNavigationBar(
                selected = selected,
                onSelect = onSelect,
                chestBadge = chestBadge,
                windowInsets = navigationBarInsets,
            )
        },
        content = content,
    )
}
