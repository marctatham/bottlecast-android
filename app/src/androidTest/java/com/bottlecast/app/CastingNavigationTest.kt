package com.bottlecast.app

import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bottlecast.app.common.analytics.AnalyticsEvent
import com.bottlecast.app.fakes.FakeAuthenticationRepository
import com.bottlecast.app.fakes.FakeCastCoordinator
import com.bottlecast.app.fakes.FakeConnectivityMonitor
import com.bottlecast.app.fakes.FakeDraftRepository
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

// the cast plays for about 2.4s before its words come, and a cold emulator is slow
private const val CAST_TIMEOUT_MILLIS = 10_000L

// the Terms checkbox sits at the start of its row, in a 48dp touch target
private val CHECKBOX_CENTRE = 24.dp

private const val MESSAGE = "If you're reading this, drink some water and text someone you miss."

/**
 * Casting (spec §2, flow 3): the sign-in sheet for a guest, then Compose, Preview and the cast's
 * success, back to the Shore, with the fakes the testdi modules install.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CastingNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var tracker: FakeTracker

    @Inject
    lateinit var authenticationRepository: FakeAuthenticationRepository

    @Inject
    lateinit var castCoordinator: FakeCastCoordinator

    @Inject
    lateinit var drafts: FakeDraftRepository

    @Inject
    lateinit var connectivityMonitor: FakeConnectivityMonitor

    @Inject
    lateinit var seaRepository: FakeSeaRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun notNow_closesTheSheet_andLeavesTheShore() {
        tapCast()
        composeTestRule.onNodeWithText(string(R.string.sign_in_title_cast)).assertIsDisplayed()

        composeTestRule.onNodeWithText(string(R.string.sign_in_not_now)).performClick()

        composeTestRule.waitUntil(conditionDescription = "the sheet has gone") {
            composeTestRule.onAllNodesWithText(string(R.string.sign_in_title_cast)).fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithText(string(R.string.shore_waiting_title)).assertIsDisplayed()
        Assert.assertEquals(listOf(Shore.screenName), tracker.screens)
    }

    // a first cast: Compose, Preview with the Terms, the cast, then back to the Shore (spec §2, flow 3)
    @Test
    fun aGoogleCast_goesThroughPreviewAndTheTerms_toSuccess_thenBackToTheShore() {
        authenticationRepository.signInAsGoogle()
        writeAndPreview()

        tickTheTerms()
        composeTestRule.onNodeWithText(string(R.string.preview_cast)).performClick()

        waitForText(string(R.string.cast_success_back))
        composeTestRule.onNodeWithText(string(R.string.cast_success_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.cast_success_back)).performClick()

        composeTestRule.onNodeWithText(string(R.string.shore_waiting_title)).assertIsDisplayed()
        Assert.assertEquals(
            listOf(Shore.screenName, Compose.SCREEN_NAME, ComposePreview.screenName, CastSuccess.SCREEN_NAME, Shore.screenName),
            tracker.screens,
        )
        val cast = castCoordinator.casts.single()
        Assert.assertEquals(MESSAGE, cast.text)
        Assert.assertTrue(cast.acceptTerms)
        Assert.assertTrue(cast.sealable)
    }

    // Compose and Preview leave the stack, so system back from success reaches the Shore (spec §2)
    @Test
    fun backFromSuccess_reachesTheShore_notCompose() {
        authenticationRepository.signInAsGoogle()
        writeAndPreview()
        tickTheTerms()
        composeTestRule.onNodeWithText(string(R.string.preview_cast)).performClick()
        waitForText(string(R.string.cast_success_back))

        Espresso.pressBack()

        composeTestRule.onNodeWithText(string(R.string.shore_waiting_title)).assertIsDisplayed()
        // the Shore's own pill reads "Cast a bottle" too, so look for Compose's editor
        editor().assertDoesNotExist()
        Assert.assertEquals(Shore.screenName, tracker.screens.last())
    }

    @Test
    fun anOfflineCast_isCorked_untilTheServerHasIt() {
        authenticationRepository.signInAsGoogle()
        connectivityMonitor.isOnline.value = false
        writeAndPreview()
        tickTheTerms()

        composeTestRule.onNodeWithText(string(R.string.preview_cast_offline)).performClick()

        waitForText(string(R.string.cast_success_back))
        composeTestRule.onNodeWithText(string(R.string.cast_success_queued_title)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(string(R.string.journey_now, string(R.string.journey_waiting))).assertExists()

        castCoordinator.confirmQueued()
        waitForText(string(R.string.cast_success_title))
    }

    @Test
    fun aRefusedCast_keepsItsWords_andOffersToTryAgain() {
        authenticationRepository.signInAsGoogle()
        castCoordinator.refuseNext = true
        writeAndPreview()
        tickTheTerms()

        composeTestRule.onNodeWithText(string(R.string.preview_cast)).performClick()

        waitForText(string(R.string.preview_cast_failed))
        // the top bar has scrolled away with the page, but Cast it stays pinned
        composeTestRule.onNodeWithText(string(R.string.preview_cast)).assertIsDisplayed()
        Assert.assertEquals(ComposePreview.screenName, tracker.screens.last())
        Assert.assertEquals(MESSAGE, drafts.draft.value?.text)
    }

    @Test
    fun closingWithWordsWritten_asksFirst_andTossingClearsThem() {
        authenticationRepository.signInAsGoogle()
        tapCast()
        editor().performTextInput(MESSAGE)

        composeTestRule.onNodeWithContentDescription(string(R.string.button_close)).performClick()
        composeTestRule.onNodeWithText(string(R.string.compose_discard_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.compose_discard_toss)).performClick()

        composeTestRule.onNodeWithText(string(R.string.shore_waiting_title)).assertIsDisplayed()
        Assert.assertNull(drafts.draft.value)
    }

    // a guest holding today's find signs in to an account that already exists, and the find comes along
    @Test
    fun aGuestWithTodaysFind_bringsItToAnExistingAccount() {
        seaRepository.foundElsewhere(FakeSeaRepository.SEEDED_ID)
        authenticationRepository.signInAsGuest()
        authenticationRepository.googleAccountExists = true

        tapCast()
        composeTestRule.onNodeWithText(string(R.string.sign_in_carry_find)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.sign_in_google)).performClick()

        composeTestRule.waitUntil(conditionDescription = "Compose is shown", timeoutMillis = CAST_TIMEOUT_MILLIS) {
            tracker.screens.lastOrNull() == Compose.SCREEN_NAME
        }
        Assert.assertEquals(1, seaRepository.handovers)
        Assert.assertEquals(FakeAuthenticationRepository.GOOGLE_USER_ID, authenticationRepository.signedInUid())
        Assert.assertTrue(AnalyticsEvent.SignedIn in tracker.events)
    }

    private fun tapCast() {
        // the calm sea's cast pill, or the one beside today's bottle
        composeTestRule.onNode(hasText(string(R.string.shore_cast))).performClick()
    }

    private fun writeAndPreview() {
        tapCast()
        editor().performTextInput(MESSAGE)
        composeTestRule.onNodeWithText(string(R.string.compose_preview)).performClick()
        waitForText(string(R.string.preview_overline).uppercase())
    }

    // the row's middle can land on one of its links, which opens a Custom Tab, so tap the box itself
    private fun tickTheTerms() {
        composeTestRule.onNodeWithText(string(R.string.preview_terms_first_cast))
            .performScrollTo()
            .performTouchInput { click(Offset(CHECKBOX_CENTRE.toPx(), centerY)) }
    }

    private fun editor() = composeTestRule.onNodeWithContentDescription(string(R.string.compose_editor_label))

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(conditionDescription = "\"$text\" is shown", timeoutMillis = CAST_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun string(@StringRes resId: Int): String = composeTestRule.activity.getString(resId)

    private fun string(@StringRes resId: Int, vararg args: Any): String = composeTestRule.activity.getString(resId, *args)
}
