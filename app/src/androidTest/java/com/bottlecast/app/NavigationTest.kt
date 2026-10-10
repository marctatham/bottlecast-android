package com.bottlecast.app

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bottlecast.app.fakes.FakeMessageRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// generous, as parsing the bottle Lottie can take over a second on a cold emulator, after which the
// test clock still has to run its ~2.5s animation before the message is revealed
private const val MESSAGE_REVEAL_TIMEOUT_MILLIS = 30_000L

/**
 * Navigation tests for [MessageInABottleApp], backed by the fakes the testdi modules install.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun discoverMessage_showsMessage_thenExplored_returnsHome() {
        composeTestRule.onNodeWithText(string(R.string.home_button_get_message)).performClick()

        composeTestRule.waitUntil(
            conditionDescription = "the discovered message is shown",
            timeoutMillis = MESSAGE_REVEAL_TIMEOUT_MILLIS,
        ) {
            composeTestRule.onAllNodesWithText(FakeMessageRepository.SEEDED_MESSAGE)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText(FakeMessageRepository.SEEDED_MESSAGE).assertIsDisplayed()

        composeTestRule.onNodeWithText(string(R.string.get_message_button_acknowledge)).performClick()

        composeTestRule.onNodeWithText(string(R.string.home_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.home_button_get_message)).assertIsDisplayed()
        composeTestRule.onNodeWithText(FakeMessageRepository.SEEDED_MESSAGE).assertDoesNotExist()
    }

    @Test
    fun discoverMessage_thenBack_returnsHome() {
        composeTestRule.onNodeWithText(string(R.string.home_button_get_message)).performClick()

        composeTestRule.onNodeWithContentDescription(string(R.string.button_back)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.home_title)).assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription(string(R.string.button_back)).performClick()

        composeTestRule.onNodeWithText(string(R.string.home_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.home_button_get_message)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(string(R.string.button_back)).assertDoesNotExist()
    }

    private fun string(@StringRes resId: Int): String = composeTestRule.activity.getString(resId)
}
