package com.bottlecast.app.feature.shore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.min
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
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
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.BreathingTide
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.common.ui.rememberBreathingTide
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
    val tide = rememberBreathingTide()

    // the bottle bobs at 0.6× while the Shore is in front, and stands still under reduced motion
    val reducedMotion = rememberReducedMotion()
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val composition: LottieComposition? by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.message_in_bottle))
    val progress: Float by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = !reducedMotion && lifecycleState.isAtLeast(Lifecycle.State.RESUMED),
        restartOnPlay = false,
        iterations = LottieConstants.IterateForever,
        speed = BOTTLE_SPEED,
    )

    ShoreScreen(
        uiState = uiState,
        tide = tide,
        composition = composition,
        progress = { if (reducedMotion) STILL_FRAME else progress },
        onFindBottleHandler = {
            tracker.trackEvent(AnalyticsEvent.GetMessageTapped)
            onFindBottleHandler()
        },
        onCastHandler = {
            tracker.trackEvent(AnalyticsEvent.PostMessageTapped)
            onCastHandler()
        },
        onModerationHandler = onModerationHandler,
        onOpenChestHandler = onOpenChestHandler,
        onOpenMeHandler = onOpenMeHandler,
        // reopening today's bottle comes with Reveal (slice 2), the reminder sheet with slice 4
        onOpenTodaysFindHandler = {},
        onReminderHandler = {},
        contentPadding = contentPadding,
    )
}

/**
 * The stateless Shore. Previews and screenshot tests pass a still [tide], a fixed Lottie
 * [progress] and a composition loaded up front, so they never capture a running animation.
 *
 * It draws edge to edge: the dome reaches up under the status bar, while the content keeps clear
 * of it through [windowInsets]. The navigation bar's height arrives in [contentPadding] instead,
 * so it isn't counted twice.
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
) {
    val spacing = BottlingTheme.spacing
    val density = LocalDensity.current
    val topInset = with(density) { windowInsets.getTop(density).toDp() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val arcY = maxHeight * DomeEdge.Full.fraction
        ShoreScenery(
            scene = if (uiState is ShoreUiState.FoundToday) ShoreScene.FoundToday else ShoreScene.BottleWaiting,
            tide = tide,
            topInset = topInset,
        )

        // the content fills the screen and only scrolls when large text makes it taller (spec §4)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .windowInsetsPadding(windowInsets)
                .padding(bottom = contentPadding.calculateBottomPadding())
                .padding(horizontal = spacing.gutter),
        ) {
            // everything above the arc, sized so state A's text ends just above it and state B's
            // card straddles it
            val aboveArc = when (uiState) {
                is ShoreUiState.BottleWaiting -> ShoreDimens.textAboveArc
                is ShoreUiState.FoundToday -> ShoreDimens.cardAboveArc
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = arcY - aboveArc - topInset),
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
                    is ShoreUiState.BottleWaiting -> BottleWaitingHero(composition = composition, progress = progress)
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
}

/** State A above the arc: the bobbing bottle, then "A bottle washed ashore". */
@Composable
private fun ColumnScope.BottleWaitingHero(
    composition: LottieComposition?,
    progress: () -> Float,
) {
    val bottleDescription = stringResource(R.string.shore_bottle_description)
    // the bottle takes whatever room the text leaves; it steps aside for large text, and when
    // that room is too little, as in landscape
    val largeText = LocalDensity.current.fontScale >= ShoreDimens.LARGE_TEXT_SCALE
    BoxWithConstraints(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        if (!largeText && maxHeight >= ShoreDimens.bottleMinHeight) {
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
