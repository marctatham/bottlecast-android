package com.bottlecast.app

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bottlecast.app.fakes.FakeMessageRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

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

    @Inject
    lateinit var messageRepository: FakeMessageRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun discoverMessage_thenBack_returnsHome() {
        composeTestRule.onNodeWithText(string(R.string.home_button_get_message)).performClick()

        composeTestRule.onNodeWithContentDescription(string(R.string.button_back)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.home_title)).assertDoesNotExist()
        Assert.assertEquals(1, messageRepository.getMessageCalls)

        composeTestRule.onNodeWithContentDescription(string(R.string.button_back)).performClick()

        composeTestRule.onNodeWithText(string(R.string.home_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.home_button_get_message)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(string(R.string.button_back)).assertDoesNotExist()
    }

    private fun string(@StringRes resId: Int): String = composeTestRule.activity.getString(resId)
}
