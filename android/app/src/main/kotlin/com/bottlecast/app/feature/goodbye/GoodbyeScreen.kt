package com.bottlecast.app.feature.goodbye

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.bottlecast.app.R
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.MoonDisc
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

/**
 * After the account is deleted (spec §3.17): "Fair winds." under the moon, and the way back to the
 * Shore as a guest. The data-deletion page it will link to comes with slice 9 (open-issues.md).
 */
@Composable
fun GoodbyeScreen(
    onBackToShoreHandler: () -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    val spacing = BottlingTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(windowInsets)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.gutter, vertical = spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        MoonDisc(diameter = BottlingTheme.sizes.moonDiscLarge)
        Spacer(modifier = Modifier.height(spacing.l))
        Text(
            text = stringResource(R.string.goodbye_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = stringResource(R.string.goodbye_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.weight(1f))
        BottlingButton(text = R.string.goodbye_back, modifier = Modifier.fillMaxWidth(), onTapHandler = onBackToShoreHandler)
    }
}

@NightDawnPreviews
@Composable
private fun GoodbyeScreenPreview() = BottlingPreview {
    GoodbyeScreen(onBackToShoreHandler = {})
}
