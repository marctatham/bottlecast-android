package com.bottlecast.app.common.components

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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

    @Test
    fun smallerPillsNight() = captureSmaller(darkTheme = true)

    @Test
    fun smallerPillsDawn() = captureSmaller(darkTheme = false)

    // Slice 2's additions: the report's destructive pill, Reveal's 48dp pair and the text button
    private fun captureSmaller(darkTheme: Boolean) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(BottlingTheme.spacing.gutter)
                    .width(CONTENT_WIDTH),
                verticalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xs),
            ) {
                BottlingButton(text = "Report", buttonType = BottlingButtonType.DESTRUCTIVE)
                Row(horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xs)) {
                    BottlingButton(
                        text = "Share",
                        buttonType = BottlingButtonType.TONAL,
                        size = BottlingButtonSize.MEDIUM,
                        leadingIcon = R.drawable.ic_symbol_share,
                        modifier = Modifier.weight(1f),
                    )
                    BottlingButton(
                        text = "Throw back",
                        buttonType = BottlingButtonType.TONAL,
                        size = BottlingButtonSize.MEDIUM,
                        leadingIcon = R.drawable.ic_symbol_waves,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xs)) {
                    BottlingTextButton(text = R.string.button_back, onTapHandler = {})
                    BottlingButton(text = "Report", buttonType = BottlingButtonType.DESTRUCTIVE, size = BottlingButtonSize.MEDIUM)
                }
            }
        }
    }

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
