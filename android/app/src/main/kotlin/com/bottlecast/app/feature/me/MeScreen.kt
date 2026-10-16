package com.bottlecast.app.feature.me

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessStarted
import com.bottlecast.app.R
import com.bottlecast.app.common.components.Avatar
import com.bottlecast.app.common.components.BottlingTextButton
import com.bottlecast.app.common.components.DomeScaffold
import com.bottlecast.app.common.components.GoogleButton
import com.bottlecast.app.common.profile.domain.initialOf
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.common.ui.monthYear
import com.bottlecast.app.feature.me.components.MeStatsCard
import com.bottlecast.app.feature.penname.PenNameSheet
import com.bottlecast.app.feature.signin.SignInReason
import com.bottlecast.app.feature.signin.SignInSheet
import java.time.YearMonth

/**
 * The Me tab (spec §3.12): the avatar, the pen name and when the account began casting in a compact
 * dome, with the settings gear at its top end, and the numbers on a card straddling the arc. Without
 * a pen name it offers one; a guest is asked to sign in.
 *
 * @param contentPadding the navigation bar's height.
 */
@Composable
fun MeScreen(
    onOpenSettingsHandler: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: MeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    var choosingPenName by rememberSaveable { mutableStateOf(false) }
    var signingIn by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose {}
    }

    // The tab stays drawn while a screen opens over it, where a keyboard, TalkBack or Switch Access
    // can still reach it, so it only acts while it's on top.
    MeScreen(
        uiState = uiState,
        onOpenSettingsHandler = dropUnlessStarted(block = onOpenSettingsHandler),
        onSetPenNameHandler = dropUnlessStarted { choosingPenName = true },
        onSignInHandler = dropUnlessStarted { signingIn = true },
        contentPadding = contentPadding,
        modifier = modifier,
    )

    if (choosingPenName) {
        PenNameSheet(onDismissRequest = { choosingPenName = false }, onSaved = { choosingPenName = false })
    }
    if (signingIn) {
        SignInSheet(
            reason = SignInReason.Account,
            onDismissRequest = { signingIn = false },
            onSignedIn = { signingIn = false },
        )
    }
}

/** The stateless Me tab, for previews and screenshot tests. */
@Composable
internal fun MeScreen(
    uiState: MeUiState,
    onOpenSettingsHandler: () -> Unit,
    onSetPenNameHandler: () -> Unit,
    onSignInHandler: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
) {
    val spacing = BottlingTheme.spacing
    val sizes = BottlingTheme.sizes
    val showing = uiState as? MeUiState.Showing

    DomeScaffold(
        edge = MeDimens.dome,
        contentPadding = contentPadding,
        modifier = modifier,
        windowInsets = windowInsets,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { toArc ->
        // everything in the dome, which ends where the stats card can straddle the arc
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = (toArc - MeDimens.cardAboveArc).coerceAtLeast(0.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Avatar(
                    diameter = sizes.profileAvatar,
                    glyphSize = sizes.profileAvatarGlyph,
                    rim = sizes.profileAvatarRim,
                    initial = (showing?.identity as? MeIdentity.PenName)?.let { initialOf(it.name) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = spacing.xxs),
                )
                IconButton(onClick = onOpenSettingsHandler, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_symbol_settings),
                        contentDescription = stringResource(R.string.me_settings),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (showing != null) {
                Spacer(modifier = Modifier.height(spacing.s))
                Text(
                    text = when (val identity = showing.identity) {
                        MeIdentity.Guest -> stringResource(R.string.me_guest_name)
                        MeIdentity.Anonymous -> stringResource(R.string.me_anonymous_name)
                        is MeIdentity.PenName -> identity.name
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                showing.castingSince?.let { since ->
                    Text(
                        text = stringResource(R.string.me_casting_since, monthYear(since)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                if (showing.identity == MeIdentity.Anonymous) {
                    BottlingTextButton(text = R.string.me_set_pen_name, onTapHandler = onSetPenNameHandler)
                }
                Spacer(modifier = Modifier.height(spacing.m))
            }
        }
        if (showing != null) {
            MeStatsCard(stats = showing.stats)
            if (showing.identity == MeIdentity.Guest) {
                Spacer(modifier = Modifier.height(spacing.l))
                Text(
                    text = stringResource(R.string.me_guest_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(spacing.m))
                GoogleButton(onClick = onSignInHandler, enabled = true, isLoading = false)
            }
        }
        Spacer(modifier = Modifier.height(spacing.l))
    }
}

@NightDawnPreviews
@Composable
private fun MeScreenPreview() = BottlingPreview {
    MeScreen(
        uiState = MeUiState.Showing(
            identity = MeIdentity.PenName("Driftwood"),
            castingSince = YearMonth.of(2026, 10),
            stats = MeStats(guest = false, bottled = 12, found = 30, kept = 9, streak = 6, best = 14, shields = 1),
        ),
        onOpenSettingsHandler = {},
        onSetPenNameHandler = {},
        onSignInHandler = {},
        contentPadding = PaddingValues(),
    )
}
