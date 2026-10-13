package com.bottlecast.app.common.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

/**
 * The template for edge, empty and error states (spec §3.18): an optional 160dp spot
 * illustration, the title, up to three lines of body, then one pill, and an optional reassuring
 * [note] under it. Copy never blames the user and always says what to do next.
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes actionIcon: Int? = null,
    note: String? = null,
    illustration: (@Composable () -> Unit)? = null,
) {
    val spacing = BottlingTheme.spacing
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (illustration != null) {
            Column(
                modifier = Modifier.size(BottlingTheme.sizes.spotIllustration),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { illustration() }
            Spacer(modifier = Modifier.height(spacing.m))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(spacing.l))
        BottlingButton(text = actionLabel, leadingIcon = actionIcon, onTapHandler = onAction)
        if (note != null) {
            Spacer(modifier = Modifier.height(spacing.s))
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@NightDawnPreviews
@Composable
private fun EmptyStatePreview() = BottlingPreview {
    EmptyState(
        title = "Your chest is empty",
        body = "Today's bottle is waiting on the shore.",
        actionLabel = "Go to the shore",
        onAction = {},
        modifier = Modifier.padding(BottlingTheme.spacing.m),
    )
}
