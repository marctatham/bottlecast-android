package com.bottlecast.app.feature.reveal.components

import android.app.Application
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.sea.domain.ReportReason
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.feature.reveal.RevealScreen
import com.bottlecast.app.feature.reveal.RevealScreenScreenshotTest
import com.bottlecast.app.testing.BoardStatusBar
import com.bottlecast.app.testing.captureScreenByDrawing
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The report dialog (spec §3.19) over Reveal, as the Report-Dialog board draws it: its own window,
 * over the theme's scrim. No reason is chosen to start with.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class ReportDialogScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun unchosenNight() = capture(darkTheme = true)

    @Test
    fun unchosenDawn() = capture(darkTheme = false)

    @Test
    fun chosenNight() = capture(darkTheme = true, reason = ReportReason.Spam, block = true)

    @Test
    fun chosenDawn() = capture(darkTheme = false, reason = ReportReason.Spam, block = true)

    // at 200% the card is taller than the screen, so it scrolls
    @Test
    @Config(fontScale = 2f)
    fun largeTextNight() = capture(darkTheme = true)

    @Test
    @Config(fontScale = 2f)
    fun largeTextDawn() = capture(darkTheme = false)

    private fun capture(darkTheme: Boolean, reason: ReportReason? = null, block: Boolean = false) {
        composeTestRule.setContent {
            BottlingTheme(darkTheme = darkTheme) {
                RevealScreen(
                    uiState = RevealScreenScreenshotTest.showing(),
                    sheen = { null },
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
                ReportDialog(onDismissRequest = {}, onReportHandler = { _, _ -> }, initialReason = reason, initialBlock = block)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.captureScreenByDrawing()
    }
}
