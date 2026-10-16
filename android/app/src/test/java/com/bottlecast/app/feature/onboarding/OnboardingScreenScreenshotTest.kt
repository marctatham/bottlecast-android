package com.bottlecast.app.feature.onboarding

import android.app.Application
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.bottlecast.app.feature.onboarding.components.OnboardingMotion
import com.bottlecast.app.testing.captureThemed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Onboarding's three pages (spec §3.1, the Onboarding-1 and Onboarding-3 boards), in Night and Dawn. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class OnboardingScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun page1Night() = capture(darkTheme = true, page = 0)

    @Test
    fun page1Dawn() = capture(darkTheme = false, page = 0)

    @Test
    fun page2Night() = capture(darkTheme = true, page = 1)

    @Test
    fun page2Dawn() = capture(darkTheme = false, page = 1)

    @Test
    fun page3Night() = capture(darkTheme = true, page = 2)

    @Test
    fun page3Dawn() = capture(darkTheme = false, page = 2)

    @Test
    fun page3StampingNight() = capture(darkTheme = true, page = 2, motion = OnboardingMotion(bob = { 0f }, sealStamp = { 0.4f }, haloAlpha = { 1f }))

    @Test
    @Config(fontScale = 2f)
    fun page1LargeTextNight() = capture(darkTheme = true, page = 0)

    @Test
    fun page3RightToLeftNight() = capture(darkTheme = true, page = 2, layoutDirection = LayoutDirection.Rtl)

    private fun capture(
        darkTheme: Boolean,
        page: Int,
        motion: OnboardingMotion = OnboardingMotion.Still,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                OnboardingScreen(
                    pagerState = rememberPagerState(initialPage = page) { PAGE_COUNT },
                    motion = motion,
                    onSkip = {},
                    onNext = {},
                    onFindFirst = {},
                    onCastInstead = {},
                    windowInsets = BoardBars,
                )
            }
        }
    }

    private companion object {
        // the boards' status bar, and the 16dp gesture bar their last pill keeps 24dp above
        val BoardBars = WindowInsets(top = 44.dp, bottom = 16.dp)
    }
}
