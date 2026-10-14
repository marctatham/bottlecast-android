package com.bottlecast.app.feature.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bottlecast.app.R
import com.bottlecast.app.common.components.Banner
import com.bottlecast.app.common.components.BottlingSnackbarHost
import com.bottlecast.app.common.components.ImmersiveTopBar
import com.bottlecast.app.common.components.StatusChip
import com.bottlecast.app.common.components.StatusChipStyle
import com.bottlecast.app.common.components.TopBarNavigation
import com.bottlecast.app.common.components.showTimed
import com.bottlecast.app.common.messages.domain.MessageLimits
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.common.ui.UNDO_WINDOW_MILLIS
import com.bottlecast.app.common.ui.rememberHaptics
import com.bottlecast.app.common.ui.rememberTimedControlMillis
import com.bottlecast.app.feature.compose.components.CastInputLimit
import com.bottlecast.app.feature.compose.components.ComposeBottomBar
import com.bottlecast.app.feature.compose.components.DiscardDraftDialog
import com.bottlecast.app.feature.compose.components.ParchmentEditor
import com.bottlecast.app.feature.compose.domain.DailyPrompt
import com.bottlecast.app.feature.compose.domain.DraftMeter
import com.bottlecast.app.feature.compose.domain.composeHelper
import com.bottlecast.app.feature.penname.PenNameSheet
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Compose (spec §3.4): a close X, the parchment editor, and Preview in a bar that rides the keyboard.
 * Writing is saved as a draft; closing with words written asks before tossing them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComposeScreen(
    onCloseHandler: () -> Unit,
    onPreviewHandler: () -> Unit,
    viewModel: ComposeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pickingPenName by rememberSaveable { mutableStateOf(false) }
    val snackbarMillis = rememberTimedControlMillis(UNDO_WINDOW_MILLIS, hasAction = true)
    val restored = stringResource(R.string.compose_draft_restored)
    val startFresh = stringResource(R.string.compose_start_fresh)
    val signedIn = stringResource(R.string.sign_in_signed_in)
    val pickOne = stringResource(R.string.sign_in_pick_pen_name)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ComposeEffect.RestoredDraft -> scope.launch {
                    if (snackbarHostState.showTimed(restored, startFresh, snackbarMillis) == SnackbarResult.ActionPerformed) {
                        viewModel.onStartFresh()
                    }
                }
                ComposeEffect.SignedIn -> scope.launch {
                    if (snackbarHostState.showTimed(signedIn, pickOne, snackbarMillis) == SnackbarResult.ActionPerformed) {
                        pickingPenName = true
                    }
                }
                ComposeEffect.Rejected -> haptics.reject()
                ComposeEffect.OpenPreview -> onPreviewHandler()
                ComposeEffect.Close -> onCloseHandler()
            }
        }
    }
    BackHandler { viewModel.onClose() }

    when (val state = uiState) {
        ComposeUiState.Loading -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )
        is ComposeUiState.Writing -> ComposeScreen(
            uiState = state,
            editor = viewModel.editor,
            imeVisible = WindowInsets.isImeVisible,
            onClose = viewModel::onClose,
            onPreview = viewModel::onPreview,
            onPreviewTooShort = viewModel::onPreviewTooShort,
            onLimit = haptics::limit,
            onKeepWriting = viewModel::onKeepWriting,
            onToss = viewModel::onToss,
            snackbarHostState = snackbarHostState,
        )
    }

    if (pickingPenName) {
        PenNameSheet(onDismissRequest = { pickingPenName = false }, onSaved = { pickingPenName = false })
    }
}

/** Compose itself, for previews and screenshot tests. */
@Composable
internal fun ComposeScreen(
    uiState: ComposeUiState.Writing,
    editor: TextFieldState,
    imeVisible: Boolean,
    onClose: () -> Unit,
    onPreview: () -> Unit,
    onPreviewTooShort: () -> Unit,
    onLimit: () -> Unit,
    onKeepWriting: () -> Unit,
    onToss: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    val spacing = BottlingTheme.spacing
    val inputLimit = remember(onLimit) { CastInputLimit(onLimit) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        ) {
            // The editor fills what's left under the bar and the limit, but never drops below 240dp:
            // past that, as with large text or the keyboard up, the page scrolls (spec §4).
            val density = LocalDensity.current
            var headerHeight by remember { mutableStateOf(0.dp) }
            val editorHeight = max(ComposeDimens.editorMinHeight, maxHeight - headerHeight - spacing.xs - spacing.m)
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Column(modifier = Modifier.onSizeChanged { headerHeight = with(density) { it.height.toDp() } }) {
                    ImmersiveTopBar(onBackHandler = onClose, navigation = TopBarNavigation.Close, title = R.string.compose_title)
                    CastLimit(castsLeft = uiState.castsLeft)
                }
                ParchmentEditor(
                    state = editor,
                    placeholder = uiState.placeholder,
                    meter = uiState.meter,
                    helper = uiState.helper,
                    inputTransformation = inputLimit,
                    modifier = Modifier
                        .padding(start = spacing.gutter, end = spacing.gutter, top = spacing.xs, bottom = spacing.m)
                        .fillMaxWidth()
                        .height(editorHeight),
                )
            }
            BottlingSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = spacing.gutter),
            )
        }
        ComposeBottomBar(
            canPreview = uiState.meter.meetsMinimum,
            imeVisible = imeVisible,
            onPreview = onPreview,
            onPreviewTooShort = onPreviewTooShort,
            windowInsets = windowInsets,
        )
    }
    if (uiState.discarding) DiscardDraftDialog(onKeepWriting = onKeepWriting, onToss = onToss)
}

/** "1 cast left today", or the day's limit with writing still allowed (spec §3.4, §3.18). */
@Composable
private fun CastLimit(castsLeft: Int?) {
    val spacing = BottlingTheme.spacing
    when (castsLeft) {
        0 -> Banner(
            text = pluralStringResource(R.plurals.compose_cast_limit, MessageLimits.CASTS_PER_DAY, MessageLimits.CASTS_PER_DAY),
            icon = R.drawable.ic_symbol_info,
            modifier = Modifier.padding(horizontal = spacing.gutter, vertical = spacing.xxs),
        )
        1 -> StatusChip(
            text = pluralStringResource(R.plurals.compose_casts_left, 1, 1),
            icon = R.drawable.ic_glyph_bottle,
            style = StatusChipStyle.Glass,
            modifier = Modifier.padding(horizontal = spacing.gutter, vertical = spacing.xxs),
        )
        else -> Unit
    }
}

@NightDawnPreviews
@Composable
private fun ComposeScreenPreview() = BottlingPreview {
    val text = "If you're reading this, drink some water and text someone you miss."
    val meter = DraftMeter.of(text)
    ComposeScreen(
        uiState = ComposeUiState.Writing(
            placeholder = DailyPrompt.forDate(LocalDate.of(2026, 10, 14)),
            meter = meter,
            helper = composeHelper(meter, minimumTapped = false, lang = "en"),
        ),
        editor = TextFieldState(text),
        imeVisible = false,
        onClose = {},
        onPreview = {},
        onPreviewTooShort = {},
        onLimit = {},
        onKeepWriting = {},
        onToss = {},
        snackbarHostState = remember { SnackbarHostState() },
    )
}
