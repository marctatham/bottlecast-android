package com.bottlecast.app.feature.reveal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import com.bottlecast.app.R
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.BottlingTextButton
import com.bottlecast.app.common.ui.BottlingTheme

/**
 * What the card collapses into once reported (spec §3.7): "It's hidden for you", with Undo while the
 * report waits out its window.
 */
@Composable
internal fun ReportedBar(onUndoHandler: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BottlingTheme.sizes.pillMinHeight)
            .clip(BottlingTheme.shapes.field)
            .background(BottlingTheme.colors.surfaceHigh)
            .padding(start = BottlingTheme.spacing.m, end = BottlingTheme.spacing.xxs)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.reveal_reported_hidden),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = BottlingTheme.spacing.s),
        )
        BottlingTextButton(text = R.string.reveal_undo, onTapHandler = onUndoHandler)
    }
}

/**
 * The report is in (spec §3.19): thanks, a word that the writer's bottles won't come again if they
 * were blocked, or that the report will go once the phone is back online.
 */
@Composable
internal fun ReportedThanks(
    blocked: Boolean,
    queuedOffline: Boolean,
    onBackToShoreHandler: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BottlingTheme.spacing
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_symbol_flag_filled),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(BottlingTheme.sizes.icon),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        ) {
            Text(
                text = stringResource(if (queuedOffline) R.string.reveal_reported_offline else R.string.reveal_reported_thanks),
                style = MaterialTheme.typography.titleMedium.copy(lineBreak = LineBreak.Heading),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            if (blocked) {
                Text(
                    text = stringResource(R.string.reveal_reported_blocked),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        BottlingButton(
            text = R.string.reveal_back_to_shore,
            onTapHandler = onBackToShoreHandler,
            modifier = Modifier.padding(top = spacing.m),
        )
    }
}
