package com.bottlecast.app.feature.getmessage

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.testing.captureThemed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Today's Discover screen in the Moonlit Shore theme, after the bottle animation has finished.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class GetMessageScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun messageReceivedNight() = capture(RECEIVED, darkTheme = true)

    @Test
    fun messageReceivedDawn() = capture(RECEIVED, darkTheme = false)

    @Test
    fun failureNight() = capture(MessageUiState.Failure, darkTheme = true)

    @Test
    fun failureDawn() = capture(MessageUiState.Failure, darkTheme = false)

    // a finished animation (progress 1) and a final state show the message straight away
    private fun capture(uiState: MessageUiState, darkTheme: Boolean) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            GetMessageView(
                onHomeHandler = {},
                uiState = uiState,
                composition = null,
                progress = { 1f },
            )
        }
    }

    private companion object {
        val RECEIVED = MessageUiState.MessageReceived(MessageEntity(id = "1", message = "Hey, that's pretty cool", userId = "1"))
    }
}
