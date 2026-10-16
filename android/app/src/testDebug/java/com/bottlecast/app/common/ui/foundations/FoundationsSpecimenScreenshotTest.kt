package com.bottlecast.app.common.ui.foundations

import android.app.Application
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.testing.captureByDrawing
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The look sign-off: [FoundationsSpecimen] at the Foundations-DesignSystem board's 1440×1000 frame
 * (PNG pixels ÷ 2 = board pixels), in Night and Dawn. Debug only, like the specimen.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w1440dp-h1000dp-xhdpi")
class FoundationsSpecimenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun foundationsNight() = capture(darkTheme = true)

    @Test
    fun foundationsDawn() = capture(darkTheme = false)

    // the focus ring animates in, so the clock is paused and advanced past it before capturing
    private fun capture(darkTheme: Boolean) {
        val focus = MutableInteractionSource()
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            BottlingTheme(darkTheme = darkTheme) { FoundationsSpecimen(focusInteractionSource = focus) }
        }
        composeTestRule.runOnIdle { focus.tryEmit(FocusInteraction.Focus()) }
        composeTestRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        composeTestRule.onRoot().captureByDrawing()
    }

    private companion object {
        const val SETTLE_MILLIS = 1_000L
    }
}
