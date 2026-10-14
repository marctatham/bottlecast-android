package com.bottlecast.app.feature.discover

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
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
import com.bottlecast.app.common.analytics.AnalyticsEvent
import com.bottlecast.app.common.analytics.LocalTracker
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.BottlingButtonType
import com.bottlecast.app.common.components.DomeEdge
import com.bottlecast.app.common.components.DomeScaffold
import com.bottlecast.app.common.components.EmptyState
import com.bottlecast.app.common.components.ImmersiveTopBar
import com.bottlecast.app.common.components.bleed
import com.bottlecast.app.common.components.bottle.BottleLook
import com.bottlecast.app.common.components.illustrations.FogIllustration
import com.bottlecast.app.common.components.illustrations.SeaCalmIllustration
import com.bottlecast.app.common.components.illustrations.SlippedBackIllustration
import com.bottlecast.app.common.ui.BottlingMotion
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.common.ui.rememberBreathingTide
import com.bottlecast.app.common.ui.rememberHaptics
import com.bottlecast.app.common.ui.rememberIdleMotionEnabled
import com.bottlecast.app.common.ui.rememberReducedMotion
import com.bottlecast.app.feature.discover.components.DiscoverMotion
import com.bottlecast.app.feature.discover.components.DiscoverScenery
import com.bottlecast.app.feature.discover.components.ShakeEffect
import com.bottlecast.app.feature.discover.components.UncorkBottle
import com.bottlecast.app.feature.discover.components.UncorkHelpSheet
import com.bottlecast.app.feature.discover.components.rememberHasAccelerometer
import com.bottlecast.app.feature.discover.domain.UncorkEffect
import com.bottlecast.app.feature.discover.domain.UncorkMethod
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Discover (spec §3.6): shake, tap or hold to uncork today's bottle. The find is recorded at the pop,
 * then Reveal opens; backing out before it leaves the day unused.
 *
 * @param onAlreadyFoundHandler today already has a find, so the Shore shows it instead.
 * @param onCastHandler the sea is calm, and the user would rather cast a bottle.
 */
