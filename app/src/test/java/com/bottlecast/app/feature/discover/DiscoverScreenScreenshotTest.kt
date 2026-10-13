package com.bottlecast.app.feature.discover

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.components.bottle.bottleLookFor
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.feature.discover.components.DiscoverMotion
import com.bottlecast.app.feature.discover.components.UncorkHelpSheet
import com.bottlecast.app.testing.BoardStatusBar
import com.bottlecast.app.testing.captureScreenByDrawing
import com.bottlecast.app.testing.captureThemed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Discover (spec §3.6) at the boards' 390×844 with a 44dp status bar, as the Discover-Idle,
 * Discover-Progress, Edge-SeaCalm and Edge-Offline boards draw it, where the spec doesn't overrule
 * them. Motion is held still: the cork ring's wave doesn't run, and the pop is pinned part-way.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class DiscoverScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun idleNight() = capture(ready(), darkTheme = true)

    @Test
    fun idleDawn() = capture(ready(), darkTheme = false)

    @Test
    fun oneThirdNight() = capture(ready(steps = 1), darkTheme = true, ring = 1f / 3)

    @Test
    fun oneThirdDawn() = capture(ready(steps = 1), darkTheme = false, ring = 1f / 3)

    @Test
    fun twoThirdsNight() = capture(ready(steps = 2), darkTheme = true, ring = 2f / 3)

    @Test
    fun twoThirdsDawn() = capture(ready(steps = 2), darkTheme = false, ring = 2f / 3)

    // held halfway: the ring fills between the thirds
    @Test
    fun holdingNight() = capture(ready(), darkTheme = true, ring = 0.5f)

    @Test
    fun holdingDawn() = capture(ready(), darkTheme = false, ring = 0.5f)

    // the cork is out and the message is late: the scroll waits half out, shimmering
    @Test
    fun unrollingNight() = capture(ready(pop = PopPhase.Late), darkTheme = true, motion = DiscoverMotion.popped(0.5f, shimmer = 0.4f))

    @Test
    fun unrollingDawn() = capture(ready(pop = PopPhase.Late), darkTheme = false, motion = DiscoverMotion.popped(0.5f, shimmer = 0.4f))

    @Test
    fun reducedMotionNight() = capture(ready(), darkTheme = true, reducedMotion = true)

    @Test
    fun reducedMotionDawn() = capture(ready(), darkTheme = false, reducedMotion = true)

    @Test
    fun noAccelerometerNight() = capture(ready(), darkTheme = true, hasAccelerometer = false)

    @Test
    fun noAccelerometerDawn() = capture(ready(), darkTheme = false, hasAccelerometer = false)

    @Test
    @Config(fontScale = 2f)
    fun largeTextNight() = capture(ready(steps = 1), darkTheme = true, ring = 1f / 3)

    @Test
    @Config(fontScale = 2f)
    fun largeTextDawn() = capture(ready(steps = 1), darkTheme = false, ring = 1f / 3)

    @Test
    fun rtlNight() = capture(ready(steps = 1), darkTheme = true, ring = 1f / 3, layoutDirection = LayoutDirection.Rtl)

    @Test
    fun rtlDawn() = capture(ready(steps = 1), darkTheme = false, ring = 1f / 3, layoutDirection = LayoutDirection.Rtl)

    @Test
    fun seaCalmNight() = capture(DiscoverUiState.SeaCalm, darkTheme = true)

    @Test
    fun seaCalmDawn() = capture(DiscoverUiState.SeaCalm, darkTheme = false)

    @Test
    fun offlineNight() = capture(DiscoverUiState.Offline, darkTheme = true)

    @Test
    fun offlineDawn() = capture(DiscoverUiState.Offline, darkTheme = false)

    @Test
    fun failedNight() = capture(DiscoverUiState.Failed, darkTheme = true)

    @Test
    fun failedDawn() = capture(DiscoverUiState.Failed, darkTheme = false)

    @Test
    fun helpSheetNight() = captureHelpSheet(darkTheme = true)

    @Test
    fun helpSheetDawn() = captureHelpSheet(darkTheme = false)

    private fun capture(
        uiState: DiscoverUiState,
        darkTheme: Boolean,
        ring: Float = 0f,
        motion: DiscoverMotion = DiscoverMotion.Still,
        reducedMotion: Boolean = false,
        hasAccelerometer: Boolean = true,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Discover(uiState, ring, motion, reducedMotion, hasAccelerometer)
            }
        }
    }

    // the sheet is its own window, sliding up over Discover, so the clock is moved past its entrance
    private fun captureHelpSheet(darkTheme: Boolean) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            BottlingTheme(darkTheme = darkTheme) {
                Discover(ready(), ring = 0f, motion = DiscoverMotion.Still, reducedMotion = false, hasAccelerometer = true)
                UncorkHelpSheet(onDismissRequest = {})
            }
        }
        composeTestRule.mainClock.advanceTimeBy(SHEET_SETTLED_MILLIS)
        composeTestRule.waitForIdle()
        composeTestRule.captureScreenByDrawing()
    }

    @Composable
    private fun Discover(
        uiState: DiscoverUiState,
        ring: Float,
        motion: DiscoverMotion,
        reducedMotion: Boolean,
        hasAccelerometer: Boolean,
    ) {
        DiscoverScreen(
            uiState = uiState,
            ringProgress = { ring },
            motion = motion,
            hasAccelerometer = hasAccelerometer,
            reducedMotion = reducedMotion,
            onBackHandler = {},
            onHelpHandler = {},
            onBottlePressed = {},
            onBottleReleased = {},
            onUncorkHandler = {},
            onTryAgainHandler = {},
            onCastHandler = {},
            windowInsets = BoardStatusBar,
            waveSpeed = 0.dp,
        )
    }

    private companion object {
        const val SHEET_SETTLED_MILLIS = 2_000L

        // the board's bottle: a sea-glass Classic (any id seeded to one would do)
        val Look = bottleLookFor("abc")

        fun ready(steps: Int = 0, pop: PopPhase = PopPhase.NotPopped) =
            DiscoverUiState.Ready(look = Look, steps = steps, moving = false, pop = pop)
    }
}
