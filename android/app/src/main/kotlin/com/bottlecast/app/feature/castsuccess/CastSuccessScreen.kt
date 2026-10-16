package com.bottlecast.app.feature.castsuccess

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bottlecast.app.R
import com.bottlecast.app.common.components.BOARD_WIDTH
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.Dome
import com.bottlecast.app.common.components.MoonDisc
import com.bottlecast.app.common.components.SceneryStar
import com.bottlecast.app.common.components.bottle.BottleLook
import com.bottlecast.app.common.components.drawStars
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.common.ui.rememberBreathingTide
import com.bottlecast.app.common.ui.rememberHaptics
import com.bottlecast.app.common.ui.rememberIdleMotionEnabled
import com.bottlecast.app.common.ui.rememberReducedMotion
import com.bottlecast.app.feature.castsuccess.components.CastMotion
import com.bottlecast.app.feature.castsuccess.components.CastStage
import com.bottlecast.app.feature.castsuccess.components.CastTimeline
import com.bottlecast.app.feature.castsuccess.components.JourneyRow
import com.bottlecast.app.feature.castsuccess.components.castPose
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The cast and its success (spec §3.5): about 2.4s of the bottle being corked and tossed out to the
 * horizon, which a tap skips, then "Corked and cast", the journey ahead, and Back to shore. Under
 * reduced motion it's a 200ms crossfade to the still scene. Back always leads to the Shore.
 */
@Composable
fun CastSuccessScreen(
    onBackToShoreHandler: () -> Unit,
    viewModel: CastSuccessViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val reducedMotion = rememberReducedMotion()
    val haptics = rememberHaptics()
    val tide = rememberBreathingTide(running = rememberIdleMotionEnabled(reducedMotion))
    val scope = rememberCoroutineScope()
    val timeline = remember {
        Animatable(
            when {
                uiState.played -> CastTimeline.END
                reducedMotion -> CastTimeline.WORDS_START
                else -> 0f
            }
        )
    }

    // one owner: the cast plays here, and a skip snaps it to the end, which interrupts it
    LaunchedEffect(Unit) {
        if (timeline.value >= CastTimeline.END) return@LaunchedEffect
        var corkLanded = false
        var splashed = false
        try {
            val remaining = if (reducedMotion) REDUCED_MOTION_MILLIS else (CastTimeline.END - timeline.value).toInt()
            timeline.animateTo(CastTimeline.END, tween(remaining, easing = LinearEasing)) {
                if (!corkLanded && value >= CastTimeline.CORK_LANDS) {
                    corkLanded = true
                    haptics.thud()
                }
                if (!splashed && value >= CastTimeline.SPLASH) {
                    splashed = true
                    haptics.thud()
                }
            }
        } catch (exception: CancellationException) {
            // skipped, unless the screen itself is going
            if (!currentCoroutineContext().isActive) throw exception
        }
        haptics.confirm()
        viewModel.onPlayed()
    }

    CastSuccessScreen(
        uiState = uiState,
        motion = CastMotion(
            timeline = { timeline.value },
            bob = tide.bob,
            pulse = { ((tide.haloAlpha() - HALO_LOW) / (1f - HALO_LOW)).coerceIn(0f, 1f) },
        ),
        onSkip = { scope.launch { timeline.snapTo(CastTimeline.END) } },
        onBackToShore = onBackToShoreHandler,
    )
}

/** Cast success itself, for previews and screenshot tests, with the motion hoisted. */
@Composable
internal fun CastSuccessScreen(
    uiState: CastSuccessUiState,
    motion: CastMotion,
    onSkip: () -> Unit,
    onBackToShore: () -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    // the words, and the button, come once the bottle is out
    val wordsShown by remember(motion) { derivedStateOf { motion.timeline() >= CastTimeline.WORDS_START } }
    val playing by remember(motion) { derivedStateOf { motion.timeline() < CastTimeline.END } }
    val skipLabel = stringResource(R.string.cast_success_skip)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(
                if (playing) {
                    // a tap anywhere skips to the end (spec §3.5)
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = skipLabel,
                        onClick = onSkip,
                    )
                } else {
                    Modifier
                }
            ),
    ) {
        val screenHeight = maxHeight
        val arcY = screenHeight * CastSuccessDimens.dome.fraction
        Dome(
            edge = CastSuccessDimens.dome,
            // from Preview's 70% down to 60% as the bottle lands (spec §1.5)
            edgeOffset = {
                (screenHeight * (CastSuccessDimens.startDome.fraction - CastSuccessDimens.dome.fraction)) *
                    (1f - castPose(motion.timeline()).settled)
            },
        )
        CastScenery(windowInsets = windowInsets)
        CastStage(
            look = uiState.look,
            motion = motion,
            arcY = arcY,
            description = stringResource(R.string.cast_success_bottle_description),
        )
        if (wordsShown) {
            CastWords(
                uiState = uiState,
                motion = motion,
                arcY = arcY,
                screenHeight = screenHeight,
                onBackToShore = onBackToShore,
                windowInsets = windowInsets,
                modifier = Modifier.graphicsLayer { alpha = castPose(motion.timeline()).words },
            )
        }
    }
}

