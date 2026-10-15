package com.bottlecast.app.common.reminders.domain

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalTime

/** The reminder's settings, kept on this phone. */
interface IReminderStore {
    val settings: Flow<ReminderSettings>

    fun setEnabled(enabled: Boolean): Job

    fun setTime(time: LocalTime): Job

    fun setQuietHours(start: LocalTime, end: LocalTime): Job

    fun markRationaleOffered(): Job

    /** The permission was refused at [at], or null once it's granted. */
    fun markDenied(at: Instant?): Job
}

/** Schedules the next reminder from the settings, or cancels it while they're off. */
fun interface IReminderScheduler {
    fun schedule(settings: ReminderSettings)
}

/** What Android lets the app do with notifications. */
interface INotificationAccess {
    /** The daily reminder can be posted: POST_NOTIFICATIONS is granted, and neither the app nor its channel is off in Android's settings. */
    fun allowed(): Boolean

    /** POST_NOTIFICATIONS is granted, whatever Android's settings say. */
    fun permissionGranted(): Boolean
}
