package com.bottlecast.app

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bottlecast.app.common.analytics.AnalyticsEvent
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.fakes.FakeAuthenticationRepository
import com.bottlecast.app.fakes.FakeOnboardingStore
import com.bottlecast.app.fakes.FakeTracker
import com.bottlecast.app.fakes.FirstLaunchRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/** First-launch onboarding (spec §2, flow 1; §3.1), and where each of its ways out leads. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OnboardingNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val firstLaunch = FirstLaunchRule()

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var tracker: FakeTracker

    @Inject
    lateinit var onboardingStore: FakeOnboardingStore

    @Inject
    lateinit var authenticationRepository: FakeAuthenticationRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    // no account is made during onboarding: the guest comes with the first look at the sea
    @Test
    fun aFirstLaunch_showsOnboarding_andMakesNoGuest() {
        composeTestRule.onNodeWithText(string(R.string.onboarding_1_title)).assertIsDisplayed()

        Assert.assertEquals(UserEntity.UnauthenticatedUser, authenticationRepository.user.value)
        Assert.assertEquals(listOf(Onboarding.screenName), tracker.screens)
    }

    @Test
    fun skip_leadsToTheShore_andOnboardingIsSeen() {
        composeTestRule.onNodeWithText(string(R.string.onboarding_skip)).performClick()

        composeTestRule.onNodeWithText(string(R.string.shore_waiting_title)).assertIsDisplayed()
        Assert.assertTrue(onboardingStore.isSeen)
        Assert.assertTrue(AnalyticsEvent.OnboardingFinished in tracker.events)
    }

    @Test
    fun theLastPage_findsTheFirstBottle_thenBackLeadsToTheShore() {
        toTheLastPage()

        composeTestRule.onNodeWithText(string(R.string.onboarding_find_first)).performClick()

        composeTestRule.onNodeWithContentDescription(string(R.string.discover_bottle)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(string(R.string.button_back)).performClick()
        composeTestRule.onNodeWithText(string(R.string.shore_waiting_title)).assertIsDisplayed()
    }

    @Test
    fun ratherCastOne_signsInFirst_thenOpensCompose() {
        toTheLastPage()

        composeTestRule.onNodeWithText(string(R.string.onboarding_cast_instead)).performClick()
        composeTestRule.onNodeWithText(string(R.string.sign_in_title_cast)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.sign_in_google)).performClick()

        composeTestRule.waitUntil(conditionDescription = "Compose is shown") {
            tracker.screens.lastOrNull() == Compose.SCREEN_NAME
        }
        Assert.assertTrue(onboardingStore.isSeen)
    }

    // onboarding leaves the stack as it ends, so back from the Shore leaves the app
    @Test
    fun afterOnboarding_backFromTheShore_leavesTheApp() {
        composeTestRule.onNodeWithText(string(R.string.onboarding_skip)).performClick()
        composeTestRule.onNodeWithText(string(R.string.shore_waiting_title)).assertIsDisplayed()

        Espresso.pressBackUnconditionally()

        composeTestRule.waitUntil(conditionDescription = "the app has left the screen") {
            !composeTestRule.activityRule.scenario.state.isAtLeast(Lifecycle.State.RESUMED)
        }
    }

    private fun toTheLastPage() {
        repeat(2) { composeTestRule.onNodeWithText(string(R.string.onboarding_next)).performClick() }
        composeTestRule.waitUntil(conditionDescription = "the last page is shown") {
            composeTestRule.onAllNodesWithText(string(R.string.onboarding_find_first)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun string(@StringRes resId: Int): String = composeTestRule.activity.getString(resId)
}
