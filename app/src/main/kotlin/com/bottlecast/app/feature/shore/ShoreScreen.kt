package com.bottlecast.app.feature.shore

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.min
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessStarted
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.bottlecast.app.R
import com.bottlecast.app.common.analytics.AnalyticsEvent
import com.bottlecast.app.common.analytics.LocalTracker
import com.bottlecast.app.common.components.Banner
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.BottlingButtonType
import com.bottlecast.app.common.components.DomeEdge
import com.bottlecast.app.common.components.DomeScaffold
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.BreathingTide
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.common.ui.rememberBreathingTide
import com.bottlecast.app.common.ui.rememberIdleMotionEnabled
import com.bottlecast.app.common.ui.rememberReducedMotion
import com.bottlecast.app.feature.shore.components.NextTide
import com.bottlecast.app.feature.shore.components.ReminderChip
import com.bottlecast.app.feature.shore.components.ShoreScene
import com.bottlecast.app.feature.shore.components.ShoreScenery
import com.bottlecast.app.feature.shore.components.ShoreStatsRow
import com.bottlecast.app.feature.shore.components.ShoreTopRow
import com.bottlecast.app.feature.shore.components.TideRing
import com.bottlecast.app.feature.shore.components.TodaysFindCard
import com.bottlecast.app.feature.shore.domain.Tide
import java.time.Duration
import java.time.LocalTime

/**
 * The Shore, the app's home (spec §3.3). State A: today's bottle is waiting. State B: it's been
 * found, and the next tide counts down to local midnight.
 *
 * @param contentPadding the navigation bar's height, which the bottom pills sit above.
 */
@Composable
fun ShoreScreen(
    onFindBottleHandler: () -> Unit,
    onCastHandler: () -> Unit,
    onModerationHandler: () -> Unit,
    onOpenChestHandler: () -> Unit,
    onOpenMeHandler: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: ShoreViewModel = hiltViewModel(),
) {
    val tracker = LocalTracker.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val reducedMotion = rememberReducedMotion()
    val idleMotion = rememberIdleMotionEnabled(reducedMotion)
    val tide = rememberBreathingTide(running = idleMotion)

    // the bottle bobs at 0.6× while it's drawn and the Shore is in front, and stands still under
    // reduced motion
    var bottleShown by remember { mutableStateOf(false) }
    val composition: LottieComposition? by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.message_in_bottle))
    val progress: Float by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = idleMotion && bottleShown,
        restartOnPlay = false,
        iterations = LottieConstants.IterateForever,
        speed = BOTTLE_SPEED,
    )

    ShoreScreen(
        uiState = uiState,
        tide = tide,
        composition = composition,
        progress = { if (reducedMotion) STILL_FRAME else progress },
        onBottleShown = { shown -> bottleShown = shown },
        // The Shore stays drawn while a screen opens over it. Taps can't reach it there, but a
        // keyboard, TalkBack or Switch Access still can, so it only acts while it's started, which
        // means on top.
        onFindBottleHandler = dropUnlessStarted {
            tracker.trackEvent(AnalyticsEvent.GetMessageTapped)
            onFindBottleHandler()
        },
        onCastHandler = dropUnlessStarted {
            tracker.trackEvent(AnalyticsEvent.PostMessageTapped)
            onCastHandler()
        },
        onModerationHandler = dropUnlessStarted(block = onModerationHandler),
        onOpenChestHandler = dropUnlessStarted(block = onOpenChestHandler),
        onOpenMeHandler = dropUnlessStarted(block = onOpenMeHandler),
        // reopening today's bottle comes with Reveal (slice 2), the reminder sheet with slice 4
        onOpenTodaysFindHandler = {},
        onReminderHandler = {},
        contentPadding = contentPadding,
    )
}

/**
 * The stateless Shore. Previews and screenshot tests pass a still [tide], a fixed Lottie
 * [progress] and a composition loaded up front, so they never capture a running animation.
 * [onBottleShown] reports whether the bottle is drawn, so the caller only animates it then.
 *
 * It draws edge to edge in a [DomeScaffold]: the dome reaches up under the status bar, while the
 * content keeps clear of [windowInsets]. The navigation bar's height arrives in [contentPadding].
 */