@Composable
fun DiscoverScreen(
    onBackHandler: () -> Unit,
    onRevealHandler: (messageId: String) -> Unit,
    onAlreadyFoundHandler: () -> Unit,
    onCastHandler: () -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val ringProgress by viewModel.ringProgress.collectAsStateWithLifecycle()
    val reducedMotion = rememberReducedMotion()
    val idleMotion = rememberIdleMotionEnabled(reducedMotion)
    val hasAccelerometer = rememberHasAccelerometer()
    val haptics = rememberHaptics()
    val tracker = LocalTracker.current
    val scope = rememberCoroutineScope()
    var showHelp by rememberSaveable { mutableStateOf(false) }

    val ready = uiState as? DiscoverUiState.Ready
    val pop = ready?.pop
    val popped = pop != null && pop != PopPhase.NotPopped
    val currentReducedMotion by rememberUpdatedState(reducedMotion)

    // A rotation after the pop starts from where the pop leaves things, rather than replaying it.
    val wobble = remember { Animatable(0f) }
    val corkFlight = remember { Animatable(if (popped) 1f else 0f) }
    val scrollOut = remember { Animatable(if (popped) HALF_OUT else 0f) }
    val flash = remember { Animatable(0f) }
    // the scroll's slide half out, which the reveal lets finish rather than cut short
    var halfOut by remember { mutableStateOf<Job?>(null) }
    val tide = rememberBreathingTide(running = idleMotion && !popped)
    val shimmer = rememberShimmer(running = pop == PopPhase.Late && !reducedMotion)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is UncorkEffect.Step -> {
                    haptics.shakeStep(effect.step)
                    if (!currentReducedMotion) {
                        scope.launch {
                            // each shake swings the other way (spec §1.5)
                            wobble.snapTo(if (effect.step % 2 == 1) DiscoverDimens.WOBBLE_DEGREES else -DiscoverDimens.WOBBLE_DEGREES)
                            wobble.animateTo(0f, spring(dampingRatio = WOBBLE_DAMPING, stiffness = WOBBLE_STIFFNESS))
                        }
                    }
                }

                is UncorkEffect.HoldTick -> haptics.holdTick()
                is UncorkEffect.Pop -> {
                    haptics.corkPop(effect.afterStep)
                    tracker.trackEvent(
                        AnalyticsEvent.BottleUncorked,
                        mapOf(AnalyticsEvent.PARAM_METHOD to effect.method.value),
                    )
                    if (currentReducedMotion) {
                        corkFlight.snapTo(1f)
                        scrollOut.snapTo(HALF_OUT)
                    } else {
                        // the cork flies, moonlight flashes, and the scroll slides half out (spec §1.5)
                        scope.launch { corkFlight.animateTo(1f, tween(CORK_FLIGHT_MILLIS, easing = BottlingMotion.EmphasizedDecelerate)) }
                        scope.launch {
                            flash.animateTo(0f, keyframes {
                                durationMillis = FLASH_MILLIS
                                FLASH_PEAK at FLASH_MILLIS / 2
                            })
                        }
                        halfOut = scope.launch {
                            delay(SCROLL_DELAY_MILLIS)
                            scrollOut.animateTo(HALF_OUT, tween(BottlingMotion.SCREEN_MILLIS, easing = BottlingMotion.Emphasized))
                        }
                    }
                }
            }
        }
    }

    // once the find is in, the scroll comes the rest of the way out, and Reveal opens. A quick find
    // waits for the slide half out, which would otherwise take the scroll back and cancel this.
    LaunchedEffect(pop) {
        if (pop is PopPhase.Revealed) {
            halfOut?.join()
            if (!currentReducedMotion) {
                scrollOut.animateTo(1f, tween(BottlingMotion.COMPONENT_MILLIS, easing = BottlingMotion.EmphasizedAccelerate))
            }
            onRevealHandler(pop.messageId)
        }
    }
    LaunchedEffect(uiState) {
        if (uiState == DiscoverUiState.AlreadyFound) onAlreadyFoundHandler()
    }
    // while a hold fills or drains the ring, it moves with every frame
    LaunchedEffect(ready?.moving) {
        if (ready?.moving == true) {
            while (true) withFrameMillis { viewModel.onFrame() }
        }
    }

    ShakeEffect(enabled = ready != null && !popped && !showHelp, onShake = viewModel::onShake)

    DiscoverScreen(
        uiState = uiState,
        ringProgress = { ringProgress },
        motion = DiscoverMotion(
            wobble = { wobble.value },
            bob = tide.bob,
            corkFlight = { corkFlight.value },
            scrollOut = { scrollOut.value },
            flash = { flash.value },
            shimmer = shimmer,
            glintPhase = tide.glintPhase,
            haloAlpha = tide.haloAlpha,
            edgeOffset = tide.edgeOffset,
        ),
        hasAccelerometer = hasAccelerometer,
        reducedMotion = reducedMotion,
        waveSpeed = if (idleMotion) WavyProgressIndicatorDefaults.CircularWavelength else 0.dp,
        onBackHandler = onBackHandler,
        onHelpHandler = {
            showHelp = true
            tracker.trackEvent(AnalyticsEvent.UncorkHelpOpened)
        },
        onBottlePressed = viewModel::onBottlePressed,
        onBottleReleased = viewModel::onBottleReleased,
        onBottleCancelled = viewModel::onBottleCancelled,
        onUncorkHandler = viewModel::onUncork,
        onTryAgainHandler = viewModel::onTryAgain,
        onCastHandler = onCastHandler,
    )

    if (showHelp) UncorkHelpSheet(onDismissRequest = { showHelp = false })
}

/** The sheen running over the scroll while the message is late (spec §3.6), or null. */
@Composable
private fun rememberShimmer(running: Boolean): () -> Float? {
    if (!running) return { null }
    val transition = rememberInfiniteTransition(label = "unrolling")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SHIMMER_MILLIS, easing = LinearEasing)),
        label = "shimmer",
    )
    return { phase }
}

/**
 * The stateless Discover screen, for previews and screenshot tests, which pass [motion] still.
 *
 * @param waveSpeed how fast the cork ring's wave runs: 0 holds it still.
 */
