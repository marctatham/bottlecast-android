package com.bottlecast.app.feature.keepsake

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.bottlecast.app.R
import com.bottlecast.app.common.analytics.AnalyticsEvent
import com.bottlecast.app.common.analytics.LocalTracker
import com.bottlecast.app.common.chest.domain.KeepResult
import com.bottlecast.app.common.chest.domain.PullResult
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.BottlingButtonSize
import com.bottlecast.app.common.components.BottlingButtonType
import com.bottlecast.app.common.components.BottlingDialog
import com.bottlecast.app.common.components.BottlingSnackbarHost
import com.bottlecast.app.common.components.BottlingTextButton
import com.bottlecast.app.common.components.DomeScaffold
import com.bottlecast.app.common.components.EmptyState
import com.bottlecast.app.common.components.ImmersiveTopBar
import com.bottlecast.app.common.components.MessageCard
import com.bottlecast.app.common.components.OverlineText
import com.bottlecast.app.common.components.ParchmentCard
import com.bottlecast.app.common.components.bleed
import com.bottlecast.app.common.components.bottle.BottleSilhouette
import com.bottlecast.app.common.components.bottle.BottleTint
import com.bottlecast.app.common.components.bottle.bottleLookFor
import com.bottlecast.app.common.components.showTimed
import com.bottlecast.app.common.sea.domain.ReportReason
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.common.ui.UNDO_WINDOW_MILLIS
import com.bottlecast.app.common.ui.rememberTimedControlMillis
import com.bottlecast.app.feature.chest.components.StandingChip
import com.bottlecast.app.feature.chest.domain.BottleStanding
import com.bottlecast.app.feature.keepsake.components.JourneyTimeline
import com.bottlecast.app.feature.reveal.Byline
import com.bottlecast.app.feature.reveal.RevealedBottle
import com.bottlecast.app.feature.reveal.components.ReportDialog
import com.bottlecast.app.feature.reveal.components.keepMessage
import com.bottlecast.app.feature.reveal.components.shareBottle
import com.bottlecast.app.feature.reveal.components.revealByline
import com.bottlecast.app.feature.reveal.domain.CastAgo
import com.bottlecast.app.feature.signin.SignInReason
import com.bottlecast.app.feature.signin.SignInSheet
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Keepsake detail (spec §3.11): a bottle from the chest on parchment, its status and traits, and its
 * journey. An unkept find has Keep at the foot, and Throw back and Report in the overflow; a drifting
 * cast of the user's own can be pulled from the sea.
 *
 * @param onBackHandler back to the Chest.
 * @param onLeaveHandler thrown back, reported or pulled: back to the Chest, which offers any Undo.
 */
@Composable
fun KeepsakeScreen(
    onBackHandler: () -> Unit,
    onLeaveHandler: () -> Unit,
    viewModel: KeepsakeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val tracker = LocalTracker.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var signingIn by rememberSaveable { mutableStateOf(false) }
    val undoWindow = rememberTimedControlMillis(UNDO_WINDOW_MILLIS, hasAction = true)
    val snackbarMillis = rememberTimedControlMillis(UNDO_WINDOW_MILLIS, hasAction = false)
    val leave = dropUnlessResumed { onLeaveHandler() }
    val keepMessages = KeepResult.entries.associateWith { stringResource(keepMessage(it)) }
    val pullMessages = PullResult.entries.associateWith { stringResource(pullMessage(it)) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                KeepsakeEffect.NeedsSignIn -> signingIn = true
                KeepsakeEffect.Left -> leave()
                is KeepsakeEffect.KeepFinished -> {
                    if (effect.result == KeepResult.Kept) {
                        tracker.trackEvent(AnalyticsEvent.BottleKept, mapOf(AnalyticsEvent.PARAM_SOURCE to KEEP_SOURCE))
                    }
                    val message = keepMessages.getValue(effect.result)
                    scope.launch { snackbarHostState.showTimed(message, actionLabel = null, millis = snackbarMillis) }
                }
                is KeepsakeEffect.PullFinished -> if (effect.result == PullResult.Pulled) {
                    tracker.trackEvent(AnalyticsEvent.BottlePulled)
                    leave()
                } else {
                    val message = pullMessages.getValue(effect.result)
                    scope.launch { snackbarHostState.showTimed(message, actionLabel = null, millis = snackbarMillis) }
                }
            }
        }
    }

    KeepsakeScreen(
        uiState = uiState,
        today = LocalDate.now(),
        onBackHandler = onBackHandler,
        onShareHandler = dropUnlessResumed {
            (uiState as? KeepsakeUiState.Showing)?.detail?.text?.let { shareBottle(context, it) }
            tracker.trackEvent(AnalyticsEvent.BottleShared)
        },
        onKeepHandler = { viewModel.onKeep() },
        onThrowBackHandler = { viewModel.onThrowBack(undoWindow) },
        onReportHandler = { reason, blockWriter -> viewModel.onReport(reason, blockWriter, undoWindow) },
        onPullHandler = viewModel::onPull,
        snackbarHostState = snackbarHostState,
    )

    if (signingIn) {
        SignInSheet(
            reason = SignInReason.Keep,
            onDismissRequest = { signingIn = false },
            onSignedIn = {
                signingIn = false
                viewModel.onKeep(signedIn = true)
            },
        )
    }
}

