package com.bottlecast.app.feature.discover.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.bottlecast.app.R
import com.bottlecast.app.common.components.BottlingBottomSheet
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.rememberBottlingSheetState
import com.bottlecast.app.common.ui.BottlingTheme
import kotlinx.coroutines.launch

/** "No shaking needed" (spec §3.6, G8): every way to uncork, and that leaving costs nothing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UncorkHelpSheet(onDismissRequest: () -> Unit) {
    val sheetState = rememberBottlingSheetState()
    val scope = rememberCoroutineScope()
    BottlingBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        UncorkHelpContent(
            onGotItHandler = {
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
            },
        )
    }
}

@Composable
internal fun UncorkHelpContent(onGotItHandler: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = BottlingTheme.spacing
    Column(
        modifier = modifier.padding(start = spacing.l, end = spacing.l, bottom = spacing.l),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Text(
            text = stringResource(R.string.discover_help_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.discover_help_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BottlingButton(
            text = R.string.discover_help_got_it,
            onTapHandler = onGotItHandler,
            modifier = Modifier.padding(top = spacing.m),
        )
    }
}
