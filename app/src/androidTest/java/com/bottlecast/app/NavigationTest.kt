package com.bottlecast.app

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bottlecast.app.common.analytics.AnalyticsEvent
import com.bottlecast.app.fakes.FakeMessageRepository
import com.bottlecast.app.fakes.FakeTracker
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

// generous, as parsing the bottle Lottie can take over a second on a cold emulator, after which the
// test clock still has to run its ~2.5s animation before the message is revealed
private const val MESSAGE_REVEAL_TIMEOUT_MILLIS = 30_000L

// well inside a screen transition (BottlingMotion.SCREEN_MILLIS)
private const val MID_TRANSITION_MILLIS = 100L

/**
 * Navigation tests for [MessageInABottleApp]: the Shore, its tabs and today's immersive screens,
 * backed by the fakes the testdi modules install.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var tracker: FakeTracker

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun findTodaysBottle_showsMessageWithoutTheBar_thenExplored_returnsToShore() {
        composeTestRule.onNodeWithText(string(R.string.shore_find_bottle)).performClick()

        composeTestRule.waitUntil(
            conditionDescription = "the discovered message is shown",
            timeoutMillis = MESSAGE_REVEAL_TIMEOUT_MILLIS,
        ) {
            composeTestRule.onAllNodesWithText(FakeMessageRepository.SEEDED_MESSAGE)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText(FakeMessageRepository.SEEDED_MESSAGE).assertIsDisplayed()
        // immersive routes hide the navigation bar (spec §2)
        composeTestRule.onNodeWithText(string(R.string.nav_chest)).assertDoesNotExist()

        composeTestRule.onNodeWithText(string(R.string.get_message_button_acknowledge)).performClick()

        assertShoreIsShown()
        composeTestRule.onNodeWithText(FakeMessageRepository.SEEDED_MESSAGE).assertDoesNotExist()
    }

    @Test
    fun findTodaysBottle_thenBack_returnsToShore() {
        composeTestRule.onNodeWithText(string(R.string.shore_find_bottle)).performClick()

        composeTestRule.onNodeWithContentDescription(string(R.string.button_back)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.shore_waiting_title)).assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription(string(R.string.button_back)).performClick()

        assertShoreIsShown()
        composeTestRule.onNodeWithContentDescription(string(R.string.button_back)).assertDoesNotExist()
    }

    @Test
    fun tabTappedAsAScreenOpens_landsOnThatScreen() {
        // the Shore stays drawn, bar and all, while the screen opening over it fades in
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithText(string(R.string.shore_find_bottle)).performClick()
        composeTestRule.mainClock.advanceTimeBy(MID_TRANSITION_MILLIS)
        composeTestRule.onNodeWithText(string(R.string.nav_chest)).performClick()
        composeTestRule.mainClock.autoAdvance = true

        // the tap landed on the opening screen, not on the bar beneath it
        composeTestRule.onNodeWithContentDescription(string(R.string.button_back)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.nav_chest)).assertDoesNotExist()
    }

    // TalkBack, Switch Access and keyboards act on a node directly, not through a tap the opening
    // screen could take
    @Test
    fun tabActivatedAsAScreenOpens_isIgnored() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithText(string(R.string.shore_find_bottle)).performClick()
        composeTestRule.mainClock.advanceTimeBy(MID_TRANSITION_MILLIS)
        composeTestRule.onNodeWithText(string(R.string.nav_chest)).performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.onNodeWithContentDescription(string(R.string.button_back)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.nav_chest)).assertDoesNotExist()
    }

    @Test
    fun findActivatedTwiceAsItsScreenOpens_opensItOnce() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithText(string(R.string.shore_find_bottle)).performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.mainClock.advanceTimeBy(MID_TRANSITION_MILLIS)
        composeTestRule.onNodeWithText(string(R.string.shore_find_bottle)).performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.mainClock.autoAdvance = true

        // one back is enough to return to the Shore
        composeTestRule.onNodeWithContentDescription(string(R.string.button_back)).performClick()
        assertShoreIsShown()
        Assert.assertEquals(1, tracker.events.count { it == AnalyticsEvent.GetMessageTapped })
    }

    @Test
    fun backTappedTwiceAsAScreenCloses_leavesTheShoreShowing() {
        composeTestRule.onNodeWithText(string(R.string.shore_cast)).performClick()
        composeTestRule.onNodeWithText(string(R.string.login_required_title)).assertIsDisplayed()

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithContentDescription(string(R.string.button_back)).performClick()
        composeTestRule.mainClock.advanceTimeBy(MID_TRANSITION_MILLIS)
        // the closing screen is still drawn over the Shore, back arrow and all
        composeTestRule.onNodeWithContentDescription(string(R.string.button_back)).performClick()
        composeTestRule.mainClock.autoAdvance = true

        assertShoreIsShown()
    }

    @Test
    fun screenViews_countEachScreenOnce_evenAcrossARecreate() {
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.onNodeWithText(string(R.string.nav_chest)).performClick()
        composeTestRule.onNodeWithText(string(R.string.chest_empty_title)).assertIsDisplayed()

        Assert.assertEquals(listOf(Shore.screenName, Chest.screenName), tracker.screens)
    }

    @Test
    fun signingInToCast_goesStraightOnToPosting() {
        composeTestRule.onNodeWithText(string(R.string.shore_cast)).performClick()
        composeTestRule.onNodeWithText(string(R.string.login_required_button_primary)).performClick()
        composeTestRule.onNodeWithText(string(R.string.btnSignInAnonymously)).performClick()

        composeTestRule.waitUntil(conditionDescription = "posting is shown") {
            tracker.screens.lastOrNull() == PostMessage.screenName
        }
        // the Shore never became the screen in between
        Assert.assertEquals(
            listOf(Shore.screenName, PostMessageLoginRequired.screenName, PostMessage.screenName),
            tracker.screens,
        )
    }

    @Test
    fun tabs_switchBetweenTheirRoots_thenBackLeadsToTheShore_thenOut() {
        composeTestRule.onNodeWithText(string(R.string.nav_chest)).performClick()
        composeTestRule.onNodeWithText(string(R.string.chest_empty_title)).assertIsDisplayed()

        composeTestRule.onNodeWithText(string(R.string.nav_me)).performClick()
        composeTestRule.onNodeWithText(string(R.string.me_placeholder_title)).assertIsDisplayed()

        // back from any tab root leads to the Shore (spec §2)
        Espresso.pressBack()
        assertShoreIsShown()

        // and back from the Shore leaves the app
        Espresso.pressBackUnconditionally()
        composeTestRule.waitUntil(conditionDescription = "the app has left the screen") {
            !composeTestRule.activityRule.scenario.state.isAtLeast(Lifecycle.State.RESUMED)
        }
        Assert.assertFalse(composeTestRule.activityRule.scenario.state.isAtLeast(Lifecycle.State.RESUMED))
    }

    @Test
    fun chest_goToTheShore_returnsToShore() {
        composeTestRule.onNodeWithText(string(R.string.nav_chest)).performClick()

        composeTestRule.onNodeWithText(string(R.string.go_to_shore)).performClick()

        assertShoreIsShown()
        composeTestRule.onNodeWithText(string(R.string.chest_empty_title)).assertDoesNotExist()
    }

    private fun assertShoreIsShown() {
        composeTestRule.onNodeWithText(string(R.string.shore_waiting_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.shore_find_bottle)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.nav_shore)).assertIsDisplayed()
    }

    private fun string(@StringRes resId: Int): String = composeTestRule.activity.getString(resId)
}
