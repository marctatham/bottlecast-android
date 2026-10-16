package com.bottlecast.app

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bottlecast.app.common.analytics.AnalyticsEvent
import com.bottlecast.app.fakes.FakeAuthenticationRepository
import com.bottlecast.app.fakes.FakeChestRepository
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

// Reveal's entrance and a cold emulator can be slow
private const val KEEP_TIMEOUT_MILLIS = 10_000L

/** Keeping today's find from Reveal (spec §2, flow 2): at once for a Google account, after signing in for a guest. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class KeepFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var tracker: FakeTracker

    @Inject
    lateinit var authenticationRepository: FakeAuthenticationRepository

    @Inject
    lateinit var chestRepository: FakeChestRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun aGoogleFind_isKept_andTheShoreSaysSo() {
        authenticationRepository.signInAsGoogle()
        revealTodaysBottle()

        composeTestRule.onNodeWithText(string(R.string.keep_bottle)).performClick()

        waitForText(string(R.string.chip_in_chest_kept))
        Assert.assertEquals(listOf(FakeSeaRepository.SEEDED_ID), chestRepository.keeps)
        Assert.assertTrue(AnalyticsEvent.BottleKept in tracker.events)

        composeTestRule.onNodeWithContentDescription(string(R.string.button_back)).performClick()
        waitForText(string(R.string.shore_found_title))
        composeTestRule.onNodeWithText(string(R.string.chip_kept), substring = true).assertIsDisplayed()
    }

    @Test
    fun aGuest_signsInToKeep_thenItsKept() {
        revealTodaysBottle()

        composeTestRule.onNodeWithText(string(R.string.keep_bottle)).performClick()
        composeTestRule.onNodeWithText(string(R.string.sign_in_title_keep)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.sign_in_google)).performClick()

        waitForText(string(R.string.chip_in_chest_kept))
        Assert.assertEquals(listOf(FakeSeaRepository.SEEDED_ID), chestRepository.keeps)
    }

    @Test
    fun leavingAFreshFindUnkept_saysItWaitsInTheChest() {
        revealTodaysBottle()

        Espresso.pressBack()

        waitForText(string(R.string.shore_waits_in_chest))
        Assert.assertTrue(chestRepository.keeps.isEmpty())
    }

    private fun revealTodaysBottle() {
        composeTestRule.onNodeWithText(string(R.string.shore_find_bottle)).performClick()
        val pill = hasText(string(R.string.discover_tap_to_uncork)) or hasText(string(R.string.discover_open_bottle))
        composeTestRule.onNode(pill and hasClickAction()).performClick()
        waitForText(FakeSeaRepository.SEEDED_MESSAGE)
        // Reveal's actions wait for its entrance to finish
        composeTestRule.waitForIdle()
    }

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(conditionDescription = "\"$text\" is shown", timeoutMillis = KEEP_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun string(@StringRes resId: Int): String = composeTestRule.activity.getString(resId)
}
