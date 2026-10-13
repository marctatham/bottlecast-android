package com.bottlecast.app.feature.reveal

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.bottlecast.app.R
import com.bottlecast.app.common.analytics.AnalyticsEvent
import com.bottlecast.app.common.analytics.LocalTracker
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.BottlingButtonSize
import com.bottlecast.app.common.components.BottlingButtonType
import com.bottlecast.app.common.components.DomeEdge
import com.bottlecast.app.common.components.DomeScaffold
import com.bottlecast.app.common.components.EmptyState
import com.bottlecast.app.common.components.ImmersiveTopBar
import com.bottlecast.app.common.components.OverlineText
import com.bottlecast.app.common.components.bleed
import com.bottlecast.app.common.components.bottle.bottleLookFor
import com.bottlecast.app.common.sea.domain.ReportReason
import com.bottlecast.app.common.ui.BottlingMotion
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.common.ui.UNDO_WINDOW_MILLIS
import com.bottlecast.app.common.ui.isLargeText
import com.bottlecast.app.common.ui.rememberReducedMotion
import com.bottlecast.app.common.ui.rememberTimedControlMillis
import com.bottlecast.app.feature.reveal.components.DriftingBottle
import com.bottlecast.app.feature.reveal.components.MessageCard
import com.bottlecast.app.feature.reveal.components.ReportDialog
import com.bottlecast.app.feature.reveal.components.ReportedBar
import com.bottlecast.app.feature.reveal.components.ReportedThanks
import com.bottlecast.app.feature.reveal.components.RevealScenery
import com.bottlecast.app.feature.reveal.domain.CastAgo
import kotlinx.coroutines.delay

/**
 * Reveal (spec §3.7): a found bottle's message on parchment, with Share and Throw back, and a flag
 * to report it. Keep, its helper and its chip arrive with the Chest in slice 4.
 *
 * @param onBackHandler back to the Shore: the find stays found.
 * @param onLeaveHandler the bottle was thrown back, or reported and done with: back to the Shore.
 */
@Composable
fun RevealScreen(
    onBackHandler: () -> Unit,
    onLeaveHandler: () -> Unit,
    viewModel: RevealViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val reducedMotion = rememberReducedMotion()
    val undoWindow = rememberTimedControlMillis(UNDO_WINDOW_MILLIS, hasAction = true)
    val context = LocalContext.current
    val tracker = LocalTracker.current

    val showing = uiState as? RevealUiState.Showing
    val fresh = showing?.fresh == true && !reducedMotion
    // a fresh find's moonlight sweeps once, after the screen has settled (spec §1.5)
    var sheenPlayed by rememberSaveable { mutableStateOf(!fresh) }
    val sheen = remember { Animatable(0f) }
    LaunchedEffect(fresh) {
        if (fresh && !sheenPlayed) {
            delay(BottlingMotion.SCREEN_MILLIS.toLong())
            sheen.animateTo(1f, tween(SHEEN_MILLIS, easing = LinearEasing))
            sheenPlayed = true
        }
    }
    // a fresh find's dome settles from Discover's expanded edge down to Reveal's compact one
    val domeSettle = remember { Animatable(if (fresh) 1f else 0f) }
    LaunchedEffect(Unit) {
        domeSettle.animateTo(0f, tween(BottlingMotion.SCREEN_MILLIS, easing = BottlingMotion.EmphasizedDecelerate))
    }
    val screenHeight = screenHeight()

    RevealScreen(
        uiState = uiState,
        sheen = { if (!sheenPlayed && sheen.isRunning) sheen.value else null },
        domeOffset = { screenHeight * ((DomeEdge.Expanded.fraction - RevealDimens.dome.fraction) * domeSettle.value) },
        onBackHandler = onBackHandler,
        onReportHandler = { reason, blockWriter ->
            viewModel.onReport(reason, blockWriter, undoWindow)
            tracker.trackEvent(
                AnalyticsEvent.BottleReported,
                mapOf(AnalyticsEvent.PARAM_BLOCKED to blockWriter.toString()),
            )
        },
        onUndoReportHandler = viewModel::onUndoReport,
        onShareHandler = dropUnlessResumed {
            showing?.let { shareBottle(context, it.bottle.text) }
            tracker.trackEvent(AnalyticsEvent.BottleShared)
        },
        onThrowBackHandler = dropUnlessResumed {
            viewModel.onThrowBack(undoWindow)
            tracker.trackEvent(AnalyticsEvent.BottleThrownBack)
            onLeaveHandler()
        },
        onBackToShoreHandler = dropUnlessResumed { onLeaveHandler() },
        onRetryHandler = viewModel::onRetry,
    )
}

