package com.bottlecast.app.feature.shore

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.fakes.FakeAuthenticationRepository
import com.bottlecast.app.fakes.FakeConnectivityMonitor
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for [ShoreScreen], with its ViewModel built from fakes.
 */
@RunWith(AndroidJUnit4::class)
class ShoreScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val authenticationRepository = FakeAuthenticationRepository()
    private val connectivityMonitor = FakeConnectivityMonitor()
    private var bottlesFound = 0
    private var moderationOpened = 0

    @Before
    fun setUp() {
        composeTestRule.setContent {
            BottlingTheme {
                ShoreScreen(
                    onFindBottleHandler = { bottlesFound++ },
                    onCastHandler = {},
                    onModerationHandler = { moderationOpened++ },
                    onOpenChestHandler = {},
                    onOpenMeHandler = {},
                    contentPadding = PaddingValues(),
                    viewModel = viewModel { ShoreViewModel(authenticationRepository, connectivityMonitor) },
                )
            }
        }
    }

    @Test
    fun shore_offersTodaysBottle_andCasting() {
        composeTestRule.onNodeWithText(string(R.string.shore_waiting_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.shore_cast)).assertIsDisplayed()

        composeTestRule.onNodeWithText(string(R.string.shore_find_bottle)).assertIsEnabled().performClick()

        Assert.assertEquals(1, bottlesFound)
    }

    @Test
    fun offline_showsTheBanner_andHoldsTodaysBottle() {
        connectivityMonitor.isOnline.value = false

        composeTestRule.onNodeWithText(string(R.string.shore_offline_banner)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.shore_find_bottle)).assertIsNotEnabled()
    }

    @Test
    fun admin_opensModeration_fromTheMenu() {
        composeTestRule.onNodeWithContentDescription(string(R.string.shore_more_options)).assertDoesNotExist()

        authenticationRepository.signInAsAdmin()
        composeTestRule.onNodeWithContentDescription(string(R.string.shore_more_options)).performClick()
        composeTestRule.onNodeWithText(string(R.string.shore_moderation)).performClick()

        Assert.assertEquals(1, moderationOpened)
    }

    private fun string(@StringRes resId: Int): String = composeTestRule.activity.getString(resId)
}
