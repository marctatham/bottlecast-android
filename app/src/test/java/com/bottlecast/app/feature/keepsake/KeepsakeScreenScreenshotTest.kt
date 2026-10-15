package com.bottlecast.app.feature.keepsake

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.feature.chest.domain.BottleStanding
import com.bottlecast.app.feature.reveal.Byline
import com.bottlecast.app.feature.reveal.domain.CastAgo
import com.bottlecast.app.testing.BoardStatusBar
import com.bottlecast.app.testing.captureScreenByDrawing
import com.bottlecast.app.testing.captureThemed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Keepsake detail (spec §3.11) at the boards' 390×844 with a 44dp status bar: PNG pixels ÷ 2 = board
 * pixels. Sealing's rows wait for slice 6, so the Kept state is the one the Keepsake-Detail board's
 * frame comes closest to.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class KeepsakeScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun notKeptNight() = capture(notKept(), darkTheme = true)

    @Test
    fun notKeptDawn() = capture(notKept(), darkTheme = false)

    @Test
    fun driftsTomorrowNight() = capture(notKept(daysLeft = 1, notice = KeepsakeNotice.DriftsTomorrow), darkTheme = true)

    @Test
    fun driftsTomorrowDawn() = capture(notKept(daysLeft = 1, notice = KeepsakeNotice.DriftsTomorrow), darkTheme = false)

    @Test
    fun keepOfflineNight() = capture(notKept(online = false), darkTheme = true)

    @Test
    fun keptNight() = capture(kept(), darkTheme = true)

    @Test
    fun keptDawn() = capture(kept(), darkTheme = false)

    @Test
    fun ownDriftingNight() = capture(own(BottleStanding.Drifting), darkTheme = true)

    @Test
    fun ownDriftingDawn() = capture(own(BottleStanding.Drifting), darkTheme = false)

    @Test
    fun ownFoundNight() = capture(own(BottleStanding.Found, foundBySomeone = true), darkTheme = true)

    @Test
    fun ownFoundDawn() = capture(own(BottleStanding.Found, foundBySomeone = true), darkTheme = false)

    @Test
    fun ownKeptNight() = capture(own(BottleStanding.Kept, foundBySomeone = true, keptBySomeone = true), darkTheme = true)

    @Test
    fun ownKeptDawn() = capture(own(BottleStanding.Kept, foundBySomeone = true, keptBySomeone = true), darkTheme = false)

    @Test
    fun underReviewNight() = capture(own(BottleStanding.UnderReview, notice = KeepsakeNotice.UnderReview), darkTheme = true)

    @Test
    fun underReviewDawn() = capture(own(BottleStanding.UnderReview, notice = KeepsakeNotice.UnderReview), darkTheme = false)

    @Test
    fun removedNight() = capture(kept().let { it.copy(detail = it.detail.copy(text = null, standing = BottleStanding.Removed, notice = KeepsakeNotice.Removed, shareable = false)) }, darkTheme = true)

    @Test
    fun removedDawn() = capture(kept().let { it.copy(detail = it.detail.copy(text = null, standing = BottleStanding.Removed, notice = KeepsakeNotice.Removed, shareable = false)) }, darkTheme = false)

    @Test
    fun goneNight() = capture(KeepsakeUiState.Gone, darkTheme = true)

    @Test
    fun goneDawn() = capture(KeepsakeUiState.Gone, darkTheme = false)

    @Test
    @Config(fontScale = 2f)
    fun largeTextNight() = capture(notKept(), darkTheme = true)

    // the overflow's Throw back and Report, and the pull's confirmation, each in a window of their own
    @Test
    fun menuNight() = captureWindows(notKept(), darkTheme = true, menuOpen = true)

    @Test
    fun menuDawn() = captureWindows(notKept(), darkTheme = false, menuOpen = true)

    @Test
    fun pullDialogNight() = captureWindows(own(BottleStanding.Drifting), darkTheme = true, pulling = true)

    @Test
    fun pullDialogDawn() = captureWindows(own(BottleStanding.Drifting), darkTheme = false, pulling = true)

    private fun capture(uiState: KeepsakeUiState, darkTheme: Boolean) {
        composeTestRule.captureThemed(darkTheme = darkTheme) { Keepsake(uiState) }
    }

    private fun captureWindows(uiState: KeepsakeUiState, darkTheme: Boolean, menuOpen: Boolean = false, pulling: Boolean = false) {
        composeTestRule.setContent {
            BottlingTheme(darkTheme = darkTheme) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    Keepsake(uiState, menuOpen = menuOpen, pulling = pulling)
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.captureScreenByDrawing()
    }

    @Composable
    private fun Keepsake(uiState: KeepsakeUiState, menuOpen: Boolean = false, pulling: Boolean = false) {
        KeepsakeScreen(
            uiState = uiState,
            today = TODAY,
            onBackHandler = {},
            onShareHandler = {},
            onKeepHandler = {},
            onThrowBackHandler = {},
            onReportHandler = { _, _ -> },
            onPullHandler = {},
            windowInsets = BoardStatusBar,
            contentPadding = PaddingValues(),
            menuOpen = menuOpen,
            pulling = pulling,
        )
    }

    private companion object {
        // the boards' day
        val TODAY: LocalDate = LocalDate.of(2026, 10, 12)

        fun notKept(daysLeft: Int = 5, notice: KeepsakeNotice? = null, online: Boolean = true) = KeepsakeUiState.Showing(
            KeepsakeDetail(
                messageId = "lighthouse0000000000",
                text = "Somewhere a lighthouse is blinking just for you.",
                byline = Byline.Stranger,
                castAgo = CastAgo.Days(4),
                lang = "en",
                standing = BottleStanding.NotKept(daysLeft),
                notice = notice,
                journey = listOf(
                    JourneyStop(JourneyStep.Cast, TODAY.minusDays(4)),
                    JourneyStop(JourneyStep.Found, TODAY.minusDays(7L - daysLeft)),
                    JourneyStop(JourneyStep.Kept, null),
                ),
                ownCast = false,
                keep = KeepsakeKeep(daysLeft = daysLeft, online = online, keeping = false),
                actions = setOf(KeepsakeAction.ThrowBack, KeepsakeAction.Report),
                shareable = true,
            ),
        )

        // Keepsake #142's message, kept the day after it was found (TEMPLATE.md)
        fun kept() = KeepsakeUiState.Showing(
            KeepsakeDetail(
                messageId = "prettycool0000000000",
                text = "Hey, that's pretty cool",
                byline = Byline.Stranger,
                castAgo = CastAgo.Days(3),
                lang = "en",
                standing = BottleStanding.Kept,
                notice = null,
                journey = listOf(
                    JourneyStop(JourneyStep.Cast, TODAY.minusDays(3)),
                    JourneyStop(JourneyStep.Found, TODAY.minusDays(1)),
                    JourneyStop(JourneyStep.Kept, TODAY.minusDays(1)),
                ),
                ownCast = false,
                keep = null,
                actions = emptySet(),
                shareable = true,
            ),
        )

        fun own(
            standing: BottleStanding,
            foundBySomeone: Boolean = false,
            keptBySomeone: Boolean = false,
            notice: KeepsakeNotice? = null,
        ) = KeepsakeUiState.Showing(
            KeepsakeDetail(
                messageId = "smallwins00000000000",
                text = "Small wins count. Today mine was a good cup of tea.",
                byline = Byline.PenName("Driftwood"),
                castAgo = CastAgo.Days(3),
                lang = "en",
                standing = standing,
                notice = notice,
                journey = listOf(
                    JourneyStop(JourneyStep.Cast, TODAY.minusDays(3)),
                    JourneyStop(JourneyStep.FoundBySomeone, TODAY.minusDays(2).takeIf { foundBySomeone }),
                    JourneyStop(JourneyStep.Kept, TODAY.minusDays(1).takeIf { keptBySomeone }),
                ),
                ownCast = true,
                keep = null,
                actions = if (standing == BottleStanding.Drifting) setOf(KeepsakeAction.Pull) else emptySet(),
                shareable = standing != BottleStanding.UnderReview,
            ),
        )
    }
}
