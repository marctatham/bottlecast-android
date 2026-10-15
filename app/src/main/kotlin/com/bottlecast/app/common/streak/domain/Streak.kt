package com.bottlecast.app.common.streak.domain

import java.time.LocalDate

/** The most calm-sea shields anyone holds at once (spec §3.3, G2). */
const val MAX_SHIELDS = 2

/**
 * The tide streak as it stands today (spec §3.3): [current] counts consecutive local days with a find,
 * including today once today's bottle is found. A streak stays alive through today until today ends.
 */
data class Streak(
    val current: Int,
    val best: Int,
    val shields: Int,
    val foundToday: Boolean,
    /** The newest day a shield kept the streak alive, or a missed day rested it. */
    val lastEvent: StreakEvent? = null,
) {
    /** The streak a guest brings to another account: the days before today, whose find moves on its own. */
    val carryable: Int get() = if (foundToday) current - 1 else current

    companion object {
        val None = Streak(current = 0, best = 0, shields = 0, foundToday = false)
    }
}

/** Something a missed day did to the streak, for the Shore's one-time snackbar. */
sealed interface StreakEvent {
    val date: LocalDate

    /** A shield kept a streak of [days] alive through the missed [date]. */
    data class ShieldUsed(override val date: LocalDate, val days: Int) : StreakEvent

    /** The missed [date] had no shield to use, so the streak rested. */
    data class Rested(override val date: LocalDate) : StreakEvent
}

/** What one local date did for the streak: a find, the day's first cast, a calm sea, a carried streak. */
data class StreakDay(
    val date: LocalDate,
    val found: Boolean = false,
    val cast: Boolean = false,
    val calm: Boolean = false,
    val carried: Int = 0,
)

/**
 * The streak folded through the end of [through], so a fold can carry on from there without reading
 * the days before it again. [lastEvent] is the newest one up to [through].
 */
data class StreakCheckpoint(
    val through: LocalDate,
    val current: Int,
    val best: Int,
    val shields: Int,
    val lastEvent: StreakEvent? = null,
)
