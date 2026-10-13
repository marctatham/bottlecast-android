package com.bottlecast.app.feature.getmessage.components

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.testing.captureThemed
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests for [UnbottledMessageCard]; goldens live in app/src/test/screenshots (see CLAUDE.md).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel9)
class UnbottledMessageCardScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun unreportedNight() = capture(isReported = false, darkTheme = true)

    @Test
    fun unreportedDawn() = capture(isReported = false, darkTheme = false)

    @Test
    fun reportedNight() = capture(isReported = true, darkTheme = true)

    @Test
    fun reportedDawn() = capture(isReported = true, darkTheme = false)

    // the plain Application keeps Hilt out
    private fun capture(isReported: Boolean, darkTheme: Boolean) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            Box(Modifier.background(MaterialTheme.colorScheme.background).padding(BottlingTheme.spacing.l)) {
                UnbottledMessageCard(
                    isReported = isReported,
                    onReportHandler = {},
                    unbottledMessage = "Hey, that's pretty cool",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
