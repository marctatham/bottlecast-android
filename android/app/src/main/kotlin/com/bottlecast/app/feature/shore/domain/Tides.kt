package com.bottlecast.app.feature.shore.domain

import java.time.Duration
import java.time.ZonedDateTime

/**
 * Where the local day's tide stands (spec §3.3): a new bottle washes up at local midnight, so the
 * next tide is the next midnight in the phone's time zone.
 *
 * @property untilNextTide the time left until local midnight.
 * @property dayElapsed the share of today that has passed, from 0 at midnight towards 1. It's
 *   measured against the day's real length, so it stays right on 23- and 25-hour days.
 */
data class Tide(
    val untilNextTide: Duration,
    val dayElapsed: Float,
)

/** The tide at [now], in [now]'s time zone. */
fun tideAt(now: ZonedDateTime): Tide {
    val today = now.toLocalDate()
    val startOfDay = today.atStartOfDay(now.zone)
    val nextTide = today.plusDays(1).atStartOfDay(now.zone)
    val dayLength = Duration.between(startOfDay, nextTide)
    return Tide(
        untilNextTide = Duration.between(now, nextTide),
        dayElapsed = Duration.between(startOfDay, now).toMillis().toFloat() / dayLength.toMillis(),
    )
}

/** How the countdown to the next tide reads (spec §3.3). */
sealed interface TideCountdown {
    /** An hour or more: "3h 12m". */
    data class HoursMinutes(val hours: Long, val minutes: Long) : TideCountdown

    /** Under an hour: "42 min". */
    data class Minutes(val minutes: Long) : TideCountdown

    /** Under a minute: "Any moment…". */
    data object AnyMoment : TideCountdown
}

/** The countdown for [untilNextTide], rounded down to the minute. */
fun countdownFor(untilNextTide: Duration): TideCountdown = when {
    untilNextTide < Duration.ofMinutes(1) -> TideCountdown.AnyMoment
    untilNextTide < Duration.ofHours(1) -> TideCountdown.Minutes(untilNextTide.toMinutes())
    else -> TideCountdown.HoursMinutes(untilNextTide.toHours(), untilNextTide.toMinutesPart().toLong())
}