@Composable
internal fun ShoreScreen(
    uiState: ShoreUiState,
    tide: BreathingTide,
    composition: LottieComposition?,
    progress: () -> Float,
    onFindBottleHandler: () -> Unit,
    onCastHandler: () -> Unit,
    onModerationHandler: () -> Unit,
    onOpenChestHandler: () -> Unit,
    onOpenMeHandler: () -> Unit,
    onOpenTodaysFindHandler: () -> Unit,
    onReminderHandler: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    onBottleShown: (shown: Boolean) -> Unit = {},
) {
    val spacing = BottlingTheme.spacing
    val largeText = isLargeText()
    val scene = if (uiState is ShoreUiState.FoundToday) ShoreScene.FoundToday else ShoreScene.BottleWaiting

    DomeScaffold(
        edge = DomeEdge.Full,
        contentPadding = contentPadding,
        modifier = modifier,
        windowInsets = windowInsets,
        glowCenter = scene.glowCenter,
        edgeOffset = tide.edgeOffset,
        scenery = { insets -> ShoreScenery(scene = scene, tide = tide, insets = insets, largeText = largeText) },
    ) { toArc ->
        // everything above the arc, sized so state A's text ends just above it and state B's card
        // straddles it
        val aboveArc = when (uiState) {
            is ShoreUiState.BottleWaiting -> ShoreDimens.textAboveArc
            is ShoreUiState.FoundToday -> ShoreDimens.cardAboveArc
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = toArc - aboveArc),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(ShoreDimens.topRowOffset))
            ShoreTopRow(
                streak = uiState.streak,
                isAdmin = uiState.isAdmin,
                onStreakHandler = onOpenMeHandler,
                onModerationHandler = onModerationHandler,
            )
            if (uiState.isOffline) {
                Spacer(modifier = Modifier.height(spacing.xs))
                Banner(text = stringResource(R.string.shore_offline_banner), icon = R.drawable.ic_symbol_wifi_off)
            }
            when (uiState) {
                is ShoreUiState.BottleWaiting -> BottleWaitingHero(
                    composition = composition,
                    progress = progress,
                    largeText = largeText,
                    onBottleShown = onBottleShown,
                )
                is ShoreUiState.FoundToday -> FoundTodayHero(
                    state = uiState,
                    tide = tide,
                    onReminderHandler = onReminderHandler,
                )
            }
        }
        if (uiState is ShoreUiState.FoundToday) {
            TodaysFindCard(find = uiState.todaysFind, onClick = onOpenTodaysFindHandler)
        }

        Spacer(modifier = Modifier.weight(1f))

        uiState.stats?.let { stats ->
            ShoreStatsRow(stats = stats, onClick = onOpenMeHandler)
            Spacer(modifier = Modifier.height(ShoreDimens.statsAbovePills))
        }
        when (uiState) {
            is ShoreUiState.BottleWaiting -> {
                BottlingButton(
                    text = R.string.shore_find_bottle,
                    leadingIcon = R.drawable.ic_glyph_bottle,
                    enabled = !uiState.isOffline,
                    onTapHandler = onFindBottleHandler,
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                BottlingButton(
                    text = R.string.shore_cast,
                    buttonType = BottlingButtonType.SECONDARY,
                    onTapHandler = onCastHandler,
                )
            }
            is ShoreUiState.FoundToday -> {
                BottlingButton(text = R.string.shore_cast, onTapHandler = onCastHandler)
                Spacer(modifier = Modifier.height(spacing.xs))
                BottlingButton(
                    text = R.string.shore_open_chest,
                    buttonType = BottlingButtonType.SECONDARY,
                    onTapHandler = onOpenChestHandler,
                )
            }
        }
        Spacer(modifier = Modifier.height(ShoreDimens.pillsAboveBar))
    }
}

