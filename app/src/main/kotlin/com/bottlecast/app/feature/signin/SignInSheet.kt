package com.bottlecast.app.feature.signin

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bottlecast.app.R
import com.bottlecast.app.common.analytics.AnalyticsEvent
import com.bottlecast.app.common.analytics.LocalTracker
import com.bottlecast.app.common.components.BottlingBottomSheet
import com.bottlecast.app.common.components.BottlingTextButton
import com.bottlecast.app.common.components.LinkedText
import com.bottlecast.app.common.components.TextLink
import com.bottlecast.app.common.components.rememberBottlingSheetState
import com.bottlecast.app.common.legal.LegalLinks
import com.bottlecast.app.common.ui.BottlingMotion
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.cssBlur
import com.bottlecast.app.feature.signin.components.GoogleButton
import com.bottlecast.app.feature.signin.domain.SignInOutcome
import kotlinx.coroutines.launch

/**
 * The lazy Google sign-in sheet (spec §3.2), the only sign-in there is. A host shows it just before an
 * action that needs Google, and carries the action on from [onSignedIn]; "Not now" just closes it.
 * Its ViewModel lives with the host, and starts each opening afresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInSheet(
    reason: SignInReason,
    onDismissRequest: () -> Unit,
    onSignedIn: (SignInOutcome) -> Unit,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val tracker = LocalTracker.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnSignedIn by rememberUpdatedState(onSignedIn)
    val busy = uiState.phase == SignInPhase.Loading || uiState.phase is SignInPhase.SignedIn
    val sheetState = rememberBottlingSheetState(canHide = { !busy })
    val contentAlpha = remember { Animatable(1f) }

    // The next opening starts from Idle, whatever this one ended on. A rotation isn't a new opening:
    // a sign-in that finished as the activity was recreated still carries the action on.
    val activity = LocalActivity.current
    DisposableEffect(viewModel) { onDispose { if (activity?.isChangingConfigurations != true) viewModel.reset() } }

    LaunchedEffect(uiState.phase) {
        when (val phase = uiState.phase) {
            is SignInPhase.SignedIn -> {
                tracker.trackEvent(AnalyticsEvent.SignedIn, mapOf(AnalyticsEvent.PARAM_RESULT to phase.outcome.analyticsValue))
                // the sheet fades out, then the action carries on (spec §3.2)
                contentAlpha.animateTo(0f, BottlingMotion.micro())
                currentOnSignedIn(phase.outcome)
            }
            SignInPhase.Failed -> tracker.trackEvent(AnalyticsEvent.SignInFailed, mapOf(AnalyticsEvent.PARAM_REASON to "failed"))
            SignInPhase.Unavailable ->
                tracker.trackEvent(AnalyticsEvent.SignInFailed, mapOf(AnalyticsEvent.PARAM_REASON to "unavailable"))
            else -> Unit
        }
    }

    SignInSheet(
        uiState = uiState,
        reason = reason,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        onContinue = { viewModel.onContinue(context) },
        onNotNow = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() } },
        contentAlpha = { contentAlpha.value },
    )
}

/** The sheet itself, for previews and screenshot tests. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SignInSheet(
    uiState: SignInUiState,
    reason: SignInReason,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onContinue: () -> Unit,
    onNotNow: () -> Unit,
    contentAlpha: () -> Float = { 1f },
) {
    val busy = uiState.phase == SignInPhase.Loading || uiState.phase is SignInPhase.SignedIn
    BottlingBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dismissible = !busy,
    ) {
        SignInSheetContent(
            uiState = uiState,
            reason = reason,
            onContinue = onContinue,
            onNotNow = onNotNow,
            modifier = Modifier.graphicsLayer { alpha = contentAlpha() },
        )
    }
}

@Composable
private fun ColumnScope.SignInSheetContent(
    uiState: SignInUiState,
    reason: SignInReason,
    onContinue: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BottlingTheme.spacing
    val colors = BottlingTheme.colors
    val loading = uiState.phase == SignInPhase.Loading
    Column(
        modifier = modifier
            .fillMaxWidth()
            // large text can make it taller than the screen (spec §4)
            .verticalScroll(rememberScrollState())
            .padding(start = spacing.l, end = spacing.l, bottom = spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SignInGlyph()
        Spacer(modifier = Modifier.height(spacing.m))
        Text(
            text = stringResource(reason.title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = stringResource(R.string.sign_in_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        carryOverText(uiState)?.let { carry ->
            Spacer(modifier = Modifier.height(spacing.m))
            CarryOverChip(text = carry)
        }
        Spacer(modifier = Modifier.height(spacing.l))
        val problem = when (uiState.phase) {
            SignInPhase.Failed -> R.string.sign_in_error
            SignInPhase.Unavailable -> R.string.sign_in_unavailable
            else -> null
        }
        if (problem != null) {
            InlineProblem(text = stringResource(problem))
            Spacer(modifier = Modifier.height(spacing.xs))
        }
        GoogleButton(onClick = onContinue, enabled = !uiState.isOffline, isLoading = loading)
        if (uiState.isOffline) {
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = stringResource(R.string.sign_in_offline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(spacing.xs))
        BottlingTextButton(text = R.string.sign_in_not_now, onTapHandler = onNotNow, enabled = !loading)
        Spacer(modifier = Modifier.height(spacing.m))
        LinkedText(
            template = stringResource(R.string.sign_in_legal),
            links = listOf(
                TextLink(stringResource(R.string.sign_in_legal_terms), LegalLinks.TERMS),
                TextLink(stringResource(R.string.sign_in_legal_privacy), LegalLinks.PRIVACY),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            linkColor = colors.glass,
            textAlign = TextAlign.Center,
        )
    }
}

/** The 64dp bottle glyph in glass, with the moon rising over its edge (spec §3.2). Decorative. */
@Composable
private fun SignInGlyph() {
    val colors = BottlingTheme.colors
    Box(modifier = Modifier.clearAndSetSemantics {}) {
        Box(
            modifier = Modifier
                .size(SignInDimens.glyph)
                .background(colors.glassContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_glyph_bottle),
                contentDescription = null,
                tint = colors.glass,
                modifier = Modifier.size(SignInDimens.glyphIcon),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = SignInDimens.moonDotOffset.x, y = SignInDimens.moonDotOffset.y)
                .size(SignInDimens.moonDot),
            contentAlignment = Alignment.Center,
        ) {
            // the glow is its own circle grown by the spread, as MoonDisc's halo is
            Box(
                modifier = Modifier
                    .requiredSize(SignInDimens.moonDot + SignInDimens.moonDotGlowSpread * 2)
                    .dropShadow(CircleShape) {
                        radius = cssBlur(SignInDimens.moonDotGlowBlur).toPx()
                        color = colors.moon.copy(alpha = SignInDimens.MOON_DOT_GLOW_ALPHA)
                    },
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(colors.moonDisc, CircleShape),
            )
        }
    }
}

