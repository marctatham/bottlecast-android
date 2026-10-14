package com.bottlecast.app.feature.composepreview

import android.app.Application
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.bottlecast.app.testing.captureThemed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Preview's states (spec §3.4b, the Compose-Preview board), in Night and Dawn. Unit tests build debug,
 * so the on-chain lines are the test network's.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class ComposePreviewScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun firstCastNight() = capture(darkTheme = true, ready())

    @Test
    fun firstCastDawn() = capture(darkTheme = false, ready())

    @Test
    fun termsTickedNight() = capture(darkTheme = true, ready(termsTicked = true))

    @Test
    fun termsTickedDawn() = capture(darkTheme = false, ready(termsTicked = true))

    @Test
    fun signedNight() = capture(darkTheme = true, ready(penName = "Driftwood", signWithPenName = true, needsTerms = false))

    @Test
    fun signedDawn() = capture(darkTheme = false, ready(penName = "Driftwood", signWithPenName = true, needsTerms = false))

    @Test
    fun noPenNameYetNight() = capture(darkTheme = true, ready(penName = null, needsTerms = false))

    @Test
    fun noPenNameYetDawn() = capture(darkTheme = false, ready(penName = null, needsTerms = false))

    @Test
    fun sealingOffNight() = capture(darkTheme = true, ready(sealable = false, needsTerms = false))

    @Test
    fun sealingOffDawn() = capture(darkTheme = false, ready(sealable = false, needsTerms = false))

    @Test
    fun offlineNight() = capture(darkTheme = true, ready(isOffline = true, needsTerms = false))

    @Test
    fun offlineDawn() = capture(darkTheme = false, ready(isOffline = true, needsTerms = false))

    @Test
    fun castingNight() = capture(darkTheme = true, ready(casting = true, needsTerms = false))

    @Test
    fun castingDawn() = capture(darkTheme = false, ready(casting = true, needsTerms = false))

    @Test
    fun atTheCapNight() = capture(darkTheme = true, ready(castsLeft = 0, needsTerms = false))

    @Test
    fun atTheCapDawn() = capture(darkTheme = false, ready(castsLeft = 0, needsTerms = false))

    @Test
    @Config(fontScale = 2f)
    fun largeTextNight() = capture(darkTheme = true, ready())

    // the foot of the page: the settings and the Terms
    @Test
    fun firstCastScrolledNight() = capture(darkTheme = true, ready(), scrolled = true)

    @Test
    fun firstCastScrolledDawn() = capture(darkTheme = false, ready(), scrolled = true)

    @Test
    fun noPenNameYetScrolledNight() = capture(darkTheme = true, ready(penName = null, needsTerms = false), scrolled = true)

    @Test
    fun sealingOffScrolledNight() = capture(darkTheme = true, ready(sealable = false, termsTicked = true), scrolled = true)

    private fun capture(darkTheme: Boolean, uiState: ComposePreviewUiState.Ready, scrolled: Boolean = false) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            ComposePreviewScreen(
                uiState = uiState,
                onEdit = {},
                onCast = {},
                onSignWithPenName = {},
                onSetPenName = {},
                onSealable = {},
                onTermsTicked = {},
                snackbarHostState = remember { SnackbarHostState() },
                windowInsets = BoardBars,
                // past the end, which the scroll clamps to its foot
                scrollState = remember { ScrollState(if (scrolled) Int.MAX_VALUE else 0) },
            )
        }
    }

    private fun ready(
        penName: String? = "Driftwood",
        signWithPenName: Boolean = false,
        sealable: Boolean = true,
        needsTerms: Boolean = true,
        termsTicked: Boolean = false,
        isOffline: Boolean = false,
        castsLeft: Int? = 5,
        casting: Boolean = false,
    ) = ComposePreviewUiState.Ready(
        messageId = "bottle00000000000013",
        text = "If you're reading this, drink some water and text someone you miss.",
        penName = penName,
        signWithPenName = signWithPenName,
        sealable = sealable,
        needsTerms = needsTerms,
        termsTicked = termsTicked,
        isOffline = isOffline,
        castsLeft = castsLeft,
        casting = casting,
    )

    private companion object {
        val BoardBars = WindowInsets(top = 44.dp, bottom = 16.dp)
    }
}
