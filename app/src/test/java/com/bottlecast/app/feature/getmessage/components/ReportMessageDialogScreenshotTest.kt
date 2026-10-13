package com.bottlecast.app.feature.getmessage.components

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bottlecast.app.common.ui.BottlingTheme
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Today's report dialog in the Moonlit Shore theme: surfaceHigh, 32dp corners, sage text buttons.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class ReportMessageDialogScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dialogNight() = capture(darkTheme = true)

    @Test
    fun dialogDawn() = capture(darkTheme = false)

    // the dialog is its own window, so the whole screen is captured
    @OptIn(ExperimentalRoborazziApi::class)
    private fun capture(darkTheme: Boolean) {
        composeTestRule.setContent {
            BottlingTheme(darkTheme = darkTheme) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                ReportMessageDialog(onDismiss = {}, onReportConfirmed = {})
            }
        }
        composeTestRule.waitForIdle()
        captureScreenRoboImage()
    }
}
