package com.bottlecast.app.feature.shore

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.bottlecast.app.R
import com.bottlecast.app.TopLevelDestination
import com.bottlecast.app.common.components.BottlingSnackbarHost
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.BreathingTide
import com.bottlecast.app.feature.shore.domain.Tide
import com.bottlecast.app.testing.BoardStatusBar
import com.bottlecast.app.testing.BoardTabFrame
import com.bottlecast.app.testing.captureScreenByDrawing
import com.bottlecast.app.testing.captureThemed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
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

    // 200% text on a shorter screen, scrolled to the end: the content scrolls below the status bar
    // rather than passing under it
    @Test
    @Config(fontScale = 2f, qualifiers = SHORT_SCREEN)
    fun largeTextScrolledNight() = captureScrolledToEnd(darkTheme = true)

    @Test
    @Config(fontScale = 2f, qualifiers = SHORT_SCREEN)
    fun largeTextScrolledDawn() = captureScrolledToEnd(darkTheme = false)

    @Test
    fun rtlNight() = capture(SampleWaiting, darkTheme = true, layoutDirection = LayoutDirection.Rtl)

    @Test
    fun rtlDawn() = capture(SampleWaiting, darkTheme = false, layoutDirection = LayoutDirection.Rtl)

    // a 3-button navigation bar under the app's: the pills still sit 32dp above the app's bar
    @Test
    fun navigationBarInsetNight() = capture(SampleWaiting, darkTheme = true, navigationBarInsets = WindowInsets(bottom = 48.dp))

    @Test
    fun navigationBarInsetDawn() = capture(SampleWaiting, darkTheme = false, navigationBarInsets = WindowInsets(bottom = 48.dp))

    // the sea is known to be calm before the tap: casting is offered instead (spec §3.3)
    @Test
    fun seaCalmNight() = capture(ShoreUiState.BottleWaiting(seaCalm = true), darkTheme = true)

    @Test
    fun seaCalmDawn() = capture(ShoreUiState.BottleWaiting(seaCalm = true), darkTheme = false)

    // today's bottle went back to the sea, or was reported: the card goes with it
    @Test
    fun foundTodayWithoutCardNight() = capture(SampleFoundToday.copy(todaysFind = null), darkTheme = true)

    @Test
    fun foundTodayWithoutCardDawn() = capture(SampleFoundToday.copy(todaysFind = null), darkTheme = false)

    @Test
    fun throwBackSnackbarNight() = captureSnackbar(string(R.string.shore_thrown_back), action = string(R.string.shore_undo), darkTheme = true)

    @Test
    fun throwBackSnackbarDawn() = captureSnackbar(string(R.string.shore_thrown_back), action = string(R.string.shore_undo), darkTheme = false)

    @Test
    fun alreadyFoundSnackbarNight() = captureSnackbar(alreadyFound(), action = null, darkTheme = true)

    @Test
    fun alreadyFoundSnackbarDawn() = captureSnackbar(alreadyFound(), action = null, darkTheme = false)

    // "While you were away" (G5), on either side of the tide
    @Test
    fun awayFindsNight() = capture(SampleWaiting.copy(awayFinds = 2), darkTheme = true)

    @Test
    fun awayFindsDawn() = capture(SampleWaiting.copy(awayFinds = 2), darkTheme = false)

    @Test
    fun foundTodayAwayFindsNight() = capture(SampleFoundToday.copy(awayFinds = 1), darkTheme = true)

    @Test
    fun foundTodayAwayFindsDawn() = capture(SampleFoundToday.copy(awayFinds = 1), darkTheme = false)

    @Test
    @Config(fontScale = 2f)
    fun foundTodayLargeTextNight() = capture(SampleFoundToday, darkTheme = true)

    @Test
    fun waitsInChestSnackbarNight() = captureSnackbar(string(R.string.shore_waits_in_chest), action = null, darkTheme = true)

    @Test
    fun waitsInChestSnackbarDawn() = captureSnackbar(string(R.string.shore_waits_in_chest), action = null, darkTheme = false)

    @Test
    fun shieldUsedSnackbarNight() = captureSnackbar(shieldUsed(), action = null, darkTheme = true)

    @Test
    fun shieldUsedSnackbarDawn() = captureSnackbar(shieldUsed(), action = null, darkTheme = false)

    @Test
    fun streakRestedSnackbarNight() = captureSnackbar(string(R.string.shore_streak_rested), action = null, darkTheme = true)

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

    private fun captureScrolledToEnd(darkTheme: Boolean) {
        composeTestRule.setContent {
            BottlingTheme(darkTheme = darkTheme) {
                ShoreFrame(uiState = SampleWaiting, navigationBarInsets = WindowInsets(0))
            }
        }
        // further than the content reaches, so the scroll stops at its end
        composeTestRule.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy ->
            scrollBy(0f, Float.MAX_VALUE)
        }
        composeTestRule.waitForIdle()
        composeTestRule.captureScreenByDrawing()
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

    // the snackbar shows over Shore B, as it does once a throw back or a redirect lands there
    // the Shore-B board's 7h 12m, worded as the app words it
    private fun alreadyFound(): String =
        string(R.string.shore_already_found, string(R.string.shore_countdown_hours_minutes, 7, 12))

    private fun string(@StringRes resId: Int, vararg args: Any): String = RuntimeEnvironment.getApplication().getString(resId, *args)

    private fun shieldUsed(): String = RuntimeEnvironment.getApplication().resources.getQuantityString(R.plurals.shore_shield_used, 6, 6)

    private fun captureSnackbar(message: String, action: String?, darkTheme: Boolean) {
        val hostState = SnackbarHostState()
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            BottlingTheme(darkTheme = darkTheme) {
                ShoreFrame(
                    uiState = SampleFoundToday.copy(todaysFind = null),
                    navigationBarInsets = WindowInsets(0),
                    snackbarHost = { BottlingSnackbarHost(hostState) },
                )
                LaunchedEffect(Unit) {
                    hostState.showSnackbar(message, actionLabel = action, duration = SnackbarDuration.Indefinite)
                }
            }
        }
        // past the snackbar's entrance
        composeTestRule.mainClock.advanceTimeBy(SNACKBAR_SETTLED_MILLIS)
        composeTestRule.waitForIdle()
        composeTestRule.captureScreenByDrawing()
    }

    @Composable
    private fun ShoreFrame(
        uiState: ShoreUiState,
        navigationBarInsets: WindowInsets,
        snackbarHost: @Composable () -> Unit = {},
    ) {
        BoardTabFrame(
            selected = TopLevelDestination.SHORE,
            navigationBarInsets = navigationBarInsets,
            snackbarHost = snackbarHost,
        ) { contentPadding ->
            ShoreScreen(
                uiState = uiState,
                tide = BreathingTide.Still,
                onFindBottleHandler = {},
                onCastHandler = {},
                onModerationHandler = {},
                onOpenChestHandler = {},
                onOpenMeHandler = {},
                onOpenTodaysFindHandler = {},
                onReminderHandler = {},
                contentPadding = contentPadding,
                windowInsets = BoardStatusBar,
            )
        }
    }

    private companion object {
        const val SHORT_SCREEN = "w390dp-h640dp-xhdpi"
        const val SNACKBAR_SETTLED_MILLIS = 1_000L

        val SampleWaiting = ShoreUiState.BottleWaiting(
            streak = ShoreStreak(days = 6, shields = 1),
            stats = ShoreStats(bottled = 12, found = 30, kept = 9),
        )

        // the Shore-B board: 7h 12m to go, so 70% of the day has passed
        val SampleFoundToday = ShoreUiState.FoundToday(
            tide = Tide(untilNextTide = Duration.ofHours(7).plusMinutes(12), dayElapsed = 0.7f),
            todaysFind = TodaysFind(
                messageId = "bottle00000000000001",
                excerpt = "Hoy el mar me devolvió algo que creía perdido: la calma.",
                daysLeft = 7,
            ),
            reminderTime = LocalTime.of(9, 0),
            streak = ShoreStreak(days = 6, shields = 1),
            stats = ShoreStats(bottled = 12, found = 30, kept = 9),
        )

        val SampleFoundTodayUnderAnHour = SampleFoundToday.copy(
            tide = Tide(untilNextTide = Duration.ofMinutes(42), dayElapsed = 1f - 42f / (24 * 60)),
            todaysFind = TodaysFind(messageId = "bottle00000000000002", excerpt = "Hey, that's pretty cool", kept = true),
            reminderTime = null,
        )
    }
}
