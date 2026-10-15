package com.bottlecast.app.common.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bottlecast.app.TopLevelDestination

/**
 * A tab root's frame: the screen with the navigation bar at its foot. Each tab root draws its own
 * bar, rather than the app shell, so the bar is already there under the Shore while a predictive
 * back gesture reveals it, and the Shore's content never jumps as the bar comes and goes.
 *
 * [content] draws edge to edge and receives the bar's height (system navigation bar included) as
 * its bottom padding. Snackbars from [snackbarHost] sit just above the bar.
 *
 * The bar only switches tabs while its tab root is started, which means on top, even
 * mid-transition. A root stays drawn while a screen opens over it: taps can't reach it there, but a
 * keyboard, TalkBack or Switch Access still can.
 */
@Composable
fun TabFrame(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
    chestBadge: Boolean = false,
    meInitial: String? = null,
    navigationBarInsets: WindowInsets = NavigationBarDefaults.windowInsets,
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = snackbarHost,
        bottomBar = {
            BottlingNavigationBar(
                selected = selected,
                onSelect = { destination ->
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) onSelect(destination)
                },
                chestBadge = chestBadge,
                meInitial = meInitial,
                windowInsets = navigationBarInsets,
            )
        },
        content = content,
    )
}