@Composable
internal fun DiscoverScreen(
    uiState: DiscoverUiState,
    ringProgress: () -> Float,
    motion: DiscoverMotion,
    hasAccelerometer: Boolean,
    reducedMotion: Boolean,
    onBackHandler: () -> Unit,
    onHelpHandler: () -> Unit,
    onBottlePressed: () -> Unit,
    onBottleReleased: () -> Unit,
    onBottleCancelled: () -> Unit,
    onUncorkHandler: (UncorkMethod) -> Unit,
    onTryAgainHandler: () -> Unit,
    onCastHandler: () -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    contentPadding: PaddingValues = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
    waveSpeed: Dp = WavyProgressIndicatorDefaults.CircularWavelength,
) {
    when (uiState) {
        is DiscoverUiState.Ready -> BottleOnTheShore(
            state = uiState,
            ringProgress = ringProgress,
            motion = motion,
            hasAccelerometer = hasAccelerometer,
            reducedMotion = reducedMotion,
            waveSpeed = waveSpeed,
            onBackHandler = onBackHandler,
            onHelpHandler = onHelpHandler,
            onBottlePressed = onBottlePressed,
            onBottleReleased = onBottleReleased,
            onBottleCancelled = onBottleCancelled,
            onUncorkHandler = onUncorkHandler,
            modifier = modifier,
            windowInsets = windowInsets,
            contentPadding = contentPadding,
        )

        DiscoverUiState.SeaCalm -> EdgeState(
            title = R.string.sea_calm_title,
            body = R.string.sea_calm_body,
            action = R.string.shore_cast,
            actionIcon = R.drawable.ic_glyph_bottle,
            note = R.string.discover_sea_calm_note,
            onAction = onCastHandler,
            onBackHandler = onBackHandler,
            modifier = modifier,
            windowInsets = windowInsets,
        ) { SeaCalmIllustration() }

        DiscoverUiState.Offline -> EdgeState(
            title = R.string.discover_offline_title,
            body = R.string.discover_offline_body,
            action = R.string.try_again,
            actionIcon = R.drawable.ic_symbol_refresh,
            onAction = onTryAgainHandler,
            onBackHandler = onBackHandler,
            modifier = modifier,
            windowInsets = windowInsets,
        ) { FogIllustration() }

        DiscoverUiState.Failed -> EdgeState(
            title = R.string.discover_failed_title,
            body = R.string.discover_failed_body,
            action = R.string.try_again,
            actionIcon = R.drawable.ic_symbol_refresh,
            onAction = onTryAgainHandler,
            onBackHandler = onBackHandler,
            modifier = modifier,
            windowInsets = windowInsets,
        ) { SlippedBackIllustration() }

        // the Shore takes over, with a word about the next tide
        DiscoverUiState.AlreadyFound -> Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
    }
}