// why a pull didn't go through, and that the bottle's safe
private fun pullMessage(result: PullResult): Int = when (result) {
    PullResult.Offline -> R.string.keepsake_pull_offline
    PullResult.NotDrifting -> R.string.keepsake_pull_not_drifting
    PullResult.Pulled, PullResult.Failed -> R.string.keepsake_pull_failed
}

private const val KEEP_SOURCE = "keepsake"

/** The stateless Keepsake detail, for previews and screenshot tests. */
@Composable
internal fun KeepsakeScreen(
    uiState: KeepsakeUiState,
    today: LocalDate,
    onBackHandler: () -> Unit,
    onShareHandler: () -> Unit,
    onKeepHandler: () -> Unit,
    onThrowBackHandler: () -> Unit,
    onReportHandler: (ReportReason?, Boolean) -> Unit,
    onPullHandler: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    windowInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    contentPadding: PaddingValues = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
    menuOpen: Boolean = false,
    pulling: Boolean = false,
) {
    var reporting by rememberSaveable { mutableStateOf(false) }
    var confirmingPull by rememberSaveable { mutableStateOf(pulling) }
    val detail = (uiState as? KeepsakeUiState.Showing)?.detail
    val spacing = BottlingTheme.spacing

    Box(modifier = modifier) {
        DomeScaffold(
            edge = KeepsakeDimens.dome,
            contentPadding = contentPadding,
            windowInsets = windowInsets,
        ) { _ ->
            ImmersiveTopBar(onBackHandler = onBackHandler, modifier = Modifier.bleed(KeepsakeDimens.topBarBleed)) {
                if (detail?.shareable == true) {
                    IconButton(onClick = onShareHandler) {
                        Icon(
                            painter = painterResource(R.drawable.ic_symbol_share),
                            contentDescription = stringResource(R.string.keepsake_share),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                if (detail != null && detail.actions.isNotEmpty()) {
                    KeepsakeMenu(
                        actions = detail.actions,
                        initiallyOpen = menuOpen,
                        onThrowBack = onThrowBackHandler,
                        onReport = { reporting = true },
                        onPull = { confirmingPull = true },
                    )
                }
            }
            when (uiState) {
                KeepsakeUiState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.xxl),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = BottlingTheme.colors.glass)
                }
                KeepsakeUiState.Gone -> EmptyState(
                    title = stringResource(R.string.keepsake_gone_title),
                    body = stringResource(R.string.keepsake_gone_body),
                    actionLabel = stringResource(R.string.keepsake_back_to_chest),
                    onAction = onBackHandler,
                    announce = true,
                    modifier = Modifier.padding(top = spacing.xxl),
                )
                is KeepsakeUiState.Showing -> KeepsakeBody(detail = uiState.detail, today = today, onKeepHandler = onKeepHandler)
            }
        }
        BottlingSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(contentPadding)
                .padding(horizontal = spacing.gutter),
        )
    }

    if (reporting && detail != null) {
        ReportDialog(
            onDismissRequest = { reporting = false },
            onReportHandler = { reason, blockWriter ->
                reporting = false
                onReportHandler(reason, blockWriter)
            },
        )
    }
    if (confirmingPull) {
        PullDialog(
            onDismissRequest = { confirmingPull = false },
            onPull = {
                confirmingPull = false
                onPullHandler()
            },
        )
    }
}

