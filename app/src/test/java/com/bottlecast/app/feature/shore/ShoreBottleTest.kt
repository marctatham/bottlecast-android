package com.bottlecast.app.feature.shore

import android.app.Application
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.v2.createComposeRule
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

/** The Shore reports whether it draws the bottle, so the bottle only animates while it can be seen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class ShoreBottleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var bottleShown: Boolean? = null

    @Test
    fun `the bottle is drawn while today's bottle waits`() {
        // given
        // when
        setShore(ShoreUiState.BottleWaiting())

        // then
        Assert.assertEquals(true, bottleShown)
    }

    @Test
    @Config(fontScale = 2f)
    fun `the bottle steps aside for large text`() {
        // given
        // when
        setShore(ShoreUiState.BottleWaiting())

        // then
        Assert.assertEquals(false, bottleShown)
    }

    @Test
    @Config(qualifiers = "w844dp-h390dp-xhdpi")
    fun `the bottle steps aside when there's no room for it`() {
        // given
        // when
        setShore(ShoreUiState.BottleWaiting())

        // then
        Assert.assertEquals(false, bottleShown)
    }

    @Test
    fun `there's no bottle once today's has been found`() {
        // given
        // when
        setShore(
            ShoreUiState.FoundToday(
                tide = Tide(untilNextTide = Duration.ofHours(7), dayElapsed = 0.7f),
                todaysFind = TodaysFind(excerpt = "Hey, that's pretty cool", isKept = true, daysLeft = 7),
                reminderTime = null,
            )
        )

        // then
        Assert.assertNotEquals(true, bottleShown)
    }

    private fun setShore(uiState: ShoreUiState) {
        composeTestRule.setContent {
            BottlingTheme {
                ShoreScreen(
                    uiState = uiState,
                    tide = BreathingTide.Still,
                    composition = null,
                    progress = { 0f },
                    onFindBottleHandler = {},
                    onCastHandler = {},
                    onModerationHandler = {},
                    onOpenChestHandler = {},
                    onOpenMeHandler = {},
                    onOpenTodaysFindHandler = {},
                    onReminderHandler = {},
                    contentPadding = PaddingValues(),
                    onBottleShown = { shown -> bottleShown = shown },
                )
            }
        }
        composeTestRule.waitForIdle()
    }
}
