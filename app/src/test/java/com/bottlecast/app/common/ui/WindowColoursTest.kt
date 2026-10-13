package com.bottlecast.app.common.ui

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.bottlecast.app.R
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The window and splash colours live in XML, so they're drawn before Compose. They must match the
 * Kotlin palette in both themes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], application = Application::class)
class WindowColoursTest {

    @Test
    @Config(qualifiers = "notnight")
    fun `dawn window and splash colours match the palette`() {
        // given
        val expected = listOf(DawnColorScheme.background, DawnBottlingColors.dome, DawnBottlingColors.glass)

        // when
        val actual = windowColours()

        // then
        Assert.assertEquals(expected, actual)
    }

    @Test
    @Config(qualifiers = "night")
    fun `night window and splash colours match the palette`() {
        // given
        val expected = listOf(NightColorScheme.background, NightBottlingColors.dome, NightBottlingColors.glass)

        // when
        val actual = windowColours()

        // then
        Assert.assertEquals(expected, actual)
    }

    private fun windowColours(): List<Color> {
        val context: Context = ApplicationProvider.getApplicationContext()
        return listOf(R.color.window_background, R.color.splash_background, R.color.splash_glyph)
            .map { id -> Color(context.getColor(id)) }
    }
}
