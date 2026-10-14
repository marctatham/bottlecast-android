package com.bottlecast.app.feature.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.bottlecast.app.R
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.ui.BottlingTheme

/**
 * Compose's bottom bar (spec §3.4): surfaceHigh, riding the keyboard, with the kindness line while the
 * keyboard is closed and the Preview pill. Preview waits for 10 characters, and a tap before then
 * says so instead.
 *
 * @param windowInsets the navigation bar and the keyboard, which the bar sits above.
 */
@Composable
internal fun ComposeBottomBar(
    canPreview: Boolean,
    imeVisible: Boolean,
    onPreview: () -> Unit,
    onPreviewTooShort: () -> Unit,
    windowInsets: WindowInsets,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BottlingTheme.colors.surfaceHigh)
            .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(BottlingTheme.spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!imeVisible) {
            Text(
                text = stringResource(R.string.compose_kindness),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(BottlingTheme.spacing.s))
        }
        BottlingButton(
            text = R.string.compose_preview,
            enabled = canPreview,
            onDisabledTapHandler = onPreviewTooShort,
            onTapHandler = onPreview,
        )
    }
}
