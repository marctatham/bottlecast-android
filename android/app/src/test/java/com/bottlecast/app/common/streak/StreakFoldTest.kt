package com.bottlecast.app.common.streak

import com.bottlecast.app.common.streak.domain.Streak
import com.bottlecast.app.common.streak.domain.StreakCheckpoint
import com.bottlecast.app.common.streak.domain.StreakDay
import com.bottlecast.app.common.streak.domain.StreakEvent
import com.bottlecast.app.common.streak.domain.foldStreak
import com.bottlecast.app.common.streak.domain.isWorthNoticing
import com.bottlecast.app.common.streak.domain.liveStreak
import org.junit.Assert
import org.junit.Test
import java.time.LocalDate

/** Unit tests for the streak's fold (spec §3.3): finds, shields, calm seas, carried streaks and today. */
class StreakFoldTest {

    private val today = LocalDate.of(2026, 10, 15)

    @Test
    fun `consecutive days with a find count, today included once it's found`() {
        // given
        val days = days(found(-2), found(-1), found(0))

        // when
        val streak = liveStreak(null, days, today)

        // then
        Assert.assertEquals(Streak(current = 3, best = 3, shields = 0, foundToday = true), streak)
    }

    @Test
    fun `a streak stays alive through today until today is over`() {
        // given
        val days = days(found(-2), found(-1))

        // when
        val streak = liveStreak(null, days, today)

        // then
        Assert.assertEquals(Streak(current = 2, best = 2, shields = 0, foundToday = false), streak)
        Assert.assertEquals(2, streak.carryable)
    }

    @Test
    fun `a missed day with no shield rests the streak, and best remembers it`() {
        // given
        val days = days(found(-4), found(-3), found(-1))

        // when
        val streak = liveStreak(null, days, today)

        // then
        Assert.assertEquals(1, streak.current)
        Assert.assertEquals(2, streak.best)
        Assert.assertEquals(StreakEvent.Rested(today.minusDays(2)), streak.lastEvent)
    }

    @Test
    fun `the day's first cast gives a shield, and a missed day uses it`() {
        // given
        val days = days(found(-4, cast = true), found(-3), found(-1))

        // when
        val streak = liveStreak(null, days, today)

        // then
        Assert.assertEquals(Streak(current = 3, best = 3, shields = 0, foundToday = false, lastEvent = StreakEvent.ShieldUsed(today.minusDays(2), 2)), streak)
    }

    @Test
    fun `shields stop at two`() {
        // given
        val days = days(found(-3, cast = true), found(-2, cast = true), found(-1, cast = true))

        // when
        val streak = liveStreak(null, days, today)

        // then
        Assert.assertEquals(2, streak.shields)
    }

    @Test
    fun `a day with a cast but no find gives its shield, then uses it`() {
        // given
        val days = days(found(-2), StreakDay(today.minusDays(1), cast = true))

        // when
        val streak = liveStreak(null, days, today)

        // then
        Assert.assertEquals(1, streak.current)
        Assert.assertEquals(0, streak.shields)
        Assert.assertEquals(StreakEvent.ShieldUsed(today.minusDays(1), 1), streak.lastEvent)
    }

    @Test
    fun `at the cap, a cast-only day ends with one shield fewer`() {
        // given
        val days = days(found(-3, cast = true), found(-2, cast = true), StreakDay(today.minusDays(1), cast = true))

        // when
        val streak = liveStreak(null, days, today)

        // then
        Assert.assertEquals(2, streak.current)
        Assert.assertEquals(1, streak.shields)
    }

    @Test
    fun `a calm sea holds the streak without adding to it`() {
        // given
        val days = days(found(-3), StreakDay(today.minusDays(2), calm = true), found(-1))

        // when
        val streak = liveStreak(null, days, today)

        // then
        Assert.assertEquals(2, streak.current)
        Assert.assertNull(streak.lastEvent)
    }

    @Test
    fun `only the first missed day rests a streak, and the days after have nothing to rest`() {
        // given
        val days = days(found(-9), found(-1))

        // when
        val streak = liveStreak(null, days, today)

        // then
        Assert.assertEquals(1, streak.current)
        Assert.assertEquals(StreakEvent.Rested(today.minusDays(8)), streak.lastEvent)
    }

    @Test
    fun `a carried streak counts as far as it reaches, then today's find adds on`() {
        // given
        val days = days(found(-1), StreakDay(today, found = true, carried = 4))

        // when
        val streak = liveStreak(null, days, today)

        // then
        Assert.assertEquals(5, streak.current)
        Assert.assertEquals(5, streak.best)
    }

    @Test
    fun `a carried streak shorter than the account's own changes nothing`() {
        // given
        val days = days(found(-3), found(-2), found(-1), StreakDay(today, carried = 1))

        // when
        val streak = liveStreak(null, days, today)

        // then
        Assert.assertEquals(3, streak.current)
    }

    @Test
    fun `folding on from a checkpoint matches folding the whole history`() {
        // given
        val days = days(found(-9, cast = true), found(-8), found(-7), found(-5), found(-4, cast = true), found(-3), found(-1))
        val checkpoint = foldStreak(null, days, today.minusDays(6))

        // when
        val fromCheckpoint = liveStreak(checkpoint, days.filterKeys { it > checkpoint.through }, today)
        val whole = liveStreak(null, days, today)

        // then
        Assert.assertEquals(whole, fromCheckpoint)
    }

    @Test
    fun `a checkpoint with nothing since carries on, missing each day`() {
        // given
        val checkpoint = StreakCheckpoint(through = today.minusDays(3), current = 6, best = 14, shields = 1)

        // when
        val streak = liveStreak(checkpoint, emptyMap(), today)

        // then: the shield kept day -2, then day -1 rested it
        Assert.assertEquals(0, streak.current)
        Assert.assertEquals(14, streak.best)
        Assert.assertEquals(StreakEvent.Rested(today.minusDays(1)), streak.lastEvent)
    }

    @Test
    fun `nothing at all is no streak`() {
        Assert.assertEquals(Streak.None, liveStreak(null, emptyMap(), today))
    }

    @Test
    fun `news is worth mentioning while it's recent, unseen and still true`() {
        // given
        val shield = StreakEvent.ShieldUsed(today.minusDays(1), 6)
        val rested = StreakEvent.Rested(today.minusDays(1))
        val alive = Streak(current = 6, best = 6, shields = 0, foundToday = false)
        val foundAgain = Streak(current = 1, best = 6, shields = 0, foundToday = true)

        // then
        Assert.assertTrue(shield.isWorthNoticing(alive, today, seenThrough = null))
        Assert.assertFalse(shield.isWorthNoticing(alive, today, seenThrough = today.minusDays(1)))
        Assert.assertFalse(StreakEvent.ShieldUsed(today.minusDays(5), 6).isWorthNoticing(alive, today, seenThrough = null))
        Assert.assertTrue(rested.isWorthNoticing(alive.copy(current = 0), today, seenThrough = null))
        Assert.assertFalse(rested.isWorthNoticing(foundAgain, today, seenThrough = null))
    }

    private fun found(daysAgo: Int, cast: Boolean = false) = StreakDay(today.plusDays(daysAgo.toLong()), found = true, cast = cast)

    private fun days(vararg days: StreakDay): Map<LocalDate, StreakDay> = days.associateBy { it.date }
}
