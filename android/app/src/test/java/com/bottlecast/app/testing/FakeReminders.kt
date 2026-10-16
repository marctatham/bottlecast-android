package com.bottlecast.app.testing

import com.bottlecast.app.common.reminders.domain.INotificationAccess
import com.bottlecast.app.common.reminders.domain.IReminderStore
import com.bottlecast.app.common.reminders.domain.ReminderSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalTime

/** The reminder's settings in memory: each change shows at once, and its Job is already done. */
class MemoryReminderStore(initial: ReminderSettings = ReminderSettings()) : IReminderStore {
    override val settings = MutableStateFlow(initial)

    override fun setEnabled(enabled: Boolean): Job = change { it.copy(enabled = enabled) }

    override fun setTime(time: LocalTime): Job = change { it.copy(time = time) }

    override fun setQuietHours(start: LocalTime, end: LocalTime): Job = change { it.copy(quietStart = start, quietEnd = end) }

    override fun markRationaleOffered(): Job = change { it.copy(rationaleOffered = true) }

    override fun markDenied(at: Instant?): Job = change { it.copy(deniedAt = at) }

    private fun change(edit: (ReminderSettings) -> ReminderSettings): Job {
        settings.update(edit)
        return Job().apply { complete() }
    }
}

/** What Android lets the app do with notifications, set by the test. */
class FakeNotificationAccess(var allowed: Boolean = false, var permissionGranted: Boolean = false) : INotificationAccess {
    override fun allowed(): Boolean = allowed

    override fun permissionGranted(): Boolean = permissionGranted
}
