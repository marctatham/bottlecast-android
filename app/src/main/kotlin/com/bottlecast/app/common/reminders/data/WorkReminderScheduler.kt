package com.bottlecast.app.common.reminders.data

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bottlecast.app.common.reminders.domain.IReminderScheduler
import com.bottlecast.app.common.reminders.domain.ReminderSettings
import com.bottlecast.app.common.reminders.domain.nextReminderAt
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** The reminder's one piece of unique work, replaced each time it's scheduled. */
internal const val DAILY_REMINDER_WORK = "daily_reminder"

/**
 * One reminder at a time, as unique one-time work at the next reminder time (spec §3.16b). Each run
 * schedules the next, and so does each start of the app and each change to the settings, so a new
 * time zone or a changed time takes effect without waiting a day.
 */
class WorkReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clock: Clock,
) : IReminderScheduler {

    override fun schedule(settings: ReminderSettings) {
        val work = WorkManager.getInstance(context)
        if (!settings.enabled) {
            work.cancelUniqueWork(DAILY_REMINDER_WORK)
            return
        }
        val now = ZonedDateTime.now(clock)
        val delay = Duration.between(now, nextReminderAt(now, settings))
        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        work.enqueueUniqueWork(DAILY_REMINDER_WORK, ExistingWorkPolicy.REPLACE, request)
        Timber.d("[schedule] The next reminder is in ${delay.toMinutes()} minutes")
    }
}
