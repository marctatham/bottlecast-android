package com.bottlecast.app.feature.shore

import android.app.Application
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.test.core.app.ApplicationProvider
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.BreathingTide
import com.bottlecast.app.feature.shore.domain.Tide
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration

/** State A's bottle shows while today's bottle waits, and steps aside when there's no room for it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class ShoreBottleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `the bottle is drawn while today's bottle waits`() {
        // given
        // when
        setShore(ShoreUiState.BottleWaiting())

        // then
        Assert.assertTrue(bottleShown())
    }

    @Test
    @Config(fontScale = 2f)
    fun `the bottle steps aside for large text`() {
        // given
        // when
        setShore(ShoreUiState.BottleWaiting())

        // then
        Assert.assertFalse(bottleShown())
    }

    @Test
    @Config(qualifiers = "w844dp-h390dp-xhdpi")
    fun `the bottle steps aside when there's no room for it`() {
        // given
        // when
        setShore(ShoreUiState.BottleWaiting())

        // then
        Assert.assertFalse(bottleShown())
    }

    @Test
    fun `there's no bottle on a calm sea`() {
        // given
        // when
        setShore(ShoreUiState.BottleWaiting(seaCalm = true))

        // then
        Assert.assertFalse(bottleShown())
    }

    @Test
    fun `there's no bottle once today's has been found`() {
        // given
        // when
        setShore(
            ShoreUiState.FoundToday(
                tide = Tide(untilNextTide = Duration.ofHours(7), dayElapsed = 0.7f),
                todaysFind = TodaysFind(messageId = "today", excerpt = "Hey, that's pretty cool"),
                reminderTime = null,
            )
        )

        // then
        Assert.assertFalse(bottleShown())
    }

    private fun bottleShown(): Boolean {
        val description = ApplicationProvider.getApplicationContext<Application>().getString(R.string.shore_bottle_description)
        return composeTestRule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
    }

    private fun setShore(uiState: ShoreUiState) {
        composeTestRule.setContent {
            BottlingTheme {
                ShoreScreen(
                    uiState = uiState,
                    tide = BreathingTide.Still,
                    onFindBottleHandler = {},
                    onCastHandler = {},
                    onModerationHandler = {},
                    onOpenChestHandler = {},
                    onOpenMeHandler = {},
                    onOpenTodaysFindHandler = {},
                    onReminderHandler = {},
                    contentPadding = PaddingValues(),
                )
            }
        }
        composeTestRule.waitForIdle()
    }
}
