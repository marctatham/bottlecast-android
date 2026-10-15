package com.bottlecast.app.feature.shore

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bottlecast.app.R
import com.bottlecast.app.common.components.BottlingSnackbarHost
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.fakes.FakeAuthenticationRepository
import com.bottlecast.app.fakes.FakeConnectivityMonitor
import com.bottlecast.app.fakes.FakeChestRepository
import com.bottlecast.app.fakes.FakeSeaActions
import com.bottlecast.app.fakes.FakeStreakRepository
import com.bottlecast.app.feature.shore.domain.ShoreNotices
import com.bottlecast.app.fakes.FakeSeaRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock

/**
 * UI tests for [ShoreScreen], with its ViewModel built from fakes.
 */
@RunWith(AndroidJUnit4::class)
class ShoreScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val authenticationRepository = FakeAuthenticationRepository()
    private val connectivityMonitor = FakeConnectivityMonitor()
    private val seaRepository = FakeSeaRepository(authenticationRepository)
    private val seaActions = FakeSeaActions(seaRepository)
    private var bottlesFound = 0
    private var moderationOpened = 0
    private var findsOpened = mutableListOf<String>()

    @Test
    fun shore_offersTodaysBottle_andCasting() {
        showShore()

        composeTestRule.onNodeWithText(string(R.string.shore_waiting_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.shore_cast)).assertIsDisplayed()

        composeTestRule.onNodeWithText(string(R.string.shore_find_bottle)).assertIsEnabled().performClick()

        Assert.assertEquals(1, bottlesFound)
    }

    @Test
    fun offline_showsTheBanner_andHoldsTodaysBottle() {
        showShore()

        connectivityMonitor.isOnline.value = false

        composeTestRule.onNodeWithText(string(R.string.shore_offline_banner)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.shore_find_bottle)).assertIsNotEnabled()
    }

    @Test
    fun admin_opensModeration_fromTheMenu() {
        showShore()
        composeTestRule.onNodeWithContentDescription(string(R.string.shore_more_options)).assertDoesNotExist()

        authenticationRepository.signInAsAdmin()
        composeTestRule.onNodeWithContentDescription(string(R.string.shore_more_options)).performClick()
        composeTestRule.onNodeWithText(string(R.string.shore_moderation)).performClick()

        Assert.assertEquals(1, moderationOpened)
    }

    @Test
    fun calmSea_offersCastingInstead() {
        seaRepository.seaCalm = true
        showShore()

        composeTestRule.waitUntil { composeTestRule.onAllNodes(hasText(string(R.string.sea_calm_title))).fetchSemanticsNodes().isNotEmpty() }
        composeTestRule.onNodeWithText(string(R.string.shore_find_bottle)).assertDoesNotExist()
    }

    @Test
    fun todaysFind_reopensFromItsCard() {
        runBlocking { seaRepository.findTodaysBottle() }
        showShore()

        composeTestRule.onNodeWithText(string(R.string.shore_found_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(excerpt()).performClick()

        Assert.assertEquals(listOf(FakeSeaRepository.SEEDED_ID), findsOpened)
    }

    @Test
    fun throwBack_hidesTheCard_untilItsUndone() {
        runBlocking { seaRepository.findTodaysBottle() }
        showShore()

        seaActions.throwBack(FakeSeaRepository.SEEDED, windowMillis = null)

        composeTestRule.onNodeWithText(excerpt()).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.shore_undo)).performClick()

        composeTestRule.onNodeWithText(excerpt()).assertIsDisplayed()
        Assert.assertTrue(seaActions.committed.isEmpty())
    }

    @Test
    fun throwBack_oncePastItsWindow_leavesNoCard() {
        runBlocking { seaRepository.findTodaysBottle() }
        showShore()

        seaActions.throwBack(FakeSeaRepository.SEEDED, windowMillis = null)
        composeTestRule.onNodeWithText(string(R.string.shore_thrown_back)).assertIsDisplayed()
        seaActions.expireAll()

        composeTestRule.waitUntil { composeTestRule.onAllNodes(hasText(string(R.string.shore_thrown_back))).fetchSemanticsNodes().isEmpty() }
        composeTestRule.onNodeWithText(excerpt()).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.shore_found_title)).assertIsDisplayed()
    }

    private fun showShore() {
        composeTestRule.setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            BottlingTheme {
                Box {
                    ShoreScreen(
                        onFindBottleHandler = { bottlesFound++ },
                        onCastHandler = {},
                        onModerationHandler = { moderationOpened++ },
                        onOpenChestHandler = {},
                        onOpenMeHandler = {},
                        onOpenTodaysFindHandler = { findsOpened += it },
                        onSeeAwayFindsHandler = {},
                        contentPadding = PaddingValues(),
                        snackbarHostState = snackbarHostState,
                        viewModel = viewModel {
                            ShoreViewModel(
                                authenticationRepository,
                                connectivityMonitor,
                                seaRepository,
                                seaActions,
                                FakeStreakRepository(),
                                FakeChestRepository(seaRepository, authenticationRepository),
                                ShoreNotices(),
                                Clock.systemDefaultZone(),
                            )
                        },
                    )
                    BottlingSnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
        }
    }

    private fun excerpt(): String = composeTestRule.activity.getString(R.string.shore_find_excerpt, FakeSeaRepository.SEEDED_MESSAGE)

    private fun string(@StringRes resId: Int): String = composeTestRule.activity.getString(resId)
}
