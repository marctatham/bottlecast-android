package com.bottlecast.app

import androidx.annotation.StringRes
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bottlecast.app.common.analytics.AnalyticsEvent
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.fakes.FakeAuthenticationRepository
import com.bottlecast.app.fakes.FakeChestRepository
import com.bottlecast.app.fakes.FakeSeaActions
import com.bottlecast.app.fakes.FakeSeaRepository
import com.bottlecast.app.fakes.FakeTracker
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import javax.inject.Inject

private const val CHEST_TIMEOUT_MILLIS = 10_000L

private const val MY_BOTTLE = "Small wins count. Today mine was a good cup of tea."

/** The Chest and Keepsake detail (spec §2, flow 4): opening a bottle, keeping it, throwing it back and pulling a cast. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ChestNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var tracker: FakeTracker

    @Inject
    lateinit var authenticationRepository: FakeAuthenticationRepository

    @Inject
    lateinit var seaRepository: FakeSeaRepository

    @Inject
    lateinit var seaActions: FakeSeaActions

    @Inject
    lateinit var chestRepository: FakeChestRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        authenticationRepository.signInAsGoogle()
    }

    @Test
    fun aHeldFind_opensFromTheChest_andIsKeptThere() {
        runBlocking { seaRepository.findTodaysBottle() }
        openChest()

        composeTestRule.onNodeWithText(FakeSeaRepository.SEEDED_MESSAGE).performClick()
        val keep = composeTestRule.activity.resources.getQuantityString(R.plurals.keepsake_keep, 7, 7)
        waitForText(keep)
        Assert.assertEquals(Keepsake.SCREEN_NAME, tracker.screens.last())
        composeTestRule.onNodeWithText(keep).performClick()

        waitForText(string(R.string.keep_kept))
        Assert.assertEquals(listOf(FakeSeaRepository.SEEDED_ID), chestRepository.keeps)
        Espresso.pressBack()
        waitForText(string(R.string.chest_title))
        composeTestRule.onNodeWithText(FakeSeaRepository.SEEDED_MESSAGE).assert(hasText(string(R.string.chip_kept)))
        Assert.assertTrue(chestRepository.viewed > 0)
    }

    @Test
    fun throwingBackFromDetail_offersItsUndoOnTheChest() {
        runBlocking { seaRepository.findTodaysBottle() }
        openChest()
        composeTestRule.onNodeWithText(FakeSeaRepository.SEEDED_MESSAGE).performClick()
        waitForDescription(string(R.string.keepsake_more))

        composeTestRule.onNodeWithContentDescription(string(R.string.keepsake_more)).performClick()
        composeTestRule.onNodeWithText(string(R.string.keepsake_throw_back)).performClick()

        waitForText(string(R.string.shore_thrown_back))
        composeTestRule.onNodeWithText(string(R.string.chest_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.shore_undo)).performClick()
        composeTestRule.waitUntil(conditionDescription = "the throw back is undone") { seaActions.pending.value == null }
        Assert.assertTrue(seaActions.committed.isEmpty())
    }

    @Test
    fun aDriftingCast_isPulledFromTheSea() {
        chestRepository.casts.value = listOf(
            MessageEntity(id = "myBottle000000000001", message = MY_BOTTLE, userId = "google", createdAt = Instant.now()),
        )
        openChest()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.chest_tab_count, string(R.string.chest_tab_cast), 1)).performClick()
        waitForText(MY_BOTTLE)
        composeTestRule.onNodeWithText(MY_BOTTLE).performClick()
        waitForDescription(string(R.string.keepsake_more))

        composeTestRule.onNodeWithContentDescription(string(R.string.keepsake_more)).performClick()
        composeTestRule.onNodeWithText(string(R.string.keepsake_pull)).performClick()
        composeTestRule.onNodeWithText(string(R.string.keepsake_pull_confirm)).performClick()

        waitForText(string(R.string.chest_title))
        composeTestRule.waitUntil(conditionDescription = "the cast has gone") { chestRepository.casts.value.isEmpty() }
        Assert.assertTrue(AnalyticsEvent.BottlePulled in tracker.events)
    }

    @Test
    fun aGuestsCastTab_asksThemToSignIn() {
        authenticationRepository.signInAsGuest()
        openChest()

        composeTestRule.onNodeWithText(string(R.string.chest_tab_cast)).performClick()
        composeTestRule.onNodeWithText(string(R.string.chest_sign_in)).performClick()

        composeTestRule.onNodeWithText(string(R.string.sign_in_title_track)).assertIsDisplayed()
    }

    private fun openChest() {
        composeTestRule.onNodeWithText(string(R.string.nav_chest)).performClick()
        waitForText(string(R.string.chest_title))
    }

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(conditionDescription = "\"$text\" is shown", timeoutMillis = CHEST_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForDescription(description: String) {
        composeTestRule.waitUntil(conditionDescription = "\"$description\" is shown", timeoutMillis = CHEST_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun string(@StringRes resId: Int): String = composeTestRule.activity.getString(resId)
}
