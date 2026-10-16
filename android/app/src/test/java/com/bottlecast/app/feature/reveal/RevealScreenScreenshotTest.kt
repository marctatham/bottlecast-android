package com.bottlecast.app.feature.reveal

import android.app.Application
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.bottlecast.app.feature.reveal.domain.CastAgo
import com.bottlecast.app.testing.BoardStatusBar
import com.bottlecast.app.testing.captureThemed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Reveal (spec §3.7, without Keep until slice 4) at the boards' 390×844 with a 44dp status bar, as
 * the Reveal board draws it where the spec doesn't overrule it. The sheen is pinned where a test
 * needs it; otherwise nothing moves.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class RevealScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun strangerNight() = capture(showing(), darkTheme = true)

    @Test
    fun strangerDawn() = capture(showing(), darkTheme = false)

    @Test
    fun penNameNight() = capture(showing(byline = Byline.PenName("Driftwood"), castAgo = CastAgo.Yesterday), darkTheme = true)

    @Test
    fun penNameDawn() = capture(showing(byline = Byline.PenName("Driftwood"), castAgo = CastAgo.Yesterday), darkTheme = false)

    // past 120 characters the message drops to titleLarge-long; past the card's cap it scrolls
    // inside, fading at the edge
    @Test
    fun longMessageNight() = capture(showing(text = LONG_MESSAGE, castAgo = CastAgo.Months(2)), darkTheme = true)

    @Test
    fun longMessageDawn() = capture(showing(text = LONG_MESSAGE, castAgo = CastAgo.Months(2)), darkTheme = false)

    // at 200% the card grows with the page instead of scrolling inside, and the drifting bottle goes
    @Test
    @Config(fontScale = 2f)
    fun largeTextNight() = capture(showing(), darkTheme = true)

    @Test
    @Config(fontScale = 2f)
    fun largeTextDawn() = capture(showing(), darkTheme = false)

    @Test
    fun rtlNight() = capture(showing(), darkTheme = true, layoutDirection = LayoutDirection.Rtl)

    @Test
    fun rtlDawn() = capture(showing(), darkTheme = false, layoutDirection = LayoutDirection.Rtl)

    // a fresh find's moonlight halfway across the card
    @Test
    fun sheenNight() = capture(showing(fresh = true), darkTheme = true, sheen = 0.5f)

    @Test
    fun sheenDawn() = capture(showing(fresh = true), darkTheme = false, sheen = 0.5f)

    // the report waits out its undo window
    @Test
    fun reportPendingNight() = capture(showing(report = RevealReport.Pending), darkTheme = true)

    @Test
    fun reportPendingDawn() = capture(showing(report = RevealReport.Pending), darkTheme = false)

    @Test
    fun reportedNight() = capture(showing(report = RevealReport.Reported(blocked = false, queuedOffline = false)), darkTheme = true)

    @Test
    fun reportedDawn() = capture(showing(report = RevealReport.Reported(blocked = false, queuedOffline = false)), darkTheme = false)

    @Test
    fun reportedAndBlockedNight() = capture(showing(report = RevealReport.Reported(blocked = true, queuedOffline = false)), darkTheme = true)

    @Test
    fun reportedAndBlockedDawn() = capture(showing(report = RevealReport.Reported(blocked = true, queuedOffline = false)), darkTheme = false)

    @Test
    fun reportedOfflineNight() = capture(showing(report = RevealReport.Reported(blocked = false, queuedOffline = true)), darkTheme = true)

    @Test
    fun reportedOfflineDawn() = capture(showing(report = RevealReport.Reported(blocked = false, queuedOffline = true)), darkTheme = false)

    // Keep (spec §3.7): the streak's day in the overline, kept, keeping, and offline
    @Test
    fun streakDayNight() = capture(showing(day = 6), darkTheme = true)

    @Test
    fun streakDayDawn() = capture(showing(day = 6), darkTheme = false)

    @Test
    fun keptNight() = capture(showing(keep = RevealKeep(kept = true), day = 6), darkTheme = true)

    @Test
    fun keptDawn() = capture(showing(keep = RevealKeep(kept = true), day = 6), darkTheme = false)

    @Test
    fun keepingNight() = capture(showing(keep = RevealKeep(keeping = true)), darkTheme = true)

    @Test
    fun keepingDawn() = capture(showing(keep = RevealKeep(keeping = true)), darkTheme = false)

    @Test
    fun keepOfflineNight() = capture(showing(keep = RevealKeep(online = false)), darkTheme = true)

    @Test
    fun keepOfflineDawn() = capture(showing(keep = RevealKeep(online = false)), darkTheme = false)

    @Test
    @Config(fontScale = 2f)
    fun keptLargeTextNight() = capture(showing(keep = RevealKeep(kept = true), day = 6), darkTheme = true)

    @Test
    fun loadingNight() = capture(RevealUiState.Loading, darkTheme = true)

    @Test
    fun loadingDawn() = capture(RevealUiState.Loading, darkTheme = false)

    // the bottle couldn't be read back: it drifted off
    @Test
    fun failedNight() = capture(RevealUiState.Failed, darkTheme = true)

    @Test
    fun failedDawn() = capture(RevealUiState.Failed, darkTheme = false)

    private fun capture(
        uiState: RevealUiState,
        darkTheme: Boolean,
        sheen: Float? = null,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Reveal(uiState, sheen)
            }
        }
    }

    @Composable
    private fun Reveal(uiState: RevealUiState, sheen: Float?) {
        RevealScreen(
            uiState = uiState,
            sheen = { sheen },
            domeOffset = { 0.dp },
            onBackHandler = {},
            onReportHandler = { _, _ -> },
            onUndoReportHandler = {},
            onShareHandler = {},
            onThrowBackHandler = {},
            onBackToShoreHandler = {},
            onRetryHandler = {},
            onKeepHandler = {},
            windowInsets = BoardStatusBar,
            contentPadding = PaddingValues(),
        )
    }

    internal companion object {
        // the board's message
        const val BOARD_MESSAGE = "Hoy el mar me devolvió algo que creía perdido: la calma."

        const val LONG_MESSAGE = "I found your bottle on the worst day of a long winter. I'd walked out to the end of " +
            "the pier because the house was too quiet, and there it was, caught between two stones, still " +
            "corked. I don't know who you are or where you were when you wrote this, but I read it three " +
            "times standing in the wind. Thank you for sending something kind out into the world without " +
            "knowing who would catch it. I'm going to keep walking out to the pier every morning now, just " +
            "in case the sea has something else to say. If you ever doubt that small things matter, they do."

        fun showing(
            text: String = BOARD_MESSAGE,
            byline: Byline = Byline.Stranger,
            castAgo: CastAgo? = CastAgo.Days(3),
            report: RevealReport = RevealReport.None,
            fresh: Boolean = false,
            keep: RevealKeep = RevealKeep(),
            day: Int? = null,
        ) = RevealUiState.Showing(
            // a sea-glass Classic drifts underneath (any id seeded to one would do)
            bottle = RevealedBottle(id = "abc", text = text, byline = byline, castAgo = castAgo),
            report = report,
            fresh = fresh,
            keep = keep,
            day = day,
        )
    }
}
