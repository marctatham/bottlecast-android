package com.bottlecast.app.feature.me

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bottlecast.app.TopLevelDestination
import com.bottlecast.app.testing.BoardStatusBar
import com.bottlecast.app.testing.BoardTabFrame
import com.bottlecast.app.testing.captureThemed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.YearMonth

/**
 * The Me tab (spec §3.12) in its tab frame at the boards' 390×844 with a 44dp status bar: PNG
 * pixels ÷ 2 = board pixels. The numbers are the boards' sample user's (TEMPLATE.md).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class MeScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun penNameNight() = capture(penName(), darkTheme = true, initial = "D")

    @Test
    fun penNameDawn() = capture(penName(), darkTheme = false, initial = "D")

    @Test
    fun anonymousNight() = capture(penName().copy(identity = MeIdentity.Anonymous), darkTheme = true)

    @Test
    fun anonymousDawn() = capture(penName().copy(identity = MeIdentity.Anonymous), darkTheme = false)

    @Test
    fun guestNight() = capture(guest(), darkTheme = true)

    @Test
    fun guestDawn() = capture(guest(), darkTheme = false)

    // offline, the counts aren't known
    @Test
    fun offlineNight() = capture(penName().copy(stats = MeStats(guest = false, streak = 6, best = 14, shields = 1)), darkTheme = true, initial = "D")

    @Test
    fun loadingNight() = capture(MeUiState.Loading, darkTheme = true)

    @Test
    @Config(fontScale = 2f)
    fun largeTextNight() = capture(penName(), darkTheme = true, initial = "D")

    private fun capture(uiState: MeUiState, darkTheme: Boolean, initial: String? = null) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            BoardTabFrame(selected = TopLevelDestination.ME, meInitial = initial) { contentPadding ->
                MeScreen(
                    uiState = uiState,
                    onOpenSettingsHandler = {},
                    onSetPenNameHandler = {},
                    onSignInHandler = {},
                    contentPadding = contentPadding,
                    windowInsets = BoardStatusBar,
                )
            }
        }
    }

    private companion object {
        fun penName() = MeUiState.Showing(
            identity = MeIdentity.PenName("Driftwood"),
            castingSince = YearMonth.of(2026, 10),
            stats = MeStats(guest = false, bottled = 12, found = 30, kept = 9, streak = 6, best = 14, shields = 1),
        )

        fun guest() = MeUiState.Showing(
            identity = MeIdentity.Guest,
            castingSince = null,
            stats = MeStats(guest = true, found = 3, streak = 2, best = 2, shields = 0),
        )
    }
}