/**
 * What a guest brings along when it signs in (spec §3.2): its streak and today's find, or either, or
 * null when there's nothing to carry. A one-day streak is just today's find.
 */
@Composable
private fun carryOverText(uiState: SignInUiState): String? {
    val streak = uiState.carriedStreak
    return when {
        uiState.carriesTodaysFind && streak >= 2 -> pluralStringResource(R.plurals.sign_in_carry_streak_and_find, streak, streak)
        uiState.carriesTodaysFind -> stringResource(R.string.sign_in_carry_find)
        streak >= 1 -> pluralStringResource(R.plurals.sign_in_carry_streak, streak, streak)
        else -> null
    }
}

/** The carry-over chip, such as "Today's find comes with you", on a primaryContainer pill with the waves (spec §3.2). */
@Composable
private fun CarryOverChip(text: String) {
    val sizes = BottlingTheme.sizes
    Row(
        modifier = Modifier
            .heightIn(min = sizes.chipMinHeight)
            .background(MaterialTheme.colorScheme.primaryContainer, BottlingTheme.shapes.pill)
            .padding(
                start = SignInDimens.chipPaddingStart,
                end = SignInDimens.chipPaddingEnd,
                top = SignInDimens.chipPaddingVertical,
                bottom = SignInDimens.chipPaddingVertical,
            ),
        horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_symbol_waves),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(sizes.iconSmall),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** An error just above the button, with an icon so it isn't colour alone, read out as it appears. */
@Composable
private fun InlineProblem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_symbol_info),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(BottlingTheme.sizes.iconSmall),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
