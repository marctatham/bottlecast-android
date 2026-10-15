package com.bottlecast.app.fakes

import com.bottlecast.app.common.reminders.domain.INotificationAccess
import com.bottlecast.app.common.reminders.domain.IReminderScheduler
import com.bottlecast.app.common.reminders.domain.IReminderStore
import com.bottlecast.app.common.reminders.domain.ReminderSettings
import com.bottlecast.app.common.settings.domain.AppSettings
import com.bottlecast.app.common.settings.domain.ISettingsStore
import com.bottlecast.app.common.settings.domain.IThemeApplier
import com.bottlecast.app.common.settings.domain.ThemeChoice
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

private fun done(): Job = Job().apply { complete() }

/** The phone's settings in memory, since DataStore stays out of instrumented tests. */
@Singleton
class FakeSettingsStore @Inject constructor() : ISettingsStore {
    override val settings = MutableStateFlow(AppSettings())

    override fun setTheme(theme: ThemeChoice): Job = done().also { settings.update { it.copy(theme = theme) } }

    override fun setReduceMotion(reduce: Boolean): Job = done().also { settings.update { it.copy(reduceMotion = reduce) } }

    override fun setHaptics(on: Boolean): Job = done().also { settings.update { it.copy(haptics = on) } }

    override fun setShakeToUncork(on: Boolean): Job = done().also { settings.update { it.copy(shakeToUncork = on) } }

    override fun setSignWithPenName(sign: Boolean): Job = done().also { settings.update { it.copy(signWithPenName = sign) } }
}

/** Records the theme Settings applied, rather than changing the emulator's. */
@Singleton
class FakeThemeApplier @Inject constructor() : IThemeApplier {
    @Volatile var applied: ThemeChoice? = null
        private set

    override fun apply(theme: ThemeChoice) {
        applied = theme
    }
}

/**
 * The reminder's settings in memory. The sheet is offered already, so it never opens by itself over a
 * test that isn't about it.
 */
@Singleton
class FakeReminderStore @Inject constructor() : IReminderStore {
    override val settings = MutableStateFlow(ReminderSettings(rationaleOffered = true))

    override fun setEnabled(enabled: Boolean): Job = done().also { settings.update { it.copy(enabled = enabled) } }

    override fun setTime(time: LocalTime): Job = done().also { settings.update { it.copy(time = time) } }

    override fun setQuietHours(start: LocalTime, end: LocalTime): Job =
        done().also { settings.update { it.copy(quietStart = start, quietEnd = end) } }

    override fun markRationaleOffered(): Job = done().also { settings.update { it.copy(rationaleOffered = true) } }

    override fun markDenied(at: Instant?): Job = done().also { settings.update { it.copy(deniedAt = at) } }
}

/** What Android lets the app do with notifications, set by the test: allowed, so no prompt shows. */
@Singleton
class FakeNotificationAccess @Inject constructor() : INotificationAccess {
    @Volatile var allowed = true
    @Volatile var permissionGranted = true

    override fun allowed(): Boolean = allowed

    override fun permissionGranted(): Boolean = permissionGranted
}

/** Schedules nothing: WorkManager isn't set up in instrumented tests. */
@Singleton
class FakeReminderScheduler @Inject constructor() : IReminderScheduler {
    @Volatile var scheduled: ReminderSettings? = null
        private set

    override fun schedule(settings: ReminderSettings) {
        scheduled = settings
    }
}
