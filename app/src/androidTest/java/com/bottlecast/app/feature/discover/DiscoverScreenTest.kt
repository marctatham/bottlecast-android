package com.bottlecast.app.feature.discover

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.fakes.FakeAuthenticationRepository
import com.bottlecast.app.fakes.FakeConnectivityMonitor
import com.bottlecast.app.fakes.FakeSeaRepository
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import kotlin.time.TimeSource

// a hold fills the ring in 1.2s of real time, then the pop plays out
private const val HOLD_TIMEOUT_MILLIS = 10_000L

/**
 * UI tests for [DiscoverScreen], with its ViewModel built from fakes. A hold runs on the phone's own
 * clock, so these press and wait rather than inject a long press.
 */
@RunWith(AndroidJUnit4::class)
class DiscoverScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val authenticationRepository = FakeAuthenticationRepository()
    private val connectivityMonitor = FakeConnectivityMonitor()
    private val seaRepository = FakeSeaRepository(authenticationRepository)
    private val revealed = mutableListOf<String>()

    @Before
    fun setUp() {
        composeTestRule.setContent {
            BottlingTheme {
                DiscoverScreen(
                    onBackHandler = {},
                    onRevealHandler = { revealed += it },
                    onAlreadyFoundHandler = {},
                    onCastHandler = {},
                    viewModel = viewModel {
                        DiscoverViewModel(
                            seaRepository,
                            connectivityMonitor,
                            Clock.systemDefaultZone(),
                            TimeSource.Monotonic,
                            createSavedStateHandle(),
                        )
                    },
                )
            }
        }
    }

    // holding the bottle fills the ring, and the cork pops when it's full (spec §3.6)
    @Test
    fun holdingTheBottle_popsTheCork() {
        bottle().performTouchInput { down(center) }

        composeTestRule.waitUntil(conditionDescription = "the held bottle opens", timeoutMillis = HOLD_TIMEOUT_MILLIS) {
            revealed.isNotEmpty()
        }
        bottle().performTouchInput { up() }

        Assert.assertEquals(listOf(FakeSeaRepository.SEEDED_ID), revealed)
        Assert.assertEquals(listOf(FakeSeaRepository.SEEDED_ID), seaRepository.finds)
    }

    // a press shorter than a hold is a tap: a third of the ring, and nothing found yet
    @Test
    fun aShortPress_loosensTheCorkAThird() {
        bottle().performClick()

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.discover_progress, 1)).assertIsDisplayed()
        Assert.assertTrue(revealed.isEmpty())
        Assert.assertTrue(seaRepository.finds.isEmpty())
    }

    private fun bottle() = composeTestRule.onNodeWithContentDescription(composeTestRule.activity.getString(R.string.discover_bottle))
}
