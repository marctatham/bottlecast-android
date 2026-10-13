package com.bottlecast.app.feature.home

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bottlecast.app.R
import com.bottlecast.app.testing.captureThemed
import com.bottlecast.app.testing.loadLottieComposition
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Today's Home in the Moonlit Shore theme (its layout is replaced by the Shore later in slice 1),
 * at the boards' 390×844 frame: PNG pixels ÷ 2 = board pixels.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class HomeScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val composition = loadLottieComposition(R.raw.message_in_bottle)

    @Test
    fun normalNight() = capture(HomeUiState.Normal, darkTheme = true)

    @Test
    fun normalDawn() = capture(HomeUiState.Normal, darkTheme = false)

    @Test
    fun adminNight() = capture(HomeUiState.Admin, darkTheme = true)

    @Test
    fun adminDawn() = capture(HomeUiState.Admin, darkTheme = false)

    private fun capture(uiState: HomeUiState, darkTheme: Boolean) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            HomeScreen(
                onGetMessageHandler = {},
                onPostMessageHandler = {},
                onViewMessagesHandler = {},
                homeUiState = uiState,
                composition = composition,
                progress = { BOTTLE_PROGRESS },
            )
        }
    }

    private companion object {
        const val BOTTLE_PROGRESS = 0f
    }
}
