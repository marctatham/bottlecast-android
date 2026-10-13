package com.bottlecast.app.feature.me

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.bottlecast.app.R
import com.bottlecast.app.common.components.Avatar
import com.bottlecast.app.common.components.Dome
import com.bottlecast.app.common.components.DomeEdge
import com.bottlecast.app.common.components.EmptyState
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

/**
 * The Me tab. The profile arrives in slice 4, so until then it says so: the avatar in its compact
 * dome (spec §3.12), then a short note on what's coming.
 *
 * @param contentPadding the navigation bar's height.
 */
@Composable
fun MeScreen(
    onGoToShoreHandler: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
) {
    val spacing = BottlingTheme.spacing
    val sizes = BottlingTheme.sizes
    val density = LocalDensity.current
    val topInset = with(density) { windowInsets.getTop(density).toDp() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val arcY = maxHeight * MeDome.fraction
        // the avatar sits in the middle of the dome, below the status bar
        val aboveAvatar = ((arcY - topInset - sizes.profileAvatar) / 2).coerceAtLeast(0.dp)
        Dome(edge = MeDome)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .windowInsetsPadding(windowInsets)
                .padding(bottom = contentPadding.calculateBottomPadding())
                .padding(horizontal = spacing.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(aboveAvatar))
            Avatar(diameter = sizes.profileAvatar, glyphSize = sizes.profileAvatarGlyph, rim = sizes.profileAvatarRim)
            Spacer(modifier = Modifier.height(aboveAvatar))
            Spacer(modifier = Modifier.weight(1f))
            EmptyState(
                title = stringResource(R.string.me_placeholder_title),
                body = stringResource(R.string.me_placeholder_body),
                actionLabel = stringResource(R.string.go_to_shore),
                onAction = onGoToShoreHandler,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

// the compact dome behind the profile's avatar: its edge at y≈260 of the spec's 800dp frame (§3.12)
private val MeDome = DomeEdge(0.325f)

@NightDawnPreviews
@Composable
private fun MeScreenPreview() = BottlingPreview {
    MeScreen(onGoToShoreHandler = {}, contentPadding = PaddingValues())
}
