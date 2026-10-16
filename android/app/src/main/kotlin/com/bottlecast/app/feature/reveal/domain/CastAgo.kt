package com.bottlecast.app.feature.reveal.domain

import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** How long ago a bottle was cast, for its byline ("cast 3 days ago"), by local calendar days. */
sealed interface CastAgo {
    data object Today : CastAgo
    data object Yesterday : CastAgo
    data class Days(val days: Int) : CastAgo
    data class Months(val months: Int) : CastAgo
    data class Years(val years: Int) : CastAgo
}

/** How long before [now] a bottle cast at [castAt] went into the sea, in [now]'s time zone. */
fun castAgo(castAt: Instant, now: ZonedDateTime): CastAgo {
    val castDay = castAt.atZone(now.zone).toLocalDate()
    val today = now.toLocalDate()
    val days = ChronoUnit.DAYS.between(castDay, today)
    return when {
        days <= 0 -> CastAgo.Today
        days == 1L -> CastAgo.Yesterday
        days < DAYS_IN_A_MONTH -> CastAgo.Days(days.toInt())
        else -> {
            val months = ChronoUnit.MONTHS.between(castDay, today).coerceAtLeast(1)
            if (months < MONTHS_IN_A_YEAR) CastAgo.Months(months.toInt()) else CastAgo.Years(ChronoUnit.YEARS.between(castDay, today).toInt())
        }
    }
}

private const val DAYS_IN_A_MONTH = 30
private const val MONTHS_IN_A_YEAR = 12
