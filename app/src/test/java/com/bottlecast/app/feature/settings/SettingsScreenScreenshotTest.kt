package com.bottlecast.app.feature.settings

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bottlecast.app.common.components.rememberBottlingSheetState
import com.bottlecast.app.common.reminders.domain.ReminderSettings
import com.bottlecast.app.common.sea.domain.BlockedWriter
import com.bottlecast.app.common.settings.domain.AppSettings
import com.bottlecast.app.common.settings.domain.ThemeChoice
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.feature.settings.components.AccountBusyOverlay
import com.bottlecast.app.feature.settings.components.BlockedWritersSheet
import com.bottlecast.app.feature.settings.components.DeleteAccountDialog
import com.bottlecast.app.feature.settings.components.QuietHoursSheet
import com.bottlecast.app.feature.settings.components.SignOutDialog
import com.bottlecast.app.testing.BoardStatusBar
import com.bottlecast.app.testing.captureScreenByDrawing
import com.bottlecast.app.testing.captureThemed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Settings (spec §3.17) at the Settings board's width, 390, and tall enough for every row, with a 44dp
 * status bar: PNG pixels ÷ 2 = board pixels. The sheets and dialogs it opens are captured at 390×844,
 * each in its own window.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h1600dp-xhdpi")
class SettingsScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun signedInNight() = capture(signedIn(), darkTheme = true)

    @Test
    fun signedInDawn() = capture(signedIn(), darkTheme = false)

    @Test
    fun guestNight() = capture(guest(), darkTheme = true)

    @Test
    fun guestDawn() = capture(guest(), darkTheme = false)

    // reminders on, but Android holds them back
    @Test
    fun notificationsOffNight() = capture(signedIn().copy(notificationsAllowed = false), darkTheme = true)

    @Test
    @Config(qualifiers = "w390dp-h2800dp-xhdpi", fontScale = 2f)
    fun largeTextNight() = capture(signedIn(), darkTheme = true)

    @Test
    @Config(qualifiers = "w390dp-h844dp-xhdpi")
    fun quietHoursNight() = captureWindow(darkTheme = true) { QuietHours() }

    @Test
    @Config(qualifiers = "w390dp-h844dp-xhdpi")
    fun quietHoursDawn() = captureWindow(darkTheme = false) { QuietHours() }

    @Test
    @Config(qualifiers = "w390dp-h844dp-xhdpi")
    fun blockedWritersNight() = captureWindow(darkTheme = true) { Blocked(listOf(BlockedWriter("w1", BLOCKED_AT))) }

    @Test
    @Config(qualifiers = "w390dp-h844dp-xhdpi")
    fun blockedWritersDawn() = captureWindow(darkTheme = false) { Blocked(listOf(BlockedWriter("w1", BLOCKED_AT))) }

    @Test
    @Config(qualifiers = "w390dp-h844dp-xhdpi")
    fun blockedWritersEmptyNight() = captureWindow(darkTheme = true) { Blocked(emptyList()) }

    @Test
    @Config(qualifiers = "w390dp-h844dp-xhdpi")
    fun signOutNight() = captureWindow(darkTheme = true) { SignOutDialog(onDismissRequest = {}, onSignOut = {}) }

    @Test
    @Config(qualifiers = "w390dp-h844dp-xhdpi")
    fun signOutDawn() = captureWindow(darkTheme = false) { SignOutDialog(onDismissRequest = {}, onSignOut = {}) }

    @Test
    @Config(qualifiers = "w390dp-h844dp-xhdpi")
    fun deleteNight() = captureWindow(darkTheme = true) { DeleteAccountDialog(onDismissRequest = {}, onDelete = {}) }

    @Test
    @Config(qualifiers = "w390dp-h844dp-xhdpi")
    fun deleteDawn() = captureWindow(darkTheme = false) { DeleteAccountDialog(onDismissRequest = {}, onDelete = {}) }

    @Test
    @Config(qualifiers = "w390dp-h844dp-xhdpi")
    fun deleteTickedNight() = captureWindow(darkTheme = true) { DeleteAccountDialog(onDismissRequest = {}, onDelete = {}, startTicked = true) }

    @Test
    @Config(qualifiers = "w390dp-h844dp-xhdpi")
    fun deletingNight() = captureWindow(darkTheme = true) { AccountBusyOverlay(deleting = true) }

    private fun capture(uiState: SettingsUiState, darkTheme: Boolean) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            SettingsScreen(
                uiState = uiState,
                onBackHandler = {},
                onRemindersChange = {},
                onTurnOnNotificationsHandler = {},
                onReminderTimeHandler = {},
                onQuietHoursHandler = {},
                onPenNameHandler = {},
                onSignWithPenNameChange = {},
                onBlockedWritersHandler = {},
                onThemeChange = {},
                onReduceMotionChange = {},
                onShakeToUncorkChange = {},
                onHapticsChange = {},
                onHowItWorksHandler = {},
                onLinkHandler = {},
                onLicencesHandler = {},
                onSignInHandler = {},
                onSignOutHandler = {},
                onDeleteAccountHandler = {},
                windowInsets = BoardStatusBar,
                contentPadding = PaddingValues(),
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun QuietHours() {
        QuietHoursSheet(
            start = LocalTime.of(22, 0),
            end = LocalTime.of(8, 0),
            sheetState = rememberBottlingSheetState(),
            onDismissRequest = {},
            onChange = { _, _ -> },
            onDone = {},
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Blocked(writers: List<BlockedWriter>) {
        BlockedWritersSheet(
            writers = writers,
            today = TODAY,
            zone = ZoneOffset.UTC,
            sheetState = rememberBottlingSheetState(),
            onDismissRequest = {},
            onUnblock = {},
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

        // the boards' day, and a block made on 5 Oct (the Settings board's "Blocked writers — 1")
        val TODAY: LocalDate = LocalDate.of(2026, 10, 12)
        val BLOCKED_AT: Instant = Instant.parse("2026-10-05T10:00:00Z")

        // the boards' sample user (TEMPLATE.md)
        fun signedIn() = SettingsUiState.Showing(
            account = SettingsAccount.Google(googleName = "Marc Tatham", penName = "Driftwood"),
            app = AppSettings(theme = ThemeChoice.System),
            reminders = ReminderSettings(enabled = true),
            notificationsAllowed = true,
            blockedWriters = listOf(BlockedWriter("w1", BLOCKED_AT)),
            version = "1.2.0",
        )

        fun guest() = SettingsUiState.Showing(
            account = SettingsAccount.Guest,
            app = AppSettings(),
            reminders = ReminderSettings(),
            notificationsAllowed = false,
            blockedWriters = emptyList(),
            version = "1.2.0",
        )
    }
}
