package com.bottlecast.app

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bottlecast.app.common.analytics.AnalyticsEvent
import com.bottlecast.app.common.authentication.domain.googleUid
import com.bottlecast.app.common.profile.domain.UserProfile
import com.bottlecast.app.common.settings.domain.ThemeChoice
import com.bottlecast.app.fakes.FakeAccountEraser
import com.bottlecast.app.fakes.FakeAuthenticationRepository
import com.bottlecast.app.fakes.FakeGoogleCredentials
import com.bottlecast.app.fakes.FakeOnboardingStore
import com.bottlecast.app.fakes.FakeProfileRepository
import com.bottlecast.app.fakes.FakeSettingsStore
import com.bottlecast.app.fakes.FakeThemeApplier
import com.bottlecast.app.fakes.FakeTracker
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

private const val SETTINGS_TIMEOUT_MILLIS = 10_000L

/** The Me tab and Settings (spec §3.12, §3.17): opening Settings, its theme, signing out and deleting the account. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MeSettingsNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var tracker: FakeTracker

    @Inject
    lateinit var authenticationRepository: FakeAuthenticationRepository

    @Inject
    lateinit var profileRepository: FakeProfileRepository

    @Inject
    lateinit var credentials: FakeGoogleCredentials

    @Inject
    lateinit var eraser: FakeAccountEraser

    @Inject
    lateinit var settingsStore: FakeSettingsStore

    @Inject
    lateinit var themeApplier: FakeThemeApplier

    @Inject
    lateinit var onboardingStore: FakeOnboardingStore

    @Before
    fun setUp() {
        hiltRule.inject()
        authenticationRepository.signInAsGoogle()
        profileRepository.current.value = UserProfile(penName = "Driftwood")
    }

    @Test
    fun theMeTab_isHeadedByThePenName_andItsGearOpensSettings_andBackReturns() {
        openMe()
        composeTestRule.onNodeWithText("Driftwood").performScrollTo()

        composeTestRule.onNodeWithContentDescription(string(R.string.me_settings)).performClick()
        waitForText(string(R.string.settings_title))
        Assert.assertEquals(Settings.screenName, tracker.screens.last())

        Espresso.pressBack()
        waitForText("Driftwood")
        Assert.assertEquals(Me.screenName, tracker.screens.last())
    }

    @Test
    fun choosingATheme_savesItAndAppliesIt() {
        openSettings()

        composeTestRule.onNodeWithText(string(R.string.settings_theme_dawn)).performScrollTo().performClick()

        composeTestRule.waitUntil(SETTINGS_TIMEOUT_MILLIS) { themeApplier.applied == ThemeChoice.Dawn }
        Assert.assertEquals(ThemeChoice.Dawn, settingsStore.settings.value.theme)
    }

    @Test
    fun signingOut_goesBackToAGuest() {
        openSettings()

        composeTestRule.onNodeWithText(string(R.string.settings_sign_out)).performScrollTo().performClick()
        inDialog(string(R.string.settings_sign_out_confirm)).performClick()

        waitForText(string(R.string.settings_signed_out))
        composeTestRule.onNodeWithText(string(R.string.sign_in_google)).performScrollTo()
        // a guest again: the Shore underneath makes a silent guest as soon as it looks at the sea
        Assert.assertNull(authenticationRepository.user.value.googleUid)
        Assert.assertEquals(1, credentials.forgotten)
        Assert.assertTrue(AnalyticsEvent.SignedOut in tracker.events)
    }

    @Test
    fun deletingTheAccount_waitsForTheTick_thenSaysGoodbye_andReturnsToTheShore() {
        openSettings()
        val uid = authenticationRepository.signedInUid()

        composeTestRule.onNodeWithText(string(R.string.settings_delete_account)).performScrollTo().performClick()
        composeTestRule.onNodeWithText(string(R.string.settings_delete_title)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.settings_delete_understand)).performClick()
        inDialog(string(R.string.settings_delete_confirm)).performClick()

        waitForText(string(R.string.goodbye_title))
        Assert.assertEquals(listOf(uid), eraser.erased.toList())
        Assert.assertTrue(authenticationRepository.deleted)
        Assert.assertTrue(AnalyticsEvent.AccountDeleted in tracker.events)

        composeTestRule.onNodeWithText(string(R.string.goodbye_back)).performClick()
        waitForText(string(R.string.shore_find_bottle))
        Assert.assertEquals(Shore.screenName, tracker.screens.last())
    }

    @Test
    fun howItWorks_replaysOnboarding_andGoesBackToSettings() {
        openSettings()

        composeTestRule.onNodeWithText(string(R.string.settings_how_it_works)).performScrollTo().performClick()
        waitForText(string(R.string.onboarding_skip))
        Assert.assertEquals(HowItWorks.screenName, tracker.screens.last())
        composeTestRule.onNodeWithText(string(R.string.onboarding_skip)).performClick()

        waitForText(string(R.string.settings_title))
        Assert.assertEquals(Settings.screenName, tracker.screens.last())
        Assert.assertFalse(AnalyticsEvent.OnboardingFinished in tracker.events)
        Assert.assertTrue(onboardingStore.isSeen)
    }

    private fun openMe() {
        composeTestRule.onNodeWithText(string(R.string.nav_me)).performClick()
        waitForText("Driftwood")
    }

    private fun openSettings() {
        openMe()
        composeTestRule.onNodeWithContentDescription(string(R.string.me_settings)).performClick()
        waitForText(string(R.string.settings_title))
    }

    // a dialog's button, rather than the row with the same words behind it
    private fun inDialog(text: String): SemanticsNodeInteraction =
        composeTestRule.onNode(hasText(text) and hasAnyAncestor(isDialog()))

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(conditionDescription = "\"$text\" is shown", timeoutMillis = SETTINGS_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun string(@StringRes resId: Int): String = composeTestRule.activity.getString(resId)
}
