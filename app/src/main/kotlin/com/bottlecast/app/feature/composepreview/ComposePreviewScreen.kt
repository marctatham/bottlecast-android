package com.bottlecast.app.feature.composepreview

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bottlecast.app.R
import com.bottlecast.app.common.components.Banner
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.BottlingSnackbarHost
import com.bottlecast.app.common.components.BottlingTextButton
import com.bottlecast.app.common.components.Dome
import com.bottlecast.app.common.components.ImmersiveTopBar
import com.bottlecast.app.common.components.MessageCard
import com.bottlecast.app.common.components.OverlineText
import com.bottlecast.app.common.components.bottle.BottleRig
import com.bottlecast.app.common.components.bottle.bottleLookFor
import com.bottlecast.app.common.components.showTimed
import com.bottlecast.app.common.messages.domain.MessageLimits
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.common.ui.UNDO_WINDOW_MILLIS
import com.bottlecast.app.common.ui.rememberTimedControlMillis
import com.bottlecast.app.feature.composepreview.components.BottleSettings
import com.bottlecast.app.feature.composepreview.components.ConsentChecklist
import com.bottlecast.app.feature.composepreview.components.RecipientRow
import com.bottlecast.app.feature.composepreview.components.TermsCheckbox
import com.bottlecast.app.feature.penname.PenNameSheet
import com.bottlecast.app.feature.signin.SignInReason
import com.bottlecast.app.feature.signin.SignInSheet
import kotlinx.coroutines.launch

/**
 * Preview (spec §3.4b): the bottle and its message exactly as they'll wash up, what casting means,
 * the bottle's settings and, on a first cast, the Terms. "Cast it" casts, signing a guest in first.
 *
 * @param onCastHandler on to Cast success, with the cast's id, whether it's queued, and whether it's sealable.
 */
@Composable
fun ComposePreviewScreen(
    onEditHandler: () -> Unit,
    onCastHandler: (messageId: String, queued: Boolean, sealable: Boolean) -> Unit,
    viewModel: ComposePreviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var signingIn by rememberSaveable { mutableStateOf(false) }
    var pickingPenName by rememberSaveable { mutableStateOf(false) }
    val currentOnEdit by rememberUpdatedState(onEditHandler)
    val failedMillis = rememberTimedControlMillis(UNDO_WINDOW_MILLIS, hasAction = true)
    val failed = stringResource(R.string.preview_cast_failed)
    val tryAgain = stringResource(R.string.try_again)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ComposePreviewEffect.Cast -> onCastHandler(effect.messageId, effect.queued, effect.sealable)
                ComposePreviewEffect.CastFailed -> scope.launch {
                    if (snackbarHostState.showTimed(failed, tryAgain, failedMillis) == SnackbarResult.ActionPerformed) viewModel.onCast()
                }
                ComposePreviewEffect.NeedsSignIn -> signingIn = true
                ComposePreviewEffect.OpenPenName -> pickingPenName = true
            }
        }
    }
    LaunchedEffect(uiState) { if (uiState == ComposePreviewUiState.NoDraft) currentOnEdit() }

    when (val state = uiState) {
        is ComposePreviewUiState.Ready -> ComposePreviewScreen(
            uiState = state,
            onEdit = onEditHandler,
            onCast = viewModel::onCast,
            onSignWithPenName = viewModel::onSignWithPenName,
            onSetPenName = viewModel::onSetPenName,
            onSealable = viewModel::onSealable,
            onTermsTicked = viewModel::onTermsTicked,
            snackbarHostState = snackbarHostState,
        )
        else -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )
    }

    if (signingIn) {
        SignInSheet(
            reason = SignInReason.Cast,
            onDismissRequest = { signingIn = false },
            onSignedIn = {
                signingIn = false
                viewModel.onCast()
            },
        )
    }
    if (pickingPenName) {
        PenNameSheet(
            onDismissRequest = { pickingPenName = false },
            onSaved = {
                pickingPenName = false
                viewModel.onPenNameSaved()
            },
        )
    }
}