/** State A above the arc: the bobbing bottle, then "A bottle washed ashore". */
@Composable
private fun ColumnScope.BottleWaitingHero(
    composition: LottieComposition?,
    progress: () -> Float,
    largeText: Boolean,
    onBottleShown: (shown: Boolean) -> Unit,
) {
    val bottleDescription = stringResource(R.string.shore_bottle_description)
    val currentOnBottleShown by rememberUpdatedState(onBottleShown)
    // the bottle takes whatever room the text leaves; it steps aside for large text, and when
    // that room is too little, as in landscape
    BoxWithConstraints(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val shown = !largeText && maxHeight >= ShoreDimens.bottleMinHeight
        DisposableEffect(shown) {
            currentOnBottleShown(shown)
            onDispose { currentOnBottleShown(false) }
        }
        if (shown) {
            LottieAnimation(
                composition = composition,
                progress = progress,
                modifier = Modifier
                    .height(min(ShoreDimens.bottleHeight, maxHeight))
                    .aspectRatio(ShoreDimens.bottleWidth / ShoreDimens.bottleHeight)
                    .semantics { contentDescription = bottleDescription },
            )
        }
    }
    Text(
        text = stringResource(R.string.shore_waiting_title),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(modifier = Modifier.height(BottlingTheme.spacing.xs))
    Text(
        text = stringResource(R.string.shore_waiting_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = BottlingTheme.spacing.m),
    )
}

/** State B above the arc: the moon in its tide ring, the countdown, and the reminder chip. */
@Composable
private fun FoundTodayHero(
    state: ShoreUiState.FoundToday,
    tide: BreathingTide,
    onReminderHandler: () -> Unit,
) {
    val spacing = BottlingTheme.spacing
    Spacer(modifier = Modifier.height(ShoreDimens.belowTopRow))
    TideRing(dayElapsed = state.tide.dayElapsed, haloAlpha = tide.haloAlpha)
    Spacer(modifier = Modifier.height(ShoreDimens.belowTideRing))
    NextTide(untilNextTide = state.tide.untilNextTide)
    Spacer(modifier = Modifier.height(spacing.xs))
    Text(
        text = stringResource(R.string.shore_found_title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(modifier = Modifier.height(spacing.xxs))
    Text(
        text = stringResource(R.string.shore_found_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = spacing.m),
    )
    state.reminderTime?.let { time ->
        Spacer(modifier = Modifier.height(spacing.xs))
        ReminderChip(time = time, onClick = onReminderHandler)
    }
}

private const val BOTTLE_SPEED = 0.6f
private const val STILL_FRAME = 0f

@NightDawnPreviews
@Composable
private fun ShoreBottleWaitingPreview() = BottlingPreview {
    ShoreScreen(
        uiState = ShoreUiState.BottleWaiting(
            streak = ShoreStreak(days = 6, shields = 1),
            stats = ShoreStats(bottled = 12, found = 30, kept = 9),
        ),
        tide = BreathingTide.Still,
        composition = null,
        progress = { STILL_FRAME },
        onFindBottleHandler = {},
        onCastHandler = {},
        onModerationHandler = {},
        onOpenChestHandler = {},
        onOpenMeHandler = {},
        onOpenTodaysFindHandler = {},
        onReminderHandler = {},
        contentPadding = PaddingValues(),
    )
}

@NightDawnPreviews
@Composable
private fun ShoreFoundTodayPreview() = BottlingPreview {
    Box {
        ShoreScreen(
            uiState = ShoreUiState.FoundToday(
                tide = Tide(untilNextTide = Duration.ofHours(7).plusMinutes(12), dayElapsed = 0.7f),
                todaysFind = TodaysFind("Hoy el mar me devolvió algo que creía perdido: la calma.", isKept = true, daysLeft = 7),
                reminderTime = LocalTime.of(9, 0),
                streak = ShoreStreak(days = 6, shields = 1),
                stats = ShoreStats(bottled = 12, found = 30, kept = 9),
            ),
            tide = BreathingTide.Still,
            composition = null,
            progress = { STILL_FRAME },
            onFindBottleHandler = {},
            onCastHandler = {},
            onModerationHandler = {},
            onOpenChestHandler = {},
            onOpenMeHandler = {},
            onOpenTodaysFindHandler = {},
            onReminderHandler = {},
            contentPadding = PaddingValues(),
        )
    }
}
