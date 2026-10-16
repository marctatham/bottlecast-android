package com.bottlecast.app.common.components.illustrations

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.testing.captureThemed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The edge states' spot illustrations (spec §1.6): the calm sea and the fog from their boards, and
 * the bottle slipping back, which no board draws.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class SpotIllustrationsScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun illustrationsNight() = capture(darkTheme = true)

    @Test
    fun illustrationsDawn() = capture(darkTheme = false)

    private fun capture(darkTheme: Boolean) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .width(390.dp)
                    .padding(vertical = BottlingTheme.spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xxl),
            ) {
                SeaCalmIllustration()
                FogIllustration()
                SlippedBackIllustration()
            }
        }
    }
}