/** Preview itself, for previews and screenshot tests. */
@Composable
internal fun ComposePreviewScreen(
    uiState: ComposePreviewUiState.Ready,
    onEdit: () -> Unit,
    onCast: () -> Unit,
    onSignWithPenName: (Boolean) -> Unit,
    onSetPenName: () -> Unit,
    onSealable: (Boolean) -> Unit,
    onTermsTicked: (Boolean) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    scrollState: ScrollState = rememberScrollState(),
) {
    val spacing = BottlingTheme.spacing
    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Dome(edge = ComposePreviewDimens.dome)
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                        .verticalScroll(scrollState)
                        .padding(bottom = spacing.m),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ImmersiveTopBar(onBackHandler = onEdit, navigationLabel = R.string.preview_edit, title = R.string.preview_title)
                    OverlineText(text = stringResource(R.string.preview_overline))
                    Spacer(modifier = Modifier.height(spacing.s))
                    val bottleDescription = stringResource(R.string.preview_bottle_description)
                    BottleRig(
                        look = bottleLookFor(uiState.messageId),
                        modifier = Modifier
                            .height(ComposePreviewDimens.bottle)
                            .semantics { contentDescription = bottleDescription },
                    )
                    Spacer(modifier = Modifier.height(spacing.l))
                    Column(
                        modifier = Modifier.padding(horizontal = spacing.gutter),
                        verticalArrangement = Arrangement.spacedBy(spacing.s),
                    ) {
                        MessageCard(text = uiState.text, byline = previewByline(uiState.signedWith), maxHeight = null)
                        RecipientRow()
                        ConsentChecklist(sealable = uiState.sealable)
                        BottleSettings(
                            penName = uiState.penName,
                            signWithPenName = uiState.signWithPenName,
                            sealable = uiState.sealable,
                            onSignWithPenName = onSignWithPenName,
                            onSetPenName = onSetPenName,
                            onSealable = onSealable,
                        )
                        if (uiState.needsTerms) TermsCheckbox(checked = uiState.termsTicked, onCheckedChange = onTermsTicked)
                    }
                }
                // the page fades into the pinned actions as it scrolls under them
                val background = MaterialTheme.colorScheme.background
                Spacer(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(spacing.l)
                        .background(Brush.verticalGradient(listOf(background.copy(alpha = 0f), background))),
                )
                BottlingSnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = spacing.gutter),
                )
            }
            PreviewActions(
                uiState = uiState,
                onCast = onCast,
                // a first cast's Cast it waits for the Terms, at the foot of the page: show them
                onCastTooSoon = { scope.launch { scrollState.animateScrollTo(scrollState.maxValue) } },
                onEdit = onEdit,
                windowInsets = windowInsets,
            )
        }
    }
}

/** "Cast it" (or "Cast when back online"), then "Edit", pinned at the foot (spec §3.4b). */
@Composable
private fun PreviewActions(
    uiState: ComposePreviewUiState.Ready,
    onCast: () -> Unit,
    onCastTooSoon: () -> Unit,
    onEdit: () -> Unit,
    windowInsets: WindowInsets,
) {
    val spacing = BottlingTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(start = spacing.gutter, end = spacing.gutter, top = spacing.xs, bottom = spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
    ) {
        if (uiState.castsLeft == 0) {
            Banner(text = stringResource(R.string.compose_cast_limit, MessageLimits.CASTS_PER_DAY), icon = R.drawable.ic_symbol_info)
            Spacer(modifier = Modifier.height(spacing.xxs))
        }
        BottlingButton(
            text = if (uiState.isOffline) R.string.preview_cast_offline else R.string.preview_cast,
            enabled = uiState.canCast,
            isLoading = uiState.casting,
            leadingIcon = R.drawable.ic_glyph_bottle,
            onDisabledTapHandler = if (uiState.needsTerms && !uiState.termsTicked) onCastTooSoon else null,
            onTapHandler = onCast,
        )
        BottlingTextButton(text = R.string.preview_edit, onTapHandler = onEdit, enabled = !uiState.casting)
    }
}

/** "— A stranger at sea · just now", or the pen name it's signed with (spec §3.4b). */
@Composable
private fun previewByline(penName: String?): String {
    val who = if (penName == null) {
        stringResource(R.string.message_byline_stranger)
    } else {
        stringResource(R.string.message_byline_pen_name, penName)
    }
    return stringResource(R.string.message_byline_when, who, stringResource(R.string.message_cast_just_now))
}

@NightDawnPreviews
@Composable
private fun ComposePreviewScreenPreview() = BottlingPreview {
    ComposePreviewScreen(
        uiState = ComposePreviewUiState.Ready(
            messageId = "bottle00000000000013",
            text = "If you're reading this, drink some water and text someone you miss.",
            penName = "Driftwood",
        ),
        onEdit = {},
        onCast = {},
        onSignWithPenName = {},
        onSetPenName = {},
        onSealable = {},
        onTermsTicked = {},
        snackbarHostState = remember { SnackbarHostState() },
    )
}
