package com.bottlecast.app.testing

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.bottlecast.app.TopLevelDestination
import com.bottlecast.app.common.components.TabFrame

/**
 * The boards' 44dp status bar. Screens drawn under the status bar take it as their window insets,
 * so a PNG lines up with its board.
 */
internal val BoardStatusBar = WindowInsets(top = 44.dp)

/**
 * A tab root as the boards draw it: in its [TabFrame], with no system navigation bar under the
 * app's unless [navigationBarInsets] adds one. [content] gets the bar's height as its padding.
 */
@Composable
internal fun BoardTabFrame(
    selected: TopLevelDestination,
    navigationBarInsets: WindowInsets = WindowInsets(0),
    chestBadge: Boolean = false,
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (contentPadding: PaddingValues) -> Unit,
) {
    TabFrame(
        selected = selected,
        onSelect = {},
        chestBadge = chestBadge,
        navigationBarInsets = navigationBarInsets,
        snackbarHost = snackbarHost,
        content = content,
    )
}
