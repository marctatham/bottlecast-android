package com.bottlecast.app.feature.compose

import android.app.Application
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.feature.compose.domain.DailyPrompt
import com.bottlecast.app.feature.compose.domain.DraftMeter
import com.bottlecast.app.feature.compose.domain.composeHelper
import com.bottlecast.app.testing.captureScreenByDrawing
import com.bottlecast.app.testing.captureThemed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** Compose's states (spec §3.4, the Compose board), in Night and Dawn. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class ComposeScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyNight() = capture(darkTheme = true, text = "")

    @Test
    fun emptyDawn() = capture(darkTheme = false, text = "")

    @Test
    fun writingNight() = capture(darkTheme = true, text = MESSAGE, lang = "en")

    @Test
    fun writingDawn() = capture(darkTheme = false, text = MESSAGE, lang = "en")

    @Test
    fun twentyLeftNight() = capture(darkTheme = true, text = longMessage(220))

    @Test
    fun twentyLeftDawn() = capture(darkTheme = false, text = longMessage(220))

    @Test
    fun atTheLimitNight() = capture(darkTheme = true, text = longMessage(240))

    @Test
    fun atTheLimitDawn() = capture(darkTheme = false, text = longMessage(240))

    @Test
    fun emojiNight() = capture(darkTheme = true, text = EMOJI)

    @Test
    fun emojiDawn() = capture(darkTheme = false, text = EMOJI)

    @Test
    fun tooShortNight() = capture(darkTheme = true, text = "Hello sea", minimumTapped = true)

    @Test
    fun tooShortDawn() = capture(darkTheme = false, text = "Hello sea", minimumTapped = true)

    @Test
    fun oneCastLeftNight() = capture(darkTheme = true, text = MESSAGE, lang = "en", castsLeft = 1)

    @Test
    fun oneCastLeftDawn() = capture(darkTheme = false, text = MESSAGE, lang = "en", castsLeft = 1)

    @Test
    fun atTheCapNight() = capture(darkTheme = true, text = MESSAGE, lang = "en", castsLeft = 0)

    @Test
    fun atTheCapDawn() = capture(darkTheme = false, text = MESSAGE, lang = "en", castsLeft = 0)

    @Test
    fun keyboardOpenNight() = capture(darkTheme = true, text = MESSAGE, lang = "en", imeVisible = true)

    @Test
    fun keyboardOpenDawn() = capture(darkTheme = false, text = MESSAGE, lang = "en", imeVisible = true)

    @Test
    @Config(fontScale = 2f)
    fun largeTextNight() = capture(darkTheme = true, text = MESSAGE, lang = "en")

    @Test
    fun discardDialogNight() = captureDialog(darkTheme = true)

    @Test
    fun discardDialogDawn() = captureDialog(darkTheme = false)

    private fun capture(
        darkTheme: Boolean,
        text: String,
        lang: String? = null,
        minimumTapped: Boolean = false,
        castsLeft: Int? = 5,
        imeVisible: Boolean = false,
    ) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            ComposeScreen(
                uiState = writing(text, lang, minimumTapped, castsLeft),
                editor = remember { TextFieldState(text) },
                imeVisible = imeVisible,
                onClose = {},
                onPreview = {},
                onPreviewTooShort = {},
                onLimit = {},
                onKeepWriting = {},
                onToss = {},
                snackbarHostState = remember { SnackbarHostState() },
                windowInsets = BoardBars,
            )
        }
    }

    // the dialog is its own window, drawn over the screen with its dim
    private fun captureDialog(darkTheme: Boolean) {
        composeTestRule.setContent {
            BottlingTheme(darkTheme = darkTheme) {
                ComposeScreen(
                    uiState = writing(MESSAGE, "en", minimumTapped = false, castsLeft = 5).copy(discarding = true),
                    editor = remember { TextFieldState(MESSAGE) },
                    imeVisible = false,
                    onClose = {},
                    onPreview = {},
                    onPreviewTooShort = {},
                    onLimit = {},
                    onKeepWriting = {},
                    onToss = {},
                    snackbarHostState = remember { SnackbarHostState() },
                    windowInsets = BoardBars,
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.captureScreenByDrawing()
    }

    private fun writing(text: String, lang: String?, minimumTapped: Boolean, castsLeft: Int?): ComposeUiState.Writing {
        val meter = DraftMeter.of(text)
        return ComposeUiState.Writing(
            placeholder = DailyPrompt.forDate(LocalDate.of(2026, 10, 14)),
            meter = meter,
            helper = composeHelper(meter, minimumTapped, lang),
            castsLeft = castsLeft,
        )
    }

    private fun longMessage(characters: Int): String =
        (LONG_SOURCE.repeat(4)).take(characters)

    private companion object {
        // TEMPLATE.md's sample message
        const val MESSAGE = "If you're reading this, drink some water and text someone you miss."
        const val LONG_SOURCE = "Leave the porch light on for the people still finding their way home. "
        // 30 letters and 58 emoji: 262 bytes, so the bytes run out long before the characters
        val EMOJI = "The tide came in gold tonight " + "🌊🌙".repeat(29)

        // the boards' status bar, and a gesture bar for the bottom bar to sit above
        val BoardBars = WindowInsets(top = 44.dp, bottom = 16.dp)
    }
}
