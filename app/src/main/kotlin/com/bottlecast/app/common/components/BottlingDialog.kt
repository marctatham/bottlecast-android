package com.bottlecast.app.common.components

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

/**
 * A dialog on surfaceHigh with 32dp corners and 24dp padding (spec §3.19), over the theme's own
 * scrim. The platform's window dim is always black, but Dawn's scrim is #1A2320 at 40% (spec §1.2),
 * so the dim is turned off and the scrim drawn here. A tap on the scrim dismisses, as the dim's
 * would; back dismisses too. The card scrolls when large text makes it taller than the screen.
 */
@Composable
fun BottlingDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BottlingTheme.colors.scrim)
                .pointerInput(onDismissRequest) { detectTapGestures { onDismissRequest() } }
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = BottlingTheme.sizes.dialogSideMargin, vertical = BottlingTheme.spacing.l),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = modifier
                    .widthIn(max = BottlingTheme.sizes.dialogMaxWidth)
                    .fillMaxWidth()
                    .clip(BottlingTheme.shapes.dialog)
                    .background(BottlingTheme.colors.surfaceHigh)
                    // taps on the card stay on the card, rather than reaching the scrim behind it
                    .pointerInput(Unit) { detectTapGestures {} }
                    .verticalScroll(rememberScrollState())
                    .padding(BottlingTheme.spacing.l),
            ) {
                // as a Surface would: text and icons on the card take onSurface unless they say otherwise
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                    content()
                }
            }
        }
    }
}

@NightDawnPreviews
@Composable
private fun BottlingDialogPreview() = BottlingPreview {
    BottlingDialog(onDismissRequest = {}) {
        Text(text = "Report this bottle?", style = MaterialTheme.typography.headlineSmall)
    }
}
