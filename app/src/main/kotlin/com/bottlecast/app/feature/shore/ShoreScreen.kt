package com.bottlecast.app.feature.shore

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import com.bottlecast.app.common.components.bottle.FloatingBottle
import com.bottlecast.app.common.components.bottle.FloatingBottleDefaults
import com.bottlecast.app.common.components.illustrations.SeaCalmIllustration
import com.bottlecast.app.common.ui.BottlingMotion
import com.bottlecast.app.common.ui.rememberHaptics
import com.bottlecast.app.feature.shore.components.countdownText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.bottlecast.app.R
import com.bottlecast.app.common.analytics.AnalyticsEvent
import com.bottlecast.app.common.analytics.LocalTracker
import com.bottlecast.app.common.components.Banner
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.BottlingButtonType
import com.bottlecast.app.common.components.DomeEdge
import com.bottlecast.app.common.components.DomeScaffold
import com.bottlecast.app.common.components.TideRing
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.isLargeText
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
import com.bottlecast.app.feature.shore.components.TodaysFindCard
import com.bottlecast.app.feature.shore.domain.Tide
import java.time.Duration
import java.time.LocalTime

/**
 * The Shore, the app's home (spec §3.3). State A: today's bottle is waiting. State B: it's been
 * found, and the next tide counts down to local midnight.
 *
 * @param contentPadding the navigation bar's height, which the bottom pills sit above.
 * @param snackbarHostState where the throw back's Undo and the Shore's notices show.
 * @param alreadyFoundNotice Discover found today already had a find; say when the next tide is.
 */
@Composable
fun ShoreScreen(
    onFindBottleHandler: () -> Unit,
    onCastHandler: () -> Unit,
    onModerationHandler: () -> Unit,
    onOpenChestHandler: () -> Unit,
    onOpenMeHandler: () -> Unit,
    onOpenTodaysFindHandler: (messageId: String) -> Unit,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    alreadyFoundNotice: Boolean = false,
    onNoticeShown: () -> Unit = {},
    viewModel: ShoreViewModel = hiltViewModel(),
) {
    val tracker = LocalTracker.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val reducedMotion = rememberReducedMotion()
    val idleMotion = rememberIdleMotionEnabled(reducedMotion)
    val tide = rememberBreathingTide(running = idleMotion)
    val haptics = rememberHaptics()
    val found = uiState as? ShoreUiState.FoundToday

    // a throw back's Undo waits as long as its window does: when the window ends, the action is
    // written and the snackbar goes with it
    val pendingThrowBack = found?.pendingThrowBack
    val thrownBack = stringResource(R.string.shore_thrown_back)
    val undo = stringResource(R.string.shore_undo)
    LaunchedEffect(pendingThrowBack?.id) {
        val action = pendingThrowBack ?: return@LaunchedEffect
        when (snackbarHostState.showSnackbar(thrownBack, actionLabel = undo, duration = SnackbarDuration.Indefinite)) {
            SnackbarResult.ActionPerformed -> viewModel.onUndoThrowBack(action.id)
            SnackbarResult.Dismissed -> viewModel.onCommitThrowBack(action.id)
        }
    }
    val alreadyFound = found?.let { stringResource(R.string.shore_already_found, countdownText(it.tide.untilNextTide)) }
    // Marked shown only once it's been seen: a rotation, or leaving the Shore, while it's up shows it
    // again next time rather than losing it.
    LaunchedEffect(alreadyFoundNotice, alreadyFound != null) {
        if (alreadyFoundNotice && alreadyFound != null) {
            snackbarHostState.showSnackbar(alreadyFound)
            onNoticeShown()
        }
    }
    // at midnight the tide turns, with a tick (spec §3.3)
    var wasFound by remember { mutableStateOf(found != null) }
    LaunchedEffect(found != null) {
        if (wasFound && found == null) haptics.tick()
        wasFound = found != null
    }

    AnimatedContent(
        targetState = uiState,
        transitionSpec = {
            if (reducedMotion) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                fadeIn(BottlingMotion.screen()) togetherWith fadeOut(BottlingMotion.screen())
            }
        },
        contentKey = { it::class },
        label = "tide",
    ) { state ->
        ShoreScreen(
            uiState = state,
            tide = tide,
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
            onOpenTodaysFindHandler = dropUnlessStarted {
                (state as? ShoreUiState.FoundToday)?.todaysFind?.let { onOpenTodaysFindHandler(it.messageId) }
            },
            // the reminder sheet comes with slice 4
            onReminderHandler = {},
            contentPadding = contentPadding,
            rollCountdown = !reducedMotion,
        )
    }
}

