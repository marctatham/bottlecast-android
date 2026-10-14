package com.bottlecast.app.feature.reveal

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bottlecast.app.R
import com.bottlecast.app.common.sea.domain.PendingSeaAction
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.fakes.FakeAuthenticationRepository
import com.bottlecast.app.fakes.FakeSeaActions
import com.bottlecast.app.fakes.FakeSeaRepository
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock

/**
 * UI tests for [RevealScreen], with its ViewModel built from fakes holding the seeded bottle.
 */
@RunWith(AndroidJUnit4::class)
class RevealScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val authenticationRepository = FakeAuthenticationRepository()
    private val seaRepository = FakeSeaRepository(authenticationRepository)
    private val seaActions = FakeSeaActions(seaRepository)
    private var leaves = 0

    @Before
    fun setUp() {
        composeTestRule.setContent {
            BottlingTheme {
                RevealScreen(
                    onBackHandler = {},
                    onLeaveHandler = { leaves++ },
                    viewModel = viewModel {
                        RevealViewModel(
                            SavedStateHandle(mapOf("messageId" to FakeSeaRepository.SEEDED_ID, "fresh" to false)),
                            seaRepository,
                            seaActions,
                            Clock.systemDefaultZone(),
                        )
                    },
                )
            }
        }
    }

    @Test
    fun theMessage_showsWithItsByline() {
        composeTestRule.onNodeWithText(FakeSeaRepository.SEEDED_MESSAGE).assertIsDisplayed()

        val castAgo = composeTestRule.activity.resources.getQuantityString(R.plurals.reveal_cast_days, 3, 3)
        val byline = composeTestRule.activity.getString(R.string.message_byline_when, string(R.string.message_byline_stranger), castAgo)
        composeTestRule.onNodeWithText(byline).assertIsDisplayed()
    }

    @Test
    fun aReport_undoneInItsWindow_bringsTheMessageBack() {
        report()
        composeTestRule.onNodeWithText(FakeSeaRepository.SEEDED_MESSAGE).assertDoesNotExist()

        composeTestRule.onNodeWithText(string(R.string.reveal_undo)).performClick()

        composeTestRule.onNodeWithText(FakeSeaRepository.SEEDED_MESSAGE).assertIsDisplayed()
        Assert.assertNull(seaActions.pending.value)
        Assert.assertTrue(seaActions.committed.isEmpty())
    }

    @Test
    fun aReport_cancelled_leavesTheMessage() {
        composeTestRule.onNodeWithContentDescription(string(R.string.reveal_report_description)).performClick()

        composeTestRule.onNodeWithText(string(R.string.report_cancel)).performClick()

        composeTestRule.onNodeWithText(string(R.string.report_title)).assertDoesNotExist()
        composeTestRule.onNodeWithText(FakeSeaRepository.SEEDED_MESSAGE).assertIsDisplayed()
        Assert.assertNull(seaActions.pending.value)
    }

    @Test
    fun throwingBack_waitsOutItsWindow_fromTheShore() {
        composeTestRule.onNode(hasText(string(R.string.reveal_throw_back)) and hasClickAction()).performClick()

        Assert.assertEquals(1, leaves)
        Assert.assertTrue(seaActions.pending.value is PendingSeaAction.ThrowBack)
    }

    // no reason, no block: both are optional (spec §3.19)
    private fun report() {
        composeTestRule.onNodeWithContentDescription(string(R.string.reveal_report_description)).performClick()
        composeTestRule.onNode(hasText(string(R.string.report_confirm)) and hasClickAction()).performClick()
        composeTestRule.onNodeWithText(string(R.string.reveal_reported_hidden)).assertIsDisplayed()
    }

    private fun string(@StringRes resId: Int): String = composeTestRule.activity.getString(resId)
}
