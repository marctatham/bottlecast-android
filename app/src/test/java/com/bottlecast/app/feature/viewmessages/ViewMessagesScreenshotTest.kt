package com.bottlecast.app.feature.viewmessages

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.bottlecast.app.testing.captureThemed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Today's admin message list in the Moonlit Shore theme.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class ViewMessagesScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun listNight() = captureList(darkTheme = true)

    @Test
    fun listDawn() = captureList(darkTheme = false)

    // nothing reported is waiting
    @Test
    fun emptyNight() = captureList(darkTheme = true, messages = emptyList())

    @Test
    fun emptyDawn() = captureList(darkTheme = false, messages = emptyList())

    // a pull, remove or restore didn't go through
    @Test
    fun actionFailedNight() = captureList(darkTheme = true, actionFailed = true)

    @Test
    fun actionFailedDawn() = captureList(darkTheme = false, actionFailed = true)

    @Test
    fun errorNight() = captureError(darkTheme = true)

    @Test
    fun errorDawn() = captureError(darkTheme = false)

    private fun captureList(darkTheme: Boolean, messages: List<MessageEntity> = MESSAGES, actionFailed: Boolean = false) {
        composeTestRule.captureThemed(darkTheme = darkTheme) { MessageListView(messages, actionFailed = actionFailed) }
    }

    private fun captureError(darkTheme: Boolean) {
        composeTestRule.captureThemed(darkTheme = darkTheme) { ViewMessagesError() }
    }

    private companion object {
        val MESSAGES = listOf(
            MessageEntity(id = "1", message = "Hey, that's pretty cool", userId = "1", reportCount = 3),
            MessageEntity(
                id = "2",
                message = "Hoy el mar me devolvió algo que creía perdido: la calma.",
                userId = "2",
                status = MessageStatus.UnderReview,
                reportCount = 1,
            ),
        )
    }
}