@Composable
private fun ColumnScope.KeepsakeBody(detail: KeepsakeDetail, today: LocalDate, onKeepHandler: () -> Unit) {
    val spacing = BottlingTheme.spacing
    val colorScheme = MaterialTheme.colorScheme
    Spacer(modifier = Modifier.height(spacing.xs))
    if (detail.text != null) {
        MessageCard(
            text = detail.text,
            byline = revealByline(RevealedBottle(detail.messageId, detail.text, detail.byline, detail.castAgo)),
            maxHeight = null,
        )
    } else {
        // a removed bottle's words are hidden (spec §3.11)
        ParchmentCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.keepsake_removed),
                style = MaterialTheme.typography.bodyLarge,
                color = BottlingTheme.colors.ink,
            )
        }
    }
    Spacer(modifier = Modifier.height(KeepsakeDimens.sectionGap))
    // the status row: the chip, and a line where the spec gives one
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        StandingChip(detail.standing)
        detail.notice?.let { notice ->
            Text(
                text = stringResource(notice.text()),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(modifier = Modifier.height(spacing.xs))
    if (detail.text != null) {
        val look = bottleLookFor(detail.messageId)
        Text(
            text = stringResource(R.string.bottle_traits, stringResource(look.silhouette.label()), stringResource(look.tint.label())),
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
        )
    }
    Spacer(modifier = Modifier.height(KeepsakeDimens.sectionGap))
    OverlineText(
        text = stringResource(R.string.keepsake_journey),
        color = colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(modifier = Modifier.height(spacing.s))
    JourneyTimeline(stops = detail.journey, today = today)
    detail.keep?.let { keep ->
        Spacer(modifier = Modifier.height(spacing.l))
        // Keep, at the foot while the page is short enough
        Spacer(modifier = Modifier.weight(1f))
        BottlingButton(
            text = pluralStringResource(R.plurals.keepsake_keep, keep.daysLeft, keep.daysLeft),
            leadingIcon = R.drawable.ic_symbol_inventory_2,
            enabled = keep.online,
            isLoading = keep.keeping,
            onTapHandler = onKeepHandler,
        )
        if (!keep.online) {
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = stringResource(R.string.keep_offline),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    Spacer(modifier = Modifier.height(spacing.l))
}

/** The overflow: Throw back and Report for an unkept find, Pull from the sea for a drifting cast (spec §3.11). */
@Composable
private fun KeepsakeMenu(
    actions: Set<KeepsakeAction>,
    initiallyOpen: Boolean,
    onThrowBack: () -> Unit,
    onReport: () -> Unit,
    onPull: () -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyOpen) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_symbol_more_vert),
                contentDescription = stringResource(R.string.keepsake_more),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = BottlingTheme.colors.surfaceHigh,
            shape = BottlingTheme.shapes.field,
        ) {
            listOf(KeepsakeAction.ThrowBack, KeepsakeAction.Report, KeepsakeAction.Pull).filter { it in actions }.forEach { action ->
                DropdownMenuItem(
                    text = { Text(text = stringResource(action.label()), style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        expanded = false
                        when (action) {
                            KeepsakeAction.ThrowBack -> onThrowBack()
                            KeepsakeAction.Report -> onReport()
                            KeepsakeAction.Pull -> onPull()
                        }
                    },
                )
            }
        }
    }
}

/** "Pull this bottle from the sea? No one will find it." [Cancel] [Pull it] (spec §3.11). */
@Composable
internal fun PullDialog(onDismissRequest: () -> Unit, onPull: () -> Unit) {
    val spacing = BottlingTheme.spacing
    BottlingDialog(onDismissRequest = onDismissRequest) {
        Text(
            text = stringResource(R.string.keepsake_pull_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(spacing.m))
        Text(
            text = stringResource(R.string.keepsake_pull_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(spacing.l))
        Row(
            modifier = Modifier.align(Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottlingTextButton(text = R.string.keepsake_pull_cancel, onTapHandler = onDismissRequest)
            BottlingButton(
                text = R.string.keepsake_pull_confirm,
                buttonType = BottlingButtonType.DESTRUCTIVE,
                size = BottlingButtonSize.MEDIUM,
                onTapHandler = onPull,
            )
        }
    }
}

private fun KeepsakeNotice.text(): Int = when (this) {
    KeepsakeNotice.UnderReview -> R.string.keepsake_under_review
    KeepsakeNotice.Removed -> R.string.keepsake_removed
    KeepsakeNotice.DriftsTomorrow -> R.string.keepsake_drifts_tomorrow
    KeepsakeNotice.DriftsToday -> R.string.keepsake_drifts_today
}

private fun KeepsakeAction.label(): Int = when (this) {
    KeepsakeAction.ThrowBack -> R.string.keepsake_throw_back
    KeepsakeAction.Report -> R.string.keepsake_report
    KeepsakeAction.Pull -> R.string.keepsake_pull
}

private fun BottleSilhouette.label(): Int = when (this) {
    BottleSilhouette.Classic -> R.string.bottle_silhouette_classic
    BottleSilhouette.Flask -> R.string.bottle_silhouette_flask
    BottleSilhouette.Gourd -> R.string.bottle_silhouette_gourd
}

private fun BottleTint.label(): Int = when (this) {
    BottleTint.SeaGlass -> R.string.bottle_tint_sea_glass
    BottleTint.Amber -> R.string.bottle_tint_amber
    BottleTint.Moonglass -> R.string.bottle_tint_moonglass
}

@NightDawnPreviews
@Composable
private fun KeepsakeScreenPreview() = BottlingPreview {
    val today = LocalDate.of(2026, 10, 12)
    KeepsakeScreen(
        uiState = KeepsakeUiState.Showing(
            KeepsakeDetail(
                messageId = "preview",
                text = "Hey, that's pretty cool",
                byline = Byline.Stranger,
                castAgo = CastAgo.Days(3),
                lang = "en",
                standing = BottleStanding.NotKept(5),
                notice = null,
                journey = listOf(
                    JourneyStop(JourneyStep.Cast, today.minusDays(4)),
                    JourneyStop(JourneyStep.Found, today.minusDays(2)),
                    JourneyStop(JourneyStep.Kept, null),
                ),
                ownCast = false,
                keep = KeepsakeKeep(daysLeft = 5, online = true, keeping = false),
                actions = setOf(KeepsakeAction.ThrowBack, KeepsakeAction.Report),
                shareable = true,
            ),
        ),
        today = today,
        onBackHandler = {},
        onShareHandler = {},
        onKeepHandler = {},
        onThrowBackHandler = {},
        onReportHandler = { _, _ -> },
        onPullHandler = {},
    )
}
