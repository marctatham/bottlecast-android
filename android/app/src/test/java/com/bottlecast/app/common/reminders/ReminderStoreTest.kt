package com.bottlecast.app.common.reminders

import com.bottlecast.app.common.reminders.data.ReminderStore
import com.bottlecast.app.common.reminders.domain.ReminderSettings
import com.bottlecast.app.testing.filePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import java.time.Instant
import java.time.LocalTime

/** Unit tests for [ReminderStore], on a real file. */
class ReminderStoreTest {

    @Test
    fun `the reminder is off at 9 AM with quiet hours from 10 PM to 8 AM until it's changed`() = runTest {
        // given
        val store = ReminderStore(filePreferences(backgroundScope), backgroundScope)

        // when
        val settings = store.settings.first()

        // then
        Assert.assertEquals(ReminderSettings(), settings)
    }

    @Test
    fun `every setting is kept`() = runTest {
        // given
        val store = ReminderStore(filePreferences(backgroundScope), backgroundScope)
        val denied = Instant.parse("2026-10-15T09:00:00Z")

        // when
        store.setEnabled(true).join()
        store.setTime(LocalTime.of(7, 45)).join()
        store.setQuietHours(LocalTime.of(23, 0), LocalTime.of(6, 30)).join()
        store.markRationaleOffered().join()
        store.markDenied(denied).join()

        // then
        Assert.assertEquals(
            ReminderSettings(
                enabled = true,
                time = LocalTime.of(7, 45),
                quietStart = LocalTime.of(23, 0),
                quietEnd = LocalTime.of(6, 30),
                rationaleOffered = true,
                deniedAt = denied,
            ),
            store.settings.first(),
        )
    }

    @Test
    fun `a granted permission clears the refusal`() = runTest {
        // given
        val store = ReminderStore(filePreferences(backgroundScope), backgroundScope)
        store.markDenied(Instant.parse("2026-10-15T09:00:00Z")).join()

        // when
        store.markDenied(null).join()

        // then
        Assert.assertNull(store.settings.first().deniedAt)
    }
}
