package com.bottlecast.app.common.ui

import android.app.Application
import androidx.compose.material3.LocalTonalElevationEnabled
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Checks what [BottlingTheme] provides to the composition.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], application = Application::class)
class BottlingThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `theme uses the expressive motion scheme`() {
        // given
        var motionScheme: MotionScheme? = null

        // when
        composeTestRule.setContent {
            BottlingTheme(darkTheme = true) { motionScheme = MaterialTheme.motionScheme }
        }

        // then
        Assert.assertSame(MotionScheme.expressive(), motionScheme)
    }

    @Test
    fun `tonal elevation is off`() {
        // given
        var tonalElevationEnabled: Boolean? = null

        // when
        composeTestRule.setContent {
            BottlingTheme(darkTheme = true) { tonalElevationEnabled = LocalTonalElevationEnabled.current }
        }

        // then
        Assert.assertEquals(false, tonalElevationEnabled)
    }

    @Test
    fun `night and dawn provide their own colours`() {
        // given
        var nightBackground: Color? = null
        var dawnBackground: Color? = null
        var nightColors: BottlingColors? = null
        var dawnColors: BottlingColors? = null

        // when
        composeTestRule.setContent {
            BottlingTheme(darkTheme = true) {
                nightBackground = MaterialTheme.colorScheme.background
                nightColors = BottlingTheme.colors
            }
            BottlingTheme(darkTheme = false) {
                dawnBackground = MaterialTheme.colorScheme.background
                dawnColors = BottlingTheme.colors
            }
        }

        // then
        Assert.assertEquals(NightColorScheme.background, nightBackground)
        Assert.assertEquals(DawnColorScheme.background, dawnBackground)
        Assert.assertSame(NightBottlingColors, nightColors)
        Assert.assertSame(DawnBottlingColors, dawnColors)
    }
}
