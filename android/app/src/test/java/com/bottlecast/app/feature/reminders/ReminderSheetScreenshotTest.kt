package com.bottlecast.app.feature.reminders

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bottlecast.app.R
import com.bottlecast.app.common.components.BottlingTimePickerDialog
import com.bottlecast.app.common.components.rememberBottlingSheetState
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.testing.captureScreenByDrawing
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalTime

/** The reminder sheet (spec §3.16a, the Notify-Rationale board) and the time picker it opens, in Night and Dawn. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class ReminderSheetScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun askingNight() = captureSheet(darkTheme = true, ReminderSheetUiState())

    @Test
    fun askingDawn() = captureSheet(darkTheme = false, ReminderSheetUiState())

    // asked once before: Android's rationale line joins the body
    @Test
    fun rationaleNight() = captureSheet(darkTheme = true, ReminderSheetUiState(time = LocalTime.of(7, 30)), showRationale = true)

    @Test
    fun blockedNight() = captureSheet(darkTheme = true, ReminderSheetUiState(blocked = true))

    @Test
    fun blockedDawn() = captureSheet(darkTheme = false, ReminderSheetUiState(blocked = true))

    @Test
    @Config(fontScale = 2f)
    fun largeTextNight() = captureSheet(darkTheme = true, ReminderSheetUiState())

    @Test
    fun timePickerNight() = captureWindow(darkTheme = true) { TimePicker(typing = false) }

    @Test
    fun timePickerDawn() = captureWindow(darkTheme = false) { TimePicker(typing = false) }

    @Test
    fun timeInputNight() = captureWindow(darkTheme = true) { TimePicker(typing = true) }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun captureSheet(darkTheme: Boolean, uiState: ReminderSheetUiState, showRationale: Boolean = false) =
        captureWindow(darkTheme) {
            ReminderSheet(
                uiState = uiState,
                showRationale = showRationale,
                sheetState = rememberBottlingSheetState(),
                onDismissRequest = {},
                onPickTime = {},
                onTurnOn = {},
                onOpenSettings = {},
                onNotNow = {},
            )
        }

    @Composable
    private fun TimePicker(typing: Boolean) {
        BottlingTimePickerDialog(
            title = R.string.reminder_time_title,
            initial = LocalTime.of(9, 0),
            onDismissRequest = {},
            onConfirm = {},
            startTyping = typing,
        )
    }

    // a sheet or dialog is its own window and animates in, so the clock is advanced past it and every
    // window is drawn, scrim included
    private fun captureWindow(darkTheme: Boolean, content: @Composable () -> Unit) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            BottlingTheme(darkTheme = darkTheme) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                content()
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
