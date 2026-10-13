package com.bottlecast.app.common.components

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.testing.captureThemed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The pills (spec §1.4): primary with its leading glyph, outlined and tonal, enabled then disabled,
 * at the boards' 358dp content width.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class BottlingButtonScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun pillsNight() = capture(darkTheme = true)

    @Test
    fun pillsDawn() = capture(darkTheme = false)

    private fun capture(darkTheme: Boolean) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(BottlingTheme.spacing.gutter)
                    .width(CONTENT_WIDTH),
                verticalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xs),
            ) {
                listOf(true, false).forEach { enabled ->
                    BottlingButton(
                        text = "Find today's bottle",
                        buttonType = BottlingButtonType.PRIMARY,
                        leadingIcon = R.drawable.ic_glyph_bottle,
                        enabled = enabled,
                    )
                    BottlingButton(text = "Cast a bottle", buttonType = BottlingButtonType.SECONDARY, enabled = enabled)
                    BottlingButton(text = "Keep it", buttonType = BottlingButtonType.TONAL, enabled = enabled)
                }
            }
        }
    }

    private companion object {
        val CONTENT_WIDTH = 358.dp
    }
}
