package com.bottlecast.app.common.components

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.testing.captureScreenByDrawing
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The dialog draws the theme's own scrim, not the platform's black dim: black at 60% on Night,
 * #1A2320 at 40% on Dawn (spec §1.2).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class BottlingDialogScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun scrimNight() = capture(darkTheme = true)

    @Test
    fun scrimDawn() = capture(darkTheme = false)

    private fun capture(darkTheme: Boolean) {
        composeTestRule.setContent {
            BottlingTheme(darkTheme = darkTheme) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(BottlingTheme.spacing.l)
                ) {
                    Text(text = "The screen behind the dialog", style = MaterialTheme.typography.headlineMedium)
                }
                BottlingDialog(onDismissRequest = {}) {
                    Text(text = "A dialog", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "It sits on surfaceHigh with 32dp corners, over the theme's scrim.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.captureScreenByDrawing()
    }
}
