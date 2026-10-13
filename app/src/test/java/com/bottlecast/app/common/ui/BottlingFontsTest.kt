package com.bottlecast.app.common.ui

import android.app.Application
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Guards the bundled fonts under Robolectric, which renders the screenshot tests: Roboto Flex is a
 * variable font, so its weights and optical sizes only show if the variation axes are applied.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class)
class BottlingFontsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val resolver = createFontFamilyResolver(context)

    @Test
    fun `roboto flex applies the weight axis`() {
        // given
        val flex = robotoFlex(14.sp)

        // when
        val regular = widthOf(flex, FontWeight.Normal)
        val medium = widthOf(flex, FontWeight.Medium)
        val semiBold = widthOf(flex, FontWeight.SemiBold)

        // then
        Assert.assertTrue("medium ($medium) should be wider than regular ($regular)", medium > regular)
        Assert.assertTrue("semibold ($semiBold) should be wider than medium ($medium)", semiBold > medium)
    }

    @Test
    fun `roboto flex applies the optical size axis`() {
        // when
        val small = widthOf(robotoFlex(11.sp), FontWeight.Normal)
        val large = widthOf(robotoFlex(16.sp), FontWeight.Normal)

        // then
        Assert.assertNotEquals("opsz 11 and 16 should measure differently", small, large, 0.01f)
    }

    @Test
    fun `bundled families render instead of the system font`() {
        // given
        val system = widthOf(FontFamily.Default, FontWeight.Normal)

        // then
        Assert.assertNotEquals(system, widthOf(RobotoSlab, FontWeight.Normal), 0.01f)
        Assert.assertNotEquals(system, widthOf(robotoFlex(14.sp), FontWeight.Normal), 0.01f)
        Assert.assertNotEquals(system, widthOf(RobotoMono, FontWeight.Normal), 0.01f)
    }

    private fun widthOf(family: FontFamily, weight: FontWeight): Float {
        val typeface = resolver.resolve(family, weight).value as Typeface
        return Paint().apply {
            this.typeface = typeface
            textSize = TEXT_SIZE_PX
        }.measureText(SAMPLE)
    }

    private companion object {
        const val SAMPLE = "A bottle washed ashore, waiting for you."
        const val TEXT_SIZE_PX = 100f
    }
}