private fun shareBottle(context: Context, message: String) {
    val text = context.getString(R.string.reveal_share_text, message, context.getString(R.string.play_store_url))
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.reveal_share_chooser)))
}

/**
 * The stateless Reveal screen, for previews and screenshot tests.
 *
 * @param sheen how far the moonlight's sweep across the card has gone, or null.
 * @param domeOffset how far the dome's edge still sits below its resting place, as it settles.
 */
@Composable
internal fun RevealScreen(
    uiState: RevealUiState,
    sheen: () -> Float?,
    domeOffset: () -> Dp,
    onBackHandler: () -> Unit,
    onReportHandler: (ReportReason?, Boolean) -> Unit,
    onUndoReportHandler: () -> Unit,
    onShareHandler: () -> Unit,
    onThrowBackHandler: () -> Unit,
    onBackToShoreHandler: () -> Unit,
    onRetryHandler: () -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    contentPadding: PaddingValues = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
    showReportDialog: Boolean = false,
) {
    var reporting by rememberSaveable { mutableStateOf(showReportDialog) }
    val showing = uiState as? RevealUiState.Showing
    val reported = showing != null && showing.report != RevealReport.None
    val spacing = BottlingTheme.spacing
    val largeText = isLargeText()

    DomeScaffold(
        edge = RevealDimens.dome,
        contentPadding = contentPadding,
        modifier = modifier,
        windowInsets = windowInsets,
        edgeOffset = domeOffset,
        horizontalAlignment = Alignment.CenterHorizontally,
        scenery = { insets -> RevealScenery(insets = insets) },
    ) { toArc ->
        ImmersiveTopBar(onBackHandler = onBackHandler, modifier = Modifier.bleed(RevealDimens.topBarBleed)) {
            if (showing != null) {
                ReportFlag(reported = reported, onClick = { reporting = true })
            }
        }
        when (uiState) {
            RevealUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xxl),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = BottlingTheme.colors.glass)
            }

            RevealUiState.Failed -> EmptyState(
                title = stringResource(R.string.adrift_title),
                body = stringResource(R.string.adrift_body),
                actionLabel = stringResource(R.string.try_again),
                actionIcon = R.drawable.ic_symbol_refresh,
                onAction = onRetryHandler,
                // below the arc: the compact dome is too short to hold it
                modifier = Modifier.padding(top = (toArc - BottlingTheme.sizes.topBarHeight).coerceAtLeast(0.dp) + spacing.xl),
            )

            is RevealUiState.Showing -> {
                OverlineText(
                    text = stringResource(R.string.reveal_found_today),
                    modifier = Modifier.padding(top = spacing.xxs, bottom = spacing.xs),
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    // at large text the card grows with the page instead of scrolling inside (spec §4)
                    val cap = if (largeText) null else screenHeight() * RevealDimens.CARD_MAX_SHARE
                    AnimatedContent(
                        targetState = uiState.report,
                        transitionSpec = {
                            fadeIn(tween(BottlingMotion.COMPONENT_MILLIS)) togetherWith fadeOut(tween(BottlingMotion.COMPONENT_MILLIS))
                        },
                        contentKey = { it::class },
                        label = "report",
                    ) { report ->
                        when (report) {
                            RevealReport.None -> MessageCard(bottle = uiState.bottle, maxHeight = cap, sheen = sheen)
                            RevealReport.Pending -> ReportedBar(onUndoHandler = onUndoReportHandler)
                            is RevealReport.Reported -> ReportedThanks(
                                blocked = report.blocked,
                                queuedOffline = report.queuedOffline,
                                onBackToShoreHandler = onBackToShoreHandler,
                            )
                        }
                    }
                }
                if (!reported) {
                    Spacer(modifier = Modifier.height(RevealDimens.actionsGap))
                    // side by side, or stacked when large text leaves no room for both (spec §4)
                    if (largeText) {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                            ShareButton(onShareHandler, Modifier.fillMaxWidth())
                            ThrowBackButton(onThrowBackHandler, Modifier.fillMaxWidth())
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                            ShareButton(onShareHandler, Modifier.weight(1f))
                            ThrowBackButton(onThrowBackHandler, Modifier.weight(1f))
                        }
                    }
                    if (!largeText) {
                        DriftingBottle(
                            look = bottleLookFor(uiState.bottle.id),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = spacing.xxl),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(spacing.l))
            }
        }
    }

    if (reporting && showing != null) {
        ReportDialog(
            onDismissRequest = { reporting = false },
            onReportHandler = { reason, blockWriter ->
                reporting = false
                onReportHandler(reason, blockWriter)
            },
        )
    }
}

