package com.bottlecast.app

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
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
import com.bottlecast.app.common.sea.domain.PendingSeaAction
import com.bottlecast.app.fakes.FakeConnectivityMonitor
import com.bottlecast.app.fakes.FakeSeaActions
import com.bottlecast.app.fakes.FakeSeaRepository
import com.bottlecast.app.fakes.FakeTracker
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

// generous: the cork's pop plays out before Reveal opens, and a cold emulator is slow
private const val REVEAL_TIMEOUT_MILLIS = 10_000L

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

    @Inject
    lateinit var seaRepository: FakeSeaRepository

    @Inject
    lateinit var seaActions: FakeSeaActions

    @Inject
    lateinit var connectivityMonitor: FakeConnectivityMonitor

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun findTodaysBottle_uncorked_showsTheMessageWithoutTheBar_thenBack_showsTodaysFind() {
        openDiscover()
        uncorkWithThePill()

        waitForReveal()
        composeTestRule.onNodeWithText(FakeSeaRepository.SEEDED_MESSAGE).assertIsDisplayed()
        // immersive routes hide the navigation bar (spec §2)
        composeTestRule.onNodeWithText(string(R.string.nav_chest)).assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription(string(R.string.button_back)).performClick()

        assertTodaysFindIsShown(withCard = true)
        Assert.assertEquals(listOf(FakeSeaRepository.SEEDED_ID), seaRepository.finds)
    }

    // the find is made at the pop, never on the way in (spec §3.6)
    @Test
    fun findTodaysBottle_thenBackBeforeThePop_leavesTodaysBottleWaiting() {
        openDiscover()
        composeTestRule.onNodeWithText(string(R.string.shore_waiting_title)).assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription(string(R.string.button_back)).performClick()

        assertShoreIsShown()
        composeTestRule.onNodeWithContentDescription(string(R.string.button_back)).assertDoesNotExist()
        Assert.assertTrue(seaRepository.finds.isEmpty())
    }

    @Test
    fun threeTapsOnTheBottle_popTheCork() {
        openDiscover()

        repeat(3) { bottle().performClick() }

        waitForReveal()
        Assert.assertTrue(AnalyticsEvent.BottleUncorked in tracker.events)
    }

    // TalkBack, Switch Access and keyboards open the bottle in one go (spec §3.6)
    @Test
    fun bottleActivatedByTalkBack_opensAtOnce() {
        openDiscover()

        bottle().performSemanticsAction(SemanticsActions.OnClick)

        waitForReveal()
    }

    @Test
    fun corksProgress_survivesARecreate() {
        openDiscover()
        bottle().performClick()
        composeTestRule.onNodeWithText(progress(1)).assertIsDisplayed()

        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.onNodeWithText(progress(1)).assertIsDisplayed()
        bottle().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, cork(1)))
    }

    @Test
    fun throwBack_thenUndo_keepsTodaysFind() {
        revealTodaysBottle()

        composeTestRule.onNode(hasText(string(R.string.reveal_throw_back)) and hasClickAction()).performClick()

        waitForText(string(R.string.shore_thrown_back))
        composeTestRule.onNodeWithText(excerpt()).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.shore_undo)).performClick()

        assertTodaysFindIsShown(withCard = true)
        Assert.assertTrue(seaActions.committed.isEmpty())
    }

    @Test
    fun throwBack_oncePastItsWindow_sendsTheBottleBack() {
        revealTodaysBottle()
        composeTestRule.onNode(hasText(string(R.string.reveal_throw_back)) and hasClickAction()).performClick()
        waitForText(string(R.string.shore_thrown_back))

        seaActions.expireAll()

        composeTestRule.waitUntil(conditionDescription = "the snackbar goes with its window") {
            composeTestRule.onAllNodesWithText(string(R.string.shore_thrown_back)).fetchSemanticsNodes().isEmpty()
        }
        assertTodaysFindIsShown(withCard = false)
        Assert.assertTrue(seaActions.committed.single() is PendingSeaAction.ThrowBack)
    }

    @Test
    fun report_andBlock_thenBackToTheShore_leavesNoCard() {
        revealTodaysBottle()

        composeTestRule.onNodeWithContentDescription(string(R.string.reveal_report_description)).performClick()
        composeTestRule.onNodeWithText(string(R.string.report_block)).performClick()
        composeTestRule.onNode(hasText(string(R.string.report_confirm)) and hasClickAction()).performClick()
        composeTestRule.onNodeWithText(string(R.string.reveal_reported_hidden)).assertIsDisplayed()

        seaActions.expireAll()

        waitForText(string(R.string.reveal_reported_thanks))
        composeTestRule.onNodeWithText(string(R.string.reveal_reported_blocked)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.reveal_back_to_shore)).performClick()

        assertTodaysFindIsShown(withCard = false)
        val report = seaActions.committed.single() as PendingSeaAction.Report
        Assert.assertTrue(report.blockWriter)
    }

    // the sea went calm between the Shore's look ahead and the pop
    @Test
    fun calmSeaAtThePop_offersCasting_andKeepsTodaysBottle() {
        openDiscover()
        seaRepository.seaCalm = true

        uncorkWithThePill()

        waitForText(string(R.string.discover_sea_calm_note))
        composeTestRule.onNodeWithText(string(R.string.sea_calm_title)).assertIsDisplayed()
        Assert.assertTrue(seaRepository.finds.isEmpty())
    }

    @Test
    fun alreadyFoundElsewhere_returnsToTheShore_withTheNextTide() {
        openDiscover()
        // today's bottle was found on another phone in the meantime, and it was another bottle
        seaRepository.foundElsewhere("foundOnTheOtherPhone")

        uncorkWithThePill()

        val notice = string(R.string.shore_already_found).substringBefore("%1")
        composeTestRule.waitUntil(conditionDescription = "the Shore says when the next tide is", timeoutMillis = REVEAL_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithText(notice, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(string(R.string.shore_found_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.shore_find_excerpt, FakeSeaRepository.ELSEWHERE_MESSAGE),
        ).assertIsDisplayed()
        Assert.assertTrue(seaRepository.finds.isEmpty())
    }

    @Test
    fun offline_rollsTheFogIn_untilTheConnectionComesBack() {
        openDiscover()

        connectivityMonitor.isOnline.value = false
        composeTestRule.onNodeWithText(string(R.string.discover_offline_title)).assertIsDisplayed()

        connectivityMonitor.isOnline.value = true
        bottle().assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.discover_offline_title)).assertDoesNotExist()
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
        openDiscover()

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

    // casting needs Google, so the sheet comes first, then Compose with the pen-name offer (spec §3.2)
    @Test
    fun signingInToCast_goesStraightOnToCompose() {
        composeTestRule.onNodeWithText(string(R.string.shore_cast)).performClick()
        composeTestRule.onNodeWithText(string(R.string.sign_in_title_cast)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.sign_in_google)).performClick()

        composeTestRule.waitUntil(conditionDescription = "Compose is shown") {
            tracker.screens.lastOrNull() == Compose.SCREEN_NAME
        }
        waitForText(string(R.string.sign_in_signed_in))
        Assert.assertEquals(listOf(Shore.screenName, Compose.SCREEN_NAME), tracker.screens)
        Assert.assertTrue(AnalyticsEvent.SignedIn in tracker.events)
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

    private fun assertTodaysFindIsShown(withCard: Boolean) {
        composeTestRule.onNodeWithText(string(R.string.shore_found_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.nav_shore)).assertIsDisplayed()
        val card = composeTestRule.onNodeWithText(excerpt())
        if (withCard) card.assertIsDisplayed() else card.assertDoesNotExist()
    }

    private fun openDiscover() {
        composeTestRule.onNodeWithText(string(R.string.shore_find_bottle)).performClick()
        bottle().assertIsDisplayed()
    }

    // the pill, not the title of the same words a phone without an accelerometer shows; with
    // animations off it's reduced motion's Open bottle
    private fun uncorkWithThePill() {
        val pill = hasText(string(R.string.discover_tap_to_uncork)) or hasText(string(R.string.discover_open_bottle))
        composeTestRule.onNode(pill and hasClickAction()).performClick()
    }

    private fun revealTodaysBottle() {
        openDiscover()
        uncorkWithThePill()
        waitForReveal()
        // Reveal's actions wait for its entrance to finish
        composeTestRule.waitForIdle()
    }

    private fun waitForReveal() = waitForText(FakeSeaRepository.SEEDED_MESSAGE)

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(conditionDescription = "\"$text\" is shown", timeoutMillis = REVEAL_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun bottle() = composeTestRule.onNodeWithContentDescription(string(R.string.discover_bottle))

    private fun excerpt(): String = composeTestRule.activity.getString(R.string.shore_find_excerpt, FakeSeaRepository.SEEDED_MESSAGE)

    private fun progress(steps: Int): String = composeTestRule.activity.getString(R.string.discover_progress, steps)

    private fun cork(steps: Int): String = composeTestRule.activity.getString(R.string.discover_cork_loosening, steps)

    private fun string(@StringRes resId: Int): String = composeTestRule.activity.getString(resId)
}
