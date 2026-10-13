package com.bottlecast.app.feature.postmessage.ui

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bottlecast.app.R
import com.bottlecast.app.testing.captureThemed
import com.bottlecast.app.testing.loadLottieComposition
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Today's compose and cast-success screens in the Moonlit Shore theme.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class PostMessageScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val composition = loadLottieComposition(R.raw.message_in_bottle)

    @Test
    fun idleNight() = captureCompose(PostMessageUiState.Idle, darkTheme = true)

    @Test
    fun idleDawn() = captureCompose(PostMessageUiState.Idle, darkTheme = false)

    @Test
    fun failureNight() = captureCompose(PostMessageUiState.Failure(FailureReason.Unknown), darkTheme = true)

    @Test
    fun failureDawn() = captureCompose(PostMessageUiState.Failure(FailureReason.Unknown), darkTheme = false)

    @Test
    fun successNight() = captureSuccess(darkTheme = true)

    @Test
    fun successDawn() = captureSuccess(darkTheme = false)

    private fun captureCompose(uiState: PostMessageUiState, darkTheme: Boolean) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            PostMessageView(onBackHandler = {}, uiState = uiState, onPostHandler = {})
        }
    }

    private fun captureSuccess(darkTheme: Boolean) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            PostMessageSuccessView(onHomeHandler = {}, composition = composition, progress = { 0f })
        }
    }
}
