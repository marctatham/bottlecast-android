package com.bottlecast.app.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

/**
 * A screen under the dome (spec §1.4): the Shore, and the Chest and Me with their compact domes.
 * The dome and the [scenery] draw edge to edge, up under the status bar, while the [content]
 * keeps inside [windowInsets]. The content scrolls only when large text makes it taller than the
 * screen (spec §4), and then within the space below the status bar, so nothing slides up under
 * the see-through bar.
 *
 * @param edge where the dome's edge sits.
 * @param flash moonlight washing over the dome, read at draw time (see [Dome]).
 * @param contentPadding the navigation bar's height, which the content ends above.
 * @param windowInsets the status bar and cutouts the content keeps clear of. The navigation bar's
 *   height arrives in [contentPadding] instead, so it isn't counted twice. They're measured whole,
 *   to place the arc, so no ancestor may consume them first.
 * @param scenery what sits on the dome, behind the content, such as the moon and stars. It gets
 *   [windowInsets] as padding, so it can keep clear of them too.
 * @param content a column inside the side gutters. It's given the distance from its top down to
 *   the dome's edge, for content that lines up with the arc.
 */
@Composable
fun DomeScaffold(
    edge: DomeEdge,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    glowCenter: Offset = DomeGlowTopEnd,
    edgeOffset: () -> Dp = { 0.dp },
    flash: () -> Float = { 0f },
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    scenery: @Composable BoxScope.(insets: PaddingValues) -> Unit = {},
    content: @Composable ColumnScope.(toArc: Dp) -> Unit,
) {
    val insets = windowInsets.asPaddingValues()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val topInset = insets.calculateTopPadding()
        val viewportHeight = maxHeight - topInset - insets.calculateBottomPadding()
        val toArc = maxHeight * edge.fraction - topInset
        Dome(edge = edge, glowCenter = glowCenter, edgeOffset = edgeOffset, flash = flash)
        scenery(insets)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets)
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
                .padding(bottom = contentPadding.calculateBottomPadding())
                .padding(horizontal = BottlingTheme.spacing.gutter),
            horizontalAlignment = horizontalAlignment,
        ) {
            content(toArc)
        }
    }
}

@NightDawnPreviews
@Composable
private fun DomeScaffoldPreview() = BottlingPreview {
    DomeScaffold(edge = DomeEdge.Full, contentPadding = PaddingValues()) { toArc ->
        Spacer(modifier = Modifier.height(toArc))
        Text(text = stringResource(R.string.chest_empty_title), style = MaterialTheme.typography.titleLarge)
    }
}
