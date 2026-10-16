package com.bottlecast.app.feature.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.bottlecast.app.R
import com.bottlecast.app.common.analytics.AnalyticsEvent
import com.bottlecast.app.common.analytics.LocalTracker
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.BottlingButtonType
import com.bottlecast.app.common.components.BottlingTextButton
import com.bottlecast.app.common.components.Dome
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.common.ui.rememberBreathingTide
import com.bottlecast.app.common.ui.rememberHaptics
import com.bottlecast.app.common.ui.rememberReducedMotion
import com.bottlecast.app.feature.onboarding.components.OnboardingArt
import com.bottlecast.app.feature.onboarding.components.OnboardingMotion
import com.bottlecast.app.feature.onboarding.components.OnboardingScenery
import com.bottlecast.app.feature.onboarding.components.PageDots
import com.bottlecast.app.feature.signin.SignInReason
import com.bottlecast.app.feature.signin.SignInSheet
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * First-launch onboarding (spec §3.1): three pages that teach the premise and the one-bottle-a-day
 * rule, then hand over to the first find, or to casting, which asks for Google first.
 *
 * @param onCastHandler goes on to Compose; true when the person has just signed in.
 * @param replay "How BottleCast works" from Settings: every exit just goes back, and nothing is recorded.
 */
@Composable
fun OnboardingScreen(
    onSkipHandler: () -> Unit,
    onFindFirstHandler: () -> Unit,
    onCastHandler: (justSignedIn: Boolean) -> Unit,
    replay: Boolean = false,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val tracker = LocalTracker.current
    val haptics = rememberHaptics()
    val reducedMotion = rememberReducedMotion()
    val tide = rememberBreathingTide()
    val pagerState = rememberPagerState { PAGE_COUNT }
    val scope = rememberCoroutineScope()
    var signingIn by rememberSaveable { mutableStateOf(false) }

    // the seal stamps once, the first time page 3 settles (spec §3.1)
    val stamp = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val stampSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage == LAST_PAGE && stamp.value < 1f) {
            stamp.animateTo(1f, stampSpec)
            haptics.thud()
        }
    }

    // a replay from Settings (spec §3.17) changes nothing, and counts as no first run
    val finish: (OnboardingExit) -> Unit = { exit ->
        if (!replay) {
            viewModel.onFinished()
            tracker.trackEvent(AnalyticsEvent.OnboardingFinished, mapOf(AnalyticsEvent.PARAM_HOW to exit.analyticsValue))
        }
    }

    OnboardingScreen(
        pagerState = pagerState,
        motion = OnboardingMotion(bob = tide.bob, sealStamp = { stamp.value }, haloAlpha = tide.haloAlpha),
        // each exit navigates, so a second tap while onboarding leaves is dropped
        onSkip = dropUnlessResumed {
            finish(OnboardingExit.Skip)
            onSkipHandler()
        },
        onNext = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
        onFindFirst = dropUnlessResumed {
            finish(OnboardingExit.FindFirst)
            onFindFirstHandler()
        },
        onCastInstead = dropUnlessResumed {
            if (uiState.signInToCast && !replay) {
                signingIn = true
            } else {
                finish(OnboardingExit.CastInstead)
                onCastHandler(false)
            }
        },
    )

    if (signingIn) {
        SignInSheet(
            reason = SignInReason.Cast,
            onDismissRequest = { signingIn = false },
            onSignedIn = {
                signingIn = false
                finish(OnboardingExit.CastInstead)
                onCastHandler(true)
            },
        )
    }
}

/** The pages, for previews and screenshot tests, with the pager and the motion hoisted. */
@Composable
internal fun OnboardingScreen(
    pagerState: PagerState,
    motion: OnboardingMotion,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onFindFirst: () -> Unit,
    onCastInstead: () -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    val spacing = BottlingTheme.spacing
    val fullArt = LocalDensity.current.fontScale <= OnboardingDimens.LARGE_TEXT_FROM
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val topInset = windowInsets.asPaddingValues().calculateTopPadding()
        val toArc = maxHeight * OnboardingDimens.dome.fraction - topInset - BottlingTheme.sizes.touchTarget
        Dome(edge = OnboardingDimens.dome)
        OnboardingScenery(
            haloAlpha = motion.haloAlpha,
            // the sky's moon gives way to page 2's, inside its tide ring
            moonAlpha = {
                val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
                abs(position - TIDE_PAGE).coerceIn(0f, 1f)
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BottlingTheme.sizes.touchTarget)
                    .padding(horizontal = spacing.xxs),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (pagerState.currentPage < LAST_PAGE) BottlingTextButton(text = R.string.onboarding_skip, onTapHandler = onSkip)
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                OnboardingPage(page = page, toArc = toArc, artSize = if (fullArt) OnboardingDimens.art else OnboardingDimens.artLargeText, motion = motion)
            }
            PageDots(
                page = pagerState.currentPage,
                pageCount = PAGE_COUNT,
                modifier = Modifier.padding(top = spacing.xs, bottom = OnboardingDimens.dotsAbovePills),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = OnboardingDimens.pillsAreaMinHeight)
                    .padding(horizontal = spacing.gutter),
                contentAlignment = Alignment.BottomCenter,
            ) {
                if (pagerState.currentPage < LAST_PAGE) {
                    BottlingButton(text = R.string.onboarding_next, onTapHandler = onNext)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        BottlingButton(
                            text = R.string.onboarding_find_first,
                            leadingIcon = R.drawable.ic_glyph_bottle,
                            onTapHandler = onFindFirst,
                        )
                        BottlingButton(
                            text = R.string.onboarding_cast_instead,
                            buttonType = BottlingButtonType.SECONDARY,
                            onTapHandler = onCastInstead,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(OnboardingDimens.pillsBottom))
        }
    }
}

/** One page: its illustration in the dome, then its title and body under the arc. */
@Composable
private fun OnboardingPage(page: Int, toArc: Dp, artSize: Dp, motion: OnboardingMotion) {
    val (title, body) = when (page) {
        0 -> R.string.onboarding_1_title to R.string.onboarding_1_body
        1 -> R.string.onboarding_2_title to R.string.onboarding_2_body
        else -> R.string.onboarding_3_title to R.string.onboarding_3_body
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            // above 150% text the copy scrolls under the smaller art (spec §3.1)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = BottlingTheme.spacing.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(toArc),
            contentAlignment = Alignment.Center,
        ) {
            OnboardingArt(page = page, size = artSize, motion = motion)
        }
        Spacer(modifier = Modifier.height(OnboardingDimens.copyBelowArc))
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
            modifier = Modifier.padding(horizontal = BottlingTheme.spacing.xs),
        )
    }
}

internal const val PAGE_COUNT = 3
private const val TIDE_PAGE = 1
private const val LAST_PAGE = PAGE_COUNT - 1

@NightDawnPreviews
@Composable
private fun OnboardingScreenPreview() = BottlingPreview {
    OnboardingScreen(
        pagerState = rememberPagerState { PAGE_COUNT },
        motion = OnboardingMotion.Still,
        onSkip = {},
        onNext = {},
        onFindFirst = {},
        onCastInstead = {},
    )
}