@Composable
private fun BottleOnTheShore(
    state: DiscoverUiState.Ready,
    ringProgress: () -> Float,
    motion: DiscoverMotion,
    hasAccelerometer: Boolean,
    reducedMotion: Boolean,
    waveSpeed: Dp,
    onBackHandler: () -> Unit,
    onHelpHandler: () -> Unit,
    onBottlePressed: () -> Unit,
    onBottleReleased: () -> Unit,
    onBottleCancelled: () -> Unit,
    onUncorkHandler: (UncorkMethod) -> Unit,
    modifier: Modifier,
    windowInsets: WindowInsets,
    contentPadding: PaddingValues,
) {
    val spacing = BottlingTheme.spacing
    val popped = state.pop != PopPhase.NotPopped
    val late = state.pop == PopPhase.Late
    DomeScaffold(
        edge = DomeEdge.Expanded,
        contentPadding = contentPadding,
        modifier = modifier,
        windowInsets = windowInsets,
        edgeOffset = motion.edgeOffset,
        flash = motion.flash,
        horizontalAlignment = Alignment.CenterHorizontally,
        scenery = { insets -> DiscoverScenery(insets = insets, motion = motion) },
    ) { toArc ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = toArc - DiscoverDimens.textAboveArc),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ImmersiveTopBar(onBackHandler = onBackHandler, modifier = Modifier.bleed(DiscoverDimens.topBarBleed)) {
                IconButton(
                    onClick = onHelpHandler,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_symbol_help),
                        contentDescription = stringResource(R.string.discover_help),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                UncorkBottle(
                    look = state.look,
                    steps = state.steps,
                    ringProgress = ringProgress,
                    motion = motion,
                    showRing = !popped,
                    waveSpeed = waveSpeed,
                    onPressed = onBottlePressed,
                    onReleased = onBottleReleased,
                    onCancelled = onBottleCancelled,
                    onActivate = { onUncorkHandler(UncorkMethod.Accessibility) },
                    modifier = Modifier.padding(vertical = spacing.m),
                )
            }
            // a hidden copy with the counter holds the prompt's tallest height, so the bottle stays
            // put as the counter comes and goes and "Unrolling…" takes over
            Box(contentAlignment = Alignment.BottomCenter) {
                Prompt(
                    steps = 1,
                    popped = false,
                    late = false,
                    hasAccelerometer = hasAccelerometer,
                    reducedMotion = reducedMotion,
                    modifier = Modifier
                        .alpha(0f)
                        .clearAndSetSemantics {},
                )
                Prompt(
                    steps = state.steps,
                    popped = popped,
                    late = late,
                    hasAccelerometer = hasAccelerometer,
                    reducedMotion = reducedMotion,
                )
            }
        }
        Spacer(modifier = Modifier.height(DiscoverDimens.textAboveArc + DiscoverDimens.pillBelowArc))
        if (!popped) {
            if (reducedMotion) {
                BottlingButton(
                    text = R.string.discover_open_bottle,
                    leadingIcon = R.drawable.ic_glyph_bottle,
                    onTapHandler = { onUncorkHandler(UncorkMethod.Button) },
                )
            } else {
                BottlingButton(
                    text = R.string.discover_tap_to_uncork,
                    buttonType = BottlingButtonType.SECONDARY,
                    onTapHandler = { onUncorkHandler(UncorkMethod.Button) },
                )
                if (hasAccelerometer) {
                    Text(
                        text = stringResource(R.string.discover_hold_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = spacing.s),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(spacing.l))
    }
}

/** The counter, headline and body above the arc; "Unrolling…" while the message is late. */
@Composable
private fun Prompt(
    steps: Int,
    popped: Boolean,
    late: Boolean,
    hasAccelerometer: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = BottlingTheme.spacing
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
        modifier = modifier.padding(horizontal = spacing.m),
    ) {
        if (late) {
            Text(
                text = stringResource(R.string.discover_unrolling),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            return@Column
        }
        if (steps > 0 && !popped) {
            Text(
                text = stringResource(R.string.discover_progress, steps),
                style = MaterialTheme.typography.labelLarge,
                color = BottlingTheme.colors.glass,
            )
        }
        val (title, body) = when {
            reducedMotion -> R.string.discover_title_still to null
            !hasAccelerometer -> R.string.discover_title_tap to R.string.discover_body_tap
            else -> R.string.discover_title_shake to R.string.discover_body_shake
        }
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        if (body != null) {
            Text(
                text = stringResource(body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** An edge state on Discover (spec §3.18): the template, centred under the top bar. */
@Composable
private fun EdgeState(
    @StringRes title: Int,
    @StringRes body: Int,
    @StringRes action: Int,
    @DrawableRes actionIcon: Int?,
    onAction: () -> Unit,
    onBackHandler: () -> Unit,
    modifier: Modifier,
    windowInsets: WindowInsets,
    @StringRes note: Int? = null,
    illustration: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(windowInsets)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = BottlingTheme.spacing.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ImmersiveTopBar(onBackHandler = onBackHandler, modifier = Modifier.bleed(DiscoverDimens.topBarBleed))
        Spacer(modifier = Modifier.height(BottlingTheme.spacing.xxl))
        EmptyState(
            title = stringResource(title),
            body = stringResource(body),
            actionLabel = stringResource(action),
            actionIcon = actionIcon,
            note = note?.let { stringResource(it) },
            onAction = onAction,
            // each takes the bottle's place, often after the pop, so TalkBack says what happened
            announce = true,
            illustration = illustration,
        )
        Spacer(modifier = Modifier.height(BottlingTheme.spacing.xxl))
    }
}

/** The scroll halfway out of the neck: where it waits for the message. */
internal const val HALF_OUT = 0.5f

private const val CORK_FLIGHT_MILLIS = 300
private const val FLASH_MILLIS = 120
private const val FLASH_PEAK = 0.6f
private const val SCROLL_DELAY_MILLIS = 150L
private const val SHIMMER_MILLIS = 1_200
private const val WOBBLE_DAMPING = 0.4f
private const val WOBBLE_STIFFNESS = 400f

@NightDawnPreviews
@Composable
private fun DiscoverScreenPreview() = BottlingPreview {
    DiscoverScreen(
        uiState = DiscoverUiState.Ready(BottleLook.Default, steps = 1, moving = false, pop = PopPhase.NotPopped),
        ringProgress = { 1f / 3 },
        motion = DiscoverMotion.Still,
        hasAccelerometer = true,
        reducedMotion = false,
        onBackHandler = {},
        onHelpHandler = {},
        onBottlePressed = {},
        onBottleReleased = {},
        onBottleCancelled = {},
        onUncorkHandler = {},
        onTryAgainHandler = {},
        onCastHandler = {},
        waveSpeed = 0.dp,
    )
}
