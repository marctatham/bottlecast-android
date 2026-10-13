package com.bottlecast.app.feature.shore

import android.app.Application
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.bottlecast.app.R
import com.bottlecast.app.TopLevelDestination
import com.bottlecast.app.common.components.TabFrame
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.BreathingTide
import com.bottlecast.app.feature.shore.domain.Tide
import com.bottlecast.app.testing.captureScreenByDrawing
import com.bottlecast.app.testing.captureThemed
import com.bottlecast.app.testing.loadLottieComposition
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration
import java.time.LocalTime

/**
 * The Shore (spec §3.3) in its tab frame at the boards' 390×844, with a 44dp status bar and no
 * system navigation bar, as the Main, Shore-A-Dawn and Shore-B boards draw it: PNG pixels ÷ 2 =
 * board pixels. Sample data matches the boards'.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class ShoreScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val composition = loadLottieComposition(R.raw.message_in_bottle)

    @Test
    fun waitingWithSampleDataNight() = capture(SampleWaiting, darkTheme = true)

    @Test
    fun waitingWithSampleDataDawn() = capture(SampleWaiting, darkTheme = false)

    // what the app shows until slices 2 and 4 record streaks and stats
    @Test
    fun waitingNight() = capture(ShoreUiState.BottleWaiting(), darkTheme = true)

    @Test
    fun waitingDawn() = capture(ShoreUiState.BottleWaiting(), darkTheme = false)

    @Test
    fun offlineNight() = capture(ShoreUiState.BottleWaiting(isOffline = true), darkTheme = true)

    @Test
    fun offlineDawn() = capture(ShoreUiState.BottleWaiting(isOffline = true), darkTheme = false)

    @Test
    fun foundTodayNight() = capture(SampleFoundToday, darkTheme = true)

    @Test
    fun foundTodayDawn() = capture(SampleFoundToday, darkTheme = false)

    @Test
    fun foundTodayUnderAnHourNight() = capture(SampleFoundTodayUnderAnHour, darkTheme = true)

    @Test
    fun foundTodayUnderAnHourDawn() = capture(SampleFoundTodayUnderAnHour, darkTheme = false)

    // 200% text through Robolectric's font scale, which scales non-linearly as a device does
    @Test
    @Config(fontScale = 2f)
    fun largeTextNight() = capture(SampleWaiting, darkTheme = true)

    @Test
    @Config(fontScale = 2f)
    fun largeTextDawn() = capture(SampleWaiting, darkTheme = false)

    @Test
    fun rtlNight() = capture(SampleWaiting, darkTheme = true, layoutDirection = LayoutDirection.Rtl)

    // a 3-button navigation bar under the app's: the pills still sit 32dp above the app's bar
    @Test
    fun navigationBarInsetNight() = capture(SampleWaiting, darkTheme = true, navigationBarInsets = WindowInsets(bottom = 48.dp))

    @Test
    fun adminMenuOpenNight() = captureAdminMenu(darkTheme = true)

    @Test
    fun adminMenuOpenDawn() = captureAdminMenu(darkTheme = false)

    private fun capture(
        uiState: ShoreUiState,
        darkTheme: Boolean,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        navigationBarInsets: WindowInsets = WindowInsets(0),
    ) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                ShoreFrame(uiState = uiState, navigationBarInsets = navigationBarInsets)
            }
        }
    }

    // the menu is a popup, its own window, which Roborazzi's screen capture leaves out, so every
    // window is drawn once the menu has finished opening
    private fun captureAdminMenu(darkTheme: Boolean) {
        composeTestRule.setContent {
            BottlingTheme(darkTheme = darkTheme) {
                ShoreFrame(uiState = SampleWaiting.copy(isAdmin = true), navigationBarInsets = WindowInsets(0))
            }
        }
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Moderation").assertExists()
        composeTestRule.captureScreenByDrawing()
    }

    @Composable
    private fun ShoreFrame(uiState: ShoreUiState, navigationBarInsets: WindowInsets) {
        TabFrame(
            selected = TopLevelDestination.SHORE,
            onSelect = {},
            navigationBarInsets = navigationBarInsets,
        ) { contentPadding ->
            ShoreScreen(
                uiState = uiState,
                tide = BreathingTide.Still,
                composition = composition,
                progress = { BOTTLE_PROGRESS },
                onFindBottleHandler = {},
                onCastHandler = {},
                onModerationHandler = {},
                onOpenChestHandler = {},
                onOpenMeHandler = {},
                onOpenTodaysFindHandler = {},
                onReminderHandler = {},
                contentPadding = contentPadding,
                windowInsets = WindowInsets(top = STATUS_BAR),
            )
        }
    }

    private companion object {
        const val BOTTLE_PROGRESS = 0f
        val STATUS_BAR = 44.dp

        val SampleWaiting = ShoreUiState.BottleWaiting(
            streak = ShoreStreak(days = 6, shields = 1),
            stats = ShoreStats(bottled = 12, found = 30, kept = 9),
        )

        // the Shore-B board: 7h 12m to go, so 70% of the day has passed
        val SampleFoundToday = ShoreUiState.FoundToday(
            tide = Tide(untilNextTide = Duration.ofHours(7).plusMinutes(12), dayElapsed = 0.7f),
            todaysFind = TodaysFind(
                excerpt = "Hoy el mar me devolvió algo que creía perdido: la calma.",
                isKept = true,
                daysLeft = 7,
            ),
            reminderTime = LocalTime.of(9, 0),
            streak = ShoreStreak(days = 6, shields = 1),
            stats = ShoreStats(bottled = 12, found = 30, kept = 9),
        )

        val SampleFoundTodayUnderAnHour = SampleFoundToday.copy(
            tide = Tide(untilNextTide = Duration.ofMinutes(42), dayElapsed = 1f - 42f / (24 * 60)),
            todaysFind = TodaysFind(excerpt = "Hey, that's pretty cool", isKept = false, daysLeft = 7),
            reminderTime = null,
        )
    }
}
