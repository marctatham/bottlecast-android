package com.bottlecast.app.common.components

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bottlecast.app.TopLevelDestination
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.testing.captureThemed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Shore/Chest/Me bar (spec §2) with each tab selected, then the Chest's badge dot, at the
 * boards' 390dp width and with no system navigation bar beneath, as the boards draw it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class BottlingNavigationBarScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tabsNight() = capture(darkTheme = true)

    @Test
    fun tabsDawn() = capture(darkTheme = false)

    private fun capture(darkTheme: Boolean) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.m),
            ) {
                TopLevelDestination.entries.forEach { selected ->
                    BottlingNavigationBar(selected = selected, onSelect = {}, windowInsets = WindowInsets(0))
                }
                BottlingNavigationBar(
                    selected = TopLevelDestination.SHORE,
                    onSelect = {},
                    chestBadge = true,
                    windowInsets = WindowInsets(0),
                )
            }
        }
    }
}
