package com.bottlecast.app.feature.me

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bottlecast.app.TopLevelDestination
import com.bottlecast.app.testing.BoardStatusBar
import com.bottlecast.app.testing.BoardTabFrame
import com.bottlecast.app.testing.captureThemed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Me tab until slice 4 fills it, in its tab frame at the boards' 390×844 with a 44dp
 * status bar: PNG pixels ÷ 2 = board pixels.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class MeScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyNight() = capture(darkTheme = true)

    @Test
    fun emptyDawn() = capture(darkTheme = false)

    private fun capture(darkTheme: Boolean) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            BoardTabFrame(selected = TopLevelDestination.ME) { contentPadding ->
                MeScreen(onGoToShoreHandler = {}, contentPadding = contentPadding, windowInsets = BoardStatusBar)
            }
        }
    }
}
