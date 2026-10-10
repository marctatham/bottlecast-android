package com.bottlecast.app.feature.home

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.fakes.FakeAuthenticationRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for [HomeScreen].
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        composeTestRule.setContent {
            BottlingTheme {
                HomeScreen(
                    onGetMessageHandler = {},
                    onPostMessageHandler = {},
                    onViewMessagesHandler = {},
                    // signed out, so it's the regular home screen rather than the admin one
                    viewModel = viewModel { HomeViewModel(FakeAuthenticationRepository()) },
                )
            }
        }
    }

    @Test
    fun homeScreen_displaysTitleAndActions() {
        composeTestRule.onNodeWithText(string(R.string.home_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.home_button_get_message))
            .assertIsDisplayed()
            .assertHasClickAction()
        composeTestRule.onNodeWithText(string(R.string.home_button_put_message))
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    private fun string(@StringRes resId: Int): String = composeTestRule.activity.getString(resId)
}