/**
 * The stateless Shore. Previews and screenshot tests pass a still [tide], so they never capture a
 * running animation.
 *
 * It draws edge to edge in a [DomeScaffold]: the dome reaches up under the status bar, while the
 * content keeps clear of [windowInsets]. The navigation bar's height arrives in [contentPadding].
 */
@Composable
internal fun ShoreScreen(
    uiState: ShoreUiState,
    tide: BreathingTide,
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
    rollCountdown: Boolean = true,
) {
    val spacing = BottlingTheme.spacing
    val largeText = isLargeText()
    val scene = when {
        uiState is ShoreUiState.FoundToday -> ShoreScene.FoundToday
        uiState is ShoreUiState.BottleWaiting && uiState.seaCalm -> ShoreScene.SeaCalm
        else -> ShoreScene.BottleWaiting
    }

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
                is ShoreUiState.BottleWaiting -> if (uiState.seaCalm) {
                    SeaCalmHero()
                } else {
                    BottleWaitingHero(tide = tide, largeText = largeText)
                }
                is ShoreUiState.FoundToday -> FoundTodayHero(
                    state = uiState,
                    tide = tide,
                    rollCountdown = rollCountdown,
                    onReminderHandler = onReminderHandler,
                )
            }
        }
        if (uiState is ShoreUiState.FoundToday) {
            uiState.todaysFind?.let { find -> TodaysFindCard(find = find, onClick = onOpenTodaysFindHandler) }
        }

        Spacer(modifier = Modifier.weight(1f))

        uiState.stats?.let { stats ->
            ShoreStatsRow(stats = stats, onClick = onOpenMeHandler)
            Spacer(modifier = Modifier.height(ShoreDimens.statsAbovePills))
        }
        when (uiState) {
            // a calm sea offers casting in place of finding (spec §3.3)
            is ShoreUiState.BottleWaiting -> if (uiState.seaCalm) {
                BottlingButton(text = R.string.shore_cast, leadingIcon = R.drawable.ic_glyph_bottle, onTapHandler = onCastHandler)
                Spacer(modifier = Modifier.height(spacing.xs))
                BottlingButton(
                    text = R.string.shore_open_chest,
                    buttonType = BottlingButtonType.SECONDARY,
                    onTapHandler = onOpenChestHandler,
                )
            } else {
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
private fun ColumnScope.BottleWaitingHero(tide: BreathingTide, largeText: Boolean) {
    val bottleDescription = stringResource(R.string.shore_bottle_description)
    // the bottle takes whatever room the text leaves; it steps aside for large text, and when
    // that room is too little, as in landscape
    BoxWithConstraints(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        if (!largeText && maxHeight >= ShoreDimens.bottleMinHeight) {
            FloatingBottle(
                height = min(ShoreDimens.bottleHeight, maxHeight - FloatingBottleDefaults.ripplesUnderBottle),
                bob = tide.bob,
                modifier = Modifier.semantics { contentDescription = bottleDescription },
            )
        }
    }
    HeroText(title = R.string.shore_waiting_title, body = R.string.shore_waiting_body)
}

/** State A on a calm sea (spec §3.3, §3.18): the moon over still water, and no bottle to find. */
@Composable
private fun ColumnScope.SeaCalmHero() {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        SeaCalmIllustration()
    }
    HeroText(title = R.string.sea_calm_title, body = R.string.sea_calm_body)
}

@Composable
private fun HeroText(@StringRes title: Int, @StringRes body: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(modifier = Modifier.height(BottlingTheme.spacing.xs))
    Text(
        text = stringResource(body),
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
    rollCountdown: Boolean,
    onReminderHandler: () -> Unit,
) {
    val spacing = BottlingTheme.spacing
    Spacer(modifier = Modifier.height(ShoreDimens.belowTopRow))
    TideRing(dayElapsed = state.tide.dayElapsed, haloAlpha = tide.haloAlpha)
    Spacer(modifier = Modifier.height(ShoreDimens.belowTideRing))
    NextTide(untilNextTide = state.tide.untilNextTide, rollDigits = rollCountdown)
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


@NightDawnPreviews
@Composable
private fun ShoreBottleWaitingPreview() = BottlingPreview {
    ShoreScreen(
        uiState = ShoreUiState.BottleWaiting(
            streak = ShoreStreak(days = 6, shields = 1),
            stats = ShoreStats(bottled = 12, found = 30, kept = 9),
        ),
        tide = BreathingTide.Still,
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
                todaysFind = TodaysFind("preview", "Hoy el mar me devolvió algo que creía perdido: la calma."),
                reminderTime = LocalTime.of(9, 0),
                streak = ShoreStreak(days = 6, shields = 1),
                stats = ShoreStats(bottled = 12, found = 30, kept = 9),
            ),
            tide = BreathingTide.Still,
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
