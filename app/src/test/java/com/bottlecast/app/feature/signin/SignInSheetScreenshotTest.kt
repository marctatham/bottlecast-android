package com.bottlecast.app.feature.signin

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bottlecast.app.common.components.rememberBottlingSheetState
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.testing.captureScreenByDrawing
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The lazy sign-in sheet's states (spec §3.2, the SignIn-Sheet board), in Night and Dawn. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class SignInSheetScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun idleNight() = capture(darkTheme = true, SignInUiState())

    @Test
    fun idleDawn() = capture(darkTheme = false, SignInUiState())

    @Test
    fun carryingTodaysFindNight() = capture(darkTheme = true, SignInUiState(carriesTodaysFind = true))

    @Test
    fun carryingTodaysFindDawn() = capture(darkTheme = false, SignInUiState(carriesTodaysFind = true))

    // a guest's streak comes too (slice 4), with today's find or on its own
    @Test
    fun carryingStreakAndFindNight() = capture(darkTheme = true, SignInUiState(carriesTodaysFind = true, carriedStreak = 3))

    @Test
    fun carryingStreakAndFindDawn() = capture(darkTheme = false, SignInUiState(carriesTodaysFind = true, carriedStreak = 3))

    @Test
    fun carryingStreakNight() = capture(darkTheme = true, SignInUiState(carriedStreak = 3))

    @Test
    fun carryingStreakDawn() = capture(darkTheme = false, SignInUiState(carriedStreak = 3))

    // signing in to keep a bottle (spec §3.2's titles)
    @Test
    fun keepNight() = capture(darkTheme = true, SignInUiState(carriesTodaysFind = true, carriedStreak = 6), reason = SignInReason.Keep)

    @Test
    fun keepDawn() = capture(darkTheme = false, SignInUiState(carriesTodaysFind = true, carriedStreak = 6), reason = SignInReason.Keep)

    @Test
    fun loadingNight() = capture(darkTheme = true, SignInUiState(phase = SignInPhase.Loading, carriesTodaysFind = true))

    @Test
    fun loadingDawn() = capture(darkTheme = false, SignInUiState(phase = SignInPhase.Loading, carriesTodaysFind = true))

    @Test
    fun failedNight() = capture(darkTheme = true, SignInUiState(phase = SignInPhase.Failed))

    @Test
    fun failedDawn() = capture(darkTheme = false, SignInUiState(phase = SignInPhase.Failed))

    @Test
    fun unavailableNight() = capture(darkTheme = true, SignInUiState(phase = SignInPhase.Unavailable))

    @Test
    fun unavailableDawn() = capture(darkTheme = false, SignInUiState(phase = SignInPhase.Unavailable))

    @Test
    fun offlineNight() = capture(darkTheme = true, SignInUiState(isOffline = true))

    @Test
    fun offlineDawn() = capture(darkTheme = false, SignInUiState(isOffline = true))

    @Test
    @Config(fontScale = 2f)
    fun largeTextNight() = capture(darkTheme = true, SignInUiState(carriesTodaysFind = true))

    // the sheet is its own window and animates open, so the clock is advanced past the animation
    // and every window is drawn, scrim included
    @OptIn(ExperimentalMaterial3Api::class)
    private fun capture(darkTheme: Boolean, uiState: SignInUiState, reason: SignInReason = SignInReason.Cast) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            BottlingTheme(darkTheme = darkTheme) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                SignInSheet(
                    uiState = uiState,
                    reason = reason,
                    sheetState = rememberBottlingSheetState(),
                    onDismissRequest = {},
                    onContinue = {},
                    onNotNow = {},
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        composeTestRule.waitForIdle()
        composeTestRule.captureScreenByDrawing()
    }

    private companion object {
        const val SETTLE_MILLIS = 2_000L
    }
}
