package com.bottlecast.app.feature.reminders

import com.bottlecast.app.common.reminders.domain.ReminderSettings
import com.bottlecast.app.testing.FakeNotificationAccess
import com.bottlecast.app.testing.MainDispatcherRule
import com.bottlecast.app.testing.MemoryReminderStore
import com.bottlecast.app.testing.MutableClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalTime

/** Unit tests for [ReminderSheetViewModel]: turning the reminder on, through Android's permission or its settings. */
class ReminderSheetViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = MutableClock(Instant.parse("2026-10-15T09:00:00Z"))
    private val store = MemoryReminderStore()
    private val access = FakeNotificationAccess()

    private fun viewModel() = ReminderSheetViewModel(store, access, clock)

    @Test
    fun `opening the sheet counts as offering it, and shows the reminder's time`() = runTest {
        // given
        store.setTime(LocalTime.of(7, 30))

        // when
        val viewModel = viewModel()

        // then
        Assert.assertTrue(store.settings.value.rationaleOffered)
        Assert.assertEquals(ReminderSheetUiState(time = LocalTime.of(7, 30)), viewModel.state.value)
    }

    @Test
    fun `with notifications allowed, turning on needs no prompt`() = runTest {
        // given
        access.allowed = true
        val viewModel = viewModel()

        // when
        val ask = viewModel.onTurnOn()

        // then
        Assert.assertFalse(ask)
        Assert.assertTrue(store.settings.value.enabled)
        Assert.assertEquals(ReminderOutcome.Enabled(LocalTime.of(9, 0)), viewModel.state.value.outcome)
    }

    @Test
    fun `without the permission, Android is asked, and a yes turns the reminder on`() = runTest {
        // given
        val viewModel = viewModel()
        store.markDenied(Instant.parse("2026-10-01T09:00:00Z"))

        // when
        val ask = viewModel.onTurnOn()
        access.allowed = true
        viewModel.onPermissionResult(granted = true, canAskAgain = false)

        // then
        Assert.assertTrue(ask)
        Assert.assertEquals(ReminderSettings(enabled = true, rationaleOffered = true), store.settings.value)
        Assert.assertEquals(ReminderOutcome.Enabled(LocalTime.of(9, 0)), viewModel.state.value.outcome)
    }

    @Test
    fun `a time in quiet hours is said as when they end`() = runTest {
        // given
        access.allowed = true
        store.setTime(LocalTime.of(23, 0))
        val viewModel = viewModel()

        // when
        viewModel.onTurnOn()

        // then
        Assert.assertEquals(ReminderOutcome.Enabled(LocalTime.of(8, 0)), viewModel.state.value.outcome)
    }

    @Test
    fun `a no closes the sheet and pauses asking for a week`() = runTest {
        // given
        val viewModel = viewModel()

        // when
        viewModel.onPermissionResult(granted = false, canAskAgain = true)

        // then
        Assert.assertEquals(ReminderOutcome.Declined, viewModel.state.value.outcome)
        Assert.assertEquals(clock.instant(), store.settings.value.deniedAt)
        Assert.assertFalse(store.settings.value.enabled)
    }

    @Test
    fun `once Android won't ask again, the sheet points to its settings, and coming back allowed turns it on`() = runTest {
        // given
        val viewModel = viewModel()
        viewModel.onPermissionResult(granted = false, canAskAgain = false)
        val blocked = viewModel.state.value

        // when
        viewModel.onOpenSettings()
        access.allowed = true
        viewModel.onResume()

        // then
        Assert.assertEquals(ReminderSheetUiState(blocked = true), blocked)
        Assert.assertEquals(ReminderOutcome.Enabled(LocalTime.of(9, 0)), viewModel.state.value.outcome)
        Assert.assertNull(store.settings.value.deniedAt)
    }

    @Test
    fun `with the permission granted but notifications off in Android's settings, it points there without asking`() = runTest {
        // given
        access.permissionGranted = true
        val viewModel = viewModel()

        // when
        val ask = viewModel.onTurnOn()

        // then
        Assert.assertFalse(ask)
        Assert.assertEquals(ReminderSheetUiState(blocked = true), viewModel.state.value)
    }

    @Test
    fun `coming back without having opened Android's settings changes nothing`() = runTest {
        // given
        val viewModel = viewModel()
        access.allowed = true

        // when
        viewModel.onResume()

        // then
        Assert.assertEquals(ReminderSheetUiState(), viewModel.state.value)
        Assert.assertFalse(store.settings.value.enabled)
    }

    @Test
    fun `each opening starts afresh, with the reminder's time as it is`() = runTest {
        // given
        val viewModel = viewModel()
        viewModel.onPermissionResult(granted = false, canAskAgain = false)
        viewModel.onOpenSettings()
        store.setTime(LocalTime.of(7, 0))

        // when
        viewModel.reset()
        access.allowed = true
        viewModel.onResume()

        // then
        Assert.assertEquals(ReminderSheetUiState(time = LocalTime.of(7, 0)), viewModel.state.value)
    }
}
