package com.bottlecast.app.common.reminders.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime

/** The daily reminder (spec §3.16), as this phone keeps it. Off until the user turns it on. */
data class ReminderSettings(
    val enabled: Boolean = false,
    val time: LocalTime = DEFAULT_REMINDER_TIME,
    /** Quiet hours hold back every notification, the reminder included (decided 2026-10-15). */
    val quietStart: LocalTime = DEFAULT_QUIET_START,
    val quietEnd: LocalTime = DEFAULT_QUIET_END,
    /** The rationale sheet was offered after the first find, so it doesn't open by itself again. */
    val rationaleOffered: Boolean = false,
    /** When the notification permission was last refused: it isn't asked for again for a week. */
    val deniedAt: Instant? = null,
)

val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(9, 0)
val DEFAULT_QUIET_START: LocalTime = LocalTime.of(22, 0)
val DEFAULT_QUIET_END: LocalTime = LocalTime.of(8, 0)

/** How long after a refusal the app waits before asking for the permission again (spec §3.16a). */
val PERMISSION_PAUSE: Duration = Duration.ofDays(7)

/** How late a reminder may still go out, when the phone was asleep or busy at its time. */
val REMINDER_GRACE: Duration = Duration.ofHours(2)

/** Whether [time] falls in quiet hours from [start] to [end], which may run past midnight. Equal ends mean none. */
fun isQuiet(time: LocalTime, start: LocalTime, end: LocalTime): Boolean = when {
    start == end -> false
    start < end -> time >= start && time < end
    else -> time >= start || time < end
}

/** The reminder's time of day once quiet hours have had their say: their end, if it falls inside them. */
fun ReminderSettings.effectiveTime(): LocalTime = if (isQuiet(time, quietStart, quietEnd)) quietEnd else time

/** When the next reminder goes out after [now], in [now]'s zone. */
fun nextReminderAt(now: ZonedDateTime, settings: ReminderSettings): ZonedDateTime {
    val time = settings.effectiveTime()
    val today = now.toLocalDate().atTime(time).atZone(now.zone)
    return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
}

/**
 * Whether the app may offer the reminder: it isn't on and working already, and Android's prompt wasn't
 * refused in the past week (spec §3.16a).
 */
fun ReminderSettings.mayOffer(now: Instant, notificationsAllowed: Boolean): Boolean =
    !(enabled && notificationsAllowed) && (deniedAt == null || now >= deniedAt.plus(PERMISSION_PAUSE))

/** Why a reminder that's due stays quiet, or that it goes out. */
enum class ReminderDecision {
    Post,
    Off,

    /** Notifications are turned off for the app. */
    NotAllowed,

    /** Today's bottle is already found (spec §3.16b). */
    FoundToday,

    /** The app is on screen, so the user can see the Shore for themselves (spec §3.16b). */
    InForeground,

    /** It ran far from its time, such as after the phone's time zone changed: it's rescheduled instead. */
    OutOfWindow,
}

/** Whether a reminder running at [now] should go out (spec §3.16b). */
fun decideReminder(
    now: ZonedDateTime,
    settings: ReminderSettings,
    notificationsAllowed: Boolean,
    foundToday: Boolean,
    inForeground: Boolean,
): ReminderDecision {
    if (!settings.enabled) return ReminderDecision.Off
    if (!notificationsAllowed) return ReminderDecision.NotAllowed
    val due = now.toLocalDate().atTime(settings.effectiveTime()).atZone(now.zone)
    val late = Duration.between(due, now)
    if (late.isNegative || late >= REMINDER_GRACE || isQuiet(now.toLocalTime(), settings.quietStart, settings.quietEnd)) {
        return ReminderDecision.OutOfWindow
    }
    if (foundToday) return ReminderDecision.FoundToday
    if (inForeground) return ReminderDecision.InForeground
    return ReminderDecision.Post
}
