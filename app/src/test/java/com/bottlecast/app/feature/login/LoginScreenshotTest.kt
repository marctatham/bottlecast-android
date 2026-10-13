package com.bottlecast.app.feature.login

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
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.bottlecast.app.testing.captureThemed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Today's sign-in explainer and sign-in sheet in the Moonlit Shore theme.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class LoginScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun educationalNight() = captureEducational(darkTheme = true)

    @Test
    fun educationalDawn() = captureEducational(darkTheme = false)

    @Test
    fun sheetNight() = captureSheet(darkTheme = true)

    @Test
    fun sheetDawn() = captureSheet(darkTheme = false)

    private fun captureEducational(darkTheme: Boolean) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            LoginRequiredToPostScreen(onProceedHandler = {}, onCancelHandler = {}, onBackHandler = {})
        }
    }

    // the sheet is its own window and animates open, so the clock is advanced past the animation
    // and the whole screen, scrim included, is captured
    @OptIn(ExperimentalRoborazziApi::class, ExperimentalMaterial3Api::class)
    private fun captureSheet(darkTheme: Boolean) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            BottlingTheme(darkTheme = darkTheme) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                LoginBottomSheet(
                    signInWithGoogleHandler = {},
                    signInAnonymouslyHandler = {},
                    onDismissRequest = {},
                    onSignInCompleteHandler = {},
                    bottomSheetState = rememberBottlingSheetState(),
                    uiState = State.Anonymous,
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        composeTestRule.waitForIdle()
        captureScreenRoboImage()
    }

    private companion object {
        const val SETTLE_MILLIS = 2_000L
    }
}