@Composable
private fun ShareButton(onShareHandler: () -> Unit, modifier: Modifier) {
    BottlingButton(
        text = R.string.reveal_share,
        buttonType = BottlingButtonType.TONAL,
        size = BottlingButtonSize.MEDIUM,
        leadingIcon = R.drawable.ic_symbol_share,
        onTapHandler = onShareHandler,
        modifier = modifier,
    )
}

@Composable
private fun ThrowBackButton(onThrowBackHandler: () -> Unit, modifier: Modifier) {
    BottlingButton(
        text = R.string.reveal_throw_back,
        buttonType = BottlingButtonType.TONAL,
        size = BottlingButtonSize.MEDIUM,
        leadingIcon = R.drawable.ic_symbol_waves,
        onTapHandler = onThrowBackHandler,
        modifier = modifier,
    )
}

/** The outlined flag: a 40dp circle in its 48dp target, filled once reported (spec §3.7, §3.19). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportFlag(reported: Boolean, onClick: () -> Unit) {
    val label = stringResource(if (reported) R.string.reveal_reported_description else R.string.reveal_report_description)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { PlainTooltip { Text(stringResource(R.string.reveal_report)) } },
        state = rememberTooltipState(),
    ) {
        OutlinedIconButton(
            onClick = onClick,
            enabled = !reported,
            border = BorderStroke(BottlingTheme.sizes.hairline, MaterialTheme.colorScheme.outline),
            colors = IconButtonDefaults.outlinedIconButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Icon(
                painter = painterResource(if (reported) R.drawable.ic_symbol_flag_filled else R.drawable.ic_symbol_flag),
                contentDescription = label,
                modifier = Modifier.size(RevealDimens.flagIcon),
            )
        }
    }
}

@Composable
private fun screenHeight(): Dp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }

private const val SHEEN_MILLIS = 900

@NightDawnPreviews
@Composable
private fun RevealScreenPreview() = BottlingPreview {
    RevealScreen(
        uiState = RevealUiState.Showing(
            bottle = RevealedBottle(
                id = "preview",
                text = "Hoy el mar me devolvió algo que creía perdido: la calma.",
                byline = Byline.Stranger,
                castAgo = CastAgo.Days(3),
            ),
            report = RevealReport.None,
            fresh = false,
        ),
        sheen = { null },
        domeOffset = { 0.dp },
        onBackHandler = {},
        onReportHandler = { _, _ -> },
        onUndoReportHandler = {},
        onShareHandler = {},
        onThrowBackHandler = {},
        onBackToShoreHandler = {},
        onRetryHandler = {},
    )
}
