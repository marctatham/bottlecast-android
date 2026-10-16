package com.bottlecast.app.common.streak.domain

import java.time.LocalDate

/**
 * Folds whole days into the streak, from the day after [from]'s `through` (or the first day there is,
 * with no checkpoint yet) up to and including [through]. Each day, in the order it happens (spec §3.3):
 * 1. a streak carried over from a guest counts as far as it reaches;
 * 2. the day's first cast gives a shield, up to [MAX_SHIELDS];
 * 3. a find adds a day; a calm sea holds the streak; any other day is missed, and uses a shield, or
 *    rests the streak when there's none.
 *
 * So a day with a cast but no find never rests a streak: its shield is given, then used.
 */
fun foldStreak(from: StreakCheckpoint?, days: Map<LocalDate, StreakDay>, through: LocalDate): StreakCheckpoint {
    val start = from?.through?.plusDays(1) ?: days.keys.minOrNull() ?: through.plusDays(1)
    var current = from?.current ?: 0
    var best = from?.best ?: 0
    var shields = from?.shields ?: 0
    var lastEvent = from?.lastEvent

    var date = start
    while (!date.isAfter(through)) {
        val day = days[date]
        if (day != null && day.carried > current) current = day.carried
        if (day?.cast == true && shields < MAX_SHIELDS) shields++
        when {
            day?.found == true -> current++
            day?.calm == true -> Unit
            current == 0 -> Unit
            shields > 0 -> {
                shields--
                lastEvent = StreakEvent.ShieldUsed(date, current)
            }
            else -> {
                current = 0
                lastEvent = StreakEvent.Rested(date)
            }
        }
        best = maxOf(best, current)
        date = date.plusDays(1)
    }
    return StreakCheckpoint(maxOf(through, from?.through ?: through), current, best, shields, lastEvent)
}

/**
 * The streak today: every whole day up to yesterday folded, then today so far. Today is never missed,
 * since it isn't over: its carried streak, cast and find count as they happen.
 */
fun liveStreak(from: StreakCheckpoint?, days: Map<LocalDate, StreakDay>, today: LocalDate): Streak {
    val past = foldStreak(from, days, today.minusDays(1))
    val day = days[today]
    var current = past.current
    var shields = past.shields
    if (day != null && day.carried > current) current = day.carried
    if (day?.cast == true && shields < MAX_SHIELDS) shields++
    val foundToday = day?.found == true
    if (foundToday) current++
    return Streak(
        current = current,
        best = maxOf(past.best, current),
        shields = shields,
        foundToday = foundToday,
        lastEvent = past.lastEvent,
    )
}

/**
 * The event the Shore should mention once (spec §3.3), or null: a shield used or a rest from the last
 * few days that the user hasn't seen, and that still says something true about the streak now.
 */
fun StreakEvent.isWorthNoticing(streak: Streak, today: LocalDate, seenThrough: LocalDate?): Boolean {
    if (seenThrough != null && !date.isAfter(seenThrough)) return false
    if (date.isBefore(today.minusDays(NOTICE_DAYS))) return false
    return when (this) {
        is StreakEvent.ShieldUsed -> streak.current > 0
        // finding today starts a new streak, so there's nothing to nudge about any more
        is StreakEvent.Rested -> !streak.foundToday
    }
}

// how far back a missed day is still news
private const val NOTICE_DAYS = 3L