/**
 * The words around the settled bottle: the title in the dome (y≈250), the journey just under the arc
 * (y≈540) with its caption, and Back to shore at the foot. With large text they can't keep to those
 * places, so each keeps clear of the last and the page scrolls (spec §4).
 */
@Composable
private fun CastWords(
    uiState: CastSuccessUiState,
    motion: CastMotion,
    arcY: Dp,
    screenHeight: Dp,
    onBackToShore: () -> Unit,
    windowInsets: WindowInsets,
    modifier: Modifier = Modifier,
) {
    val spacing = BottlingTheme.spacing
    val insets = windowInsets.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    Layout(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = insets.calculateStartPadding(layoutDirection) + spacing.gutter,
                end = insets.calculateEndPadding(layoutDirection) + spacing.gutter,
            ),
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = stringResource(if (uiState.queued) R.string.cast_success_queued_title else R.string.cast_success_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics {
                        heading()
                        liveRegion = LiveRegionMode.Polite
                    },
                )
                Text(
                    text = stringResource(if (uiState.queued) R.string.cast_success_queued_body else R.string.cast_success_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = spacing.m),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(spacing.s)) {
                JourneyRow(queued = uiState.queued, sealable = uiState.sealable, pulse = motion.pulse, modifier = Modifier.fillMaxWidth())
                Text(
                    text = stringResource(R.string.cast_success_caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            BottlingButton(text = R.string.cast_success_back, onTapHandler = onBackToShore)
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minHeight = 0)
        val (title, journey, button) = measurables.map { it.measure(loose) }
        val gap = spacing.l.roundToPx()
        val arc = arcY.roundToPx()
        val top = insets.calculateTopPadding().roundToPx() + spacing.m.roundToPx()
        // inside the scroll the height is unbounded, so the screen's own says where the foot is
        val screenBottom = screenHeight.roundToPx()
        val bottomInset = insets.calculateBottomPadding().roundToPx() + spacing.l.roundToPx()

        val titleY = maxOf(top, arc - CastSuccessDimens.titleAboveArc.roundToPx())
        val journeyY = maxOf(arc + CastSuccessDimens.journeyBelowArc.roundToPx(), titleY + title.height + gap)
        val buttonY = maxOf(journeyY + journey.height + gap, screenBottom - bottomInset - button.height)
        val height = maxOf(buttonY + button.height + bottomInset, screenBottom)
        layout(constraints.maxWidth, height) {
            title.placeRelative((constraints.maxWidth - title.width) / 2, titleY)
            journey.placeRelative((constraints.maxWidth - journey.width) / 2, journeyY)
            button.placeRelative((constraints.maxWidth - button.width) / 2, buttonY)
        }
    }
}

/** The dome's stars and the moon, top-end (the Cast-Success board). Decorative. */
@Composable
private fun CastScenery(windowInsets: WindowInsets) {
    val colors = BottlingTheme.colors
    val insets = windowInsets.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clearAndSetSemantics {},
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) { drawStars(Stars, colors) }
        MoonDisc(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = insets.calculateTopPadding() + CastSuccessDimens.moonInset,
                    end = insets.calculateEndPadding(layoutDirection) + CastSuccessDimens.moonInset,
                ),
        )
    }
}

private val Stars = listOf(
    SceneryStar(49f / BOARD_WIDTH, 139.dp, 0.45f),
    SceneryStar(113f / BOARD_WIDTH, 93.dp, 0.30f),
    SceneryStar(345f / BOARD_WIDTH, 237.dp, 0.35f),
    SceneryStar(31f / BOARD_WIDTH, 301.dp, 0.30f),
)

private const val REDUCED_MOTION_MILLIS = 200
private const val HALO_LOW = 0.9f

@NightDawnPreviews
@Composable
private fun CastSuccessScreenPreview() = BottlingPreview {
    CastSuccessScreen(
        uiState = CastSuccessUiState(look = BottleLook.Default, queued = false, sealable = true, played = true),
        motion = CastMotion.Settled,
        onSkip = {},
        onBackToShore = {},
    )
}
