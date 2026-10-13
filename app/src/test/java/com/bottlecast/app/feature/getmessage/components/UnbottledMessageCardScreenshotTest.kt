package com.bottlecast.app.feature.getmessage.components

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.ui.BottlingTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
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

    @Test
    fun unreportedLight() = capture(isReported = false, darkTheme = false)

    @Test
    fun reportedDark() = capture(isReported = true, darkTheme = true)

    // the plain Application keeps Hilt out
    private fun capture(isReported: Boolean, darkTheme: Boolean) {
        captureRoboImage {
            BottlingTheme(darkTheme = darkTheme) {
                Box(Modifier.background(MaterialTheme.colorScheme.background).padding(24.dp)) {
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
}
