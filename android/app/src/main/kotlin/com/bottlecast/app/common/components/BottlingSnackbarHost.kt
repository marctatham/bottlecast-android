package com.bottlecast.app.common.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

/**
 * Shows a snackbar for [millis], or until it's dismissed when [millis] is null: what
 * rememberTimedControlMillis gives, so an action stays up for at least the system's recommended
 * timeout, and for as long as needed while TalkBack is on (spec §4).
 */
suspend fun SnackbarHostState.showTimed(message: String, actionLabel: String?, millis: Long?): SnackbarResult = coroutineScope {
    val timer = millis?.let {
        launch {
            delay(it)
            currentSnackbarData?.dismiss()
        }
    }
    showSnackbar(message, actionLabel = actionLabel, duration = SnackbarDuration.Indefinite).also { timer?.cancel() }
}
