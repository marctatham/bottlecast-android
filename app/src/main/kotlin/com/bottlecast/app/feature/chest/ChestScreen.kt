package com.bottlecast.app.feature.chest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.bottlecast.app.R
import com.bottlecast.app.common.components.Dome
import com.bottlecast.app.common.components.DomeEdge
import com.bottlecast.app.common.components.EmptyState
import com.bottlecast.app.common.components.MoonDisc
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

/**
 * The Chest tab. Keeping arrives in slice 4, so until then the chest is honestly empty: its
 * compact dome header, then the spec's own empty state (spec §3.10).
 *
 * @param contentPadding the navigation bar's height.
 */
@Composable
fun ChestScreen(
    onGoToShoreHandler: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
) {
    val spacing = BottlingTheme.spacing
    val density = LocalDensity.current
    val topInset = with(density) { windowInsets.getTop(density).toDp() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val arcY = maxHeight * ChestDome.fraction
        Dome(edge = ChestDome)
        MoonDisc(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = topInset + MOON_TOP, end = MOON_END),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .windowInsetsPadding(windowInsets)
                .padding(bottom = contentPadding.calculateBottomPadding())
                .padding(horizontal = spacing.gutter),
        ) {
            // the header sits in the dome, like the Shore's top row
            Box(
                modifier = Modifier
                    .padding(top = spacing.xxs)
                    .fillMaxWidth()
                    .heightIn(min = BottlingTheme.sizes.touchTarget),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = stringResource(R.string.chest_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
            }
            // below the arc, the empty state has the rest of the screen
            Spacer(modifier = Modifier.height((arcY - topInset - spacing.xxs - BottlingTheme.sizes.touchTarget).coerceAtLeast(0.dp)))
            Spacer(modifier = Modifier.weight(1f))
            EmptyState(
                title = stringResource(R.string.chest_empty_title),
                body = stringResource(R.string.chest_empty_body),
                actionLabel = stringResource(R.string.go_to_shore),
                onAction = onGoToShoreHandler,
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

// the compact dome behind the Chest's header: its edge at y≈180 of the spec's 800dp frame (§3.10)
private val ChestDome = DomeEdge(0.225f)

// the Chest-Found board's moon sits top-end, just below the status bar
private val MOON_TOP = 8.dp
private val MOON_END = 32.dp

@NightDawnPreviews
@Composable
private fun ChestScreenPreview() = BottlingPreview {
    ChestScreen(onGoToShoreHandler = {}, contentPadding = PaddingValues())
}
