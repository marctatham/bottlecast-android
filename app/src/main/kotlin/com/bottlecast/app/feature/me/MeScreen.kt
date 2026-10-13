package com.bottlecast.app.feature.me

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.bottlecast.app.R
import com.bottlecast.app.common.components.Avatar
import com.bottlecast.app.common.components.DomeEdge
import com.bottlecast.app.common.components.DomeScaffold
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
    val sizes = BottlingTheme.sizes

    DomeScaffold(
        edge = MeDome,
        contentPadding = contentPadding,
        modifier = modifier,
        windowInsets = windowInsets,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { toArc ->
        // the avatar sits in the middle of the dome, below the status bar
        val aboveAvatar = ((toArc - sizes.profileAvatar) / 2).coerceAtLeast(0.dp)
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

// the compact dome behind the profile's avatar: its edge at y≈260 of the spec's 800dp frame (§3.12)
private val MeDome = DomeEdge(0.325f)

@NightDawnPreviews
@Composable
private fun MeScreenPreview() = BottlingPreview {
    MeScreen(onGoToShoreHandler = {}, contentPadding = PaddingValues())
}
