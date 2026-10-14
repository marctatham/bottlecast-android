package com.bottlecast.app.common.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bottlecast.app.common.ui.BottlingTheme

/** Snackbars on surfaceHigh with a primary action (spec §3.18), such as a throw back's Undo. */
@Composable
fun BottlingSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = BottlingTheme.shapes.field,
            containerColor = BottlingTheme.colors.surfaceHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            actionColor = MaterialTheme.colorScheme.primary,
            actionContentColor = MaterialTheme.colorScheme.primary,
        )
    }
}
