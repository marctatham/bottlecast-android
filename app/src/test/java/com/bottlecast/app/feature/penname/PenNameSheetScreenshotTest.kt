package com.bottlecast.app.feature.penname

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bottlecast.app.common.components.rememberBottlingSheetState
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.testing.captureScreenByDrawing
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The pen-name sheet (spec §3.2): empty, with a name, and too short. No board draws it. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class PenNameSheetScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyNight() = capture(darkTheme = true, PenNameUiState.Editing())

    @Test
    fun emptyDawn() = capture(darkTheme = false, PenNameUiState.Editing())

    @Test
    fun namedNight() = capture(darkTheme = true, PenNameUiState.Editing(name = "Driftwood"))

    @Test
    fun namedDawn() = capture(darkTheme = false, PenNameUiState.Editing(name = "Driftwood"))

    @Test
    fun tooShortNight() = capture(darkTheme = true, PenNameUiState.Editing(name = "Al", showError = true))

    @Test
    fun tooShortDawn() = capture(darkTheme = false, PenNameUiState.Editing(name = "Al", showError = true))

    @OptIn(ExperimentalMaterial3Api::class)
    private fun capture(darkTheme: Boolean, uiState: PenNameUiState.Editing) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            BottlingTheme(darkTheme = darkTheme) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                PenNameSheet(
                    uiState = uiState,
                    sheetState = rememberBottlingSheetState(),
                    onDismissRequest = {},
                    onNameChange = {},
                    onSurpriseMe = {},
                    onSave = {},
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        composeTestRule.waitForIdle()
        composeTestRule.captureScreenByDrawing()
    }

    private companion object {
        const val SETTLE_MILLIS = 2_000L
    }
}
