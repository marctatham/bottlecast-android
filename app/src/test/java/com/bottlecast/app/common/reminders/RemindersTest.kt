package com.bottlecast.app.common.reminders

import com.bottlecast.app.common.reminders.domain.ReminderDecision
import com.bottlecast.app.common.reminders.domain.ReminderSettings
import com.bottlecast.app.common.reminders.domain.decideReminder
import com.bottlecast.app.common.reminders.domain.effectiveTime
import com.bottlecast.app.common.reminders.domain.isQuiet
import com.bottlecast.app.common.reminders.domain.mayOffer
import com.bottlecast.app.common.reminders.domain.nextReminderAt
import org.junit.Assert
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Unit tests for the daily reminder's rules: quiet hours, when it's next due, and whether a run posts it. */
class RemindersTest {

    private val london = ZoneId.of("Europe/London")
    private val on = ReminderSettings(enabled = true)

    @Test
    fun `quiet hours can run past midnight, and equal ends mean none`() {
        // given
        val ten = LocalTime.of(22, 0)
        val eight = LocalTime.of(8, 0)

        // when
        val atEleven = isQuiet(LocalTime.of(23, 0), ten, eight)
        val atSeven = isQuiet(LocalTime.of(7, 59), ten, eight)
        val atEight = isQuiet(eight, ten, eight)
        val atNoon = isQuiet(LocalTime.NOON, ten, eight)
        val sameDay = isQuiet(LocalTime.of(13, 0), LocalTime.of(12, 0), LocalTime.of(14, 0))
        val none = isQuiet(LocalTime.of(3, 0), ten, ten)

        // then
        Assert.assertTrue(atEleven)
        Assert.assertTrue(atSeven)
        Assert.assertFalse(atEight)
        Assert.assertFalse(atNoon)
        Assert.assertTrue(sameDay)
        Assert.assertFalse(none)
    }

    @Test
    fun `a time inside quiet hours waits for them to end`() {
        // given
        val late = on.copy(time = LocalTime.of(23, 30))

        // when
        val time = late.effectiveTime()

        // then
        Assert.assertEquals(LocalTime.of(8, 0), time)
        Assert.assertEquals(LocalTime.of(9, 0), on.effectiveTime())
    }

    @Test
    fun `the next reminder is today's until its time has passed, then tomorrow's`() {
        // given
        val morning = ZonedDateTime.of(LocalDateTime.of(2026, 10, 15, 7, 0), london)
        val afternoon = morning.withHour(15)

        // when
        val fromMorning = nextReminderAt(morning, on)
        val fromAfternoon = nextReminderAt(afternoon, on)

        // then
        Assert.assertEquals(ZonedDateTime.of(LocalDateTime.of(2026, 10, 15, 9, 0), london), fromMorning)
        Assert.assertEquals(ZonedDateTime.of(LocalDateTime.of(2026, 10, 16, 9, 0), london), fromAfternoon)
    }

    @Test
    fun `across a clock change the reminder keeps its time of day, and a time that doesn't exist moves on`() {
        // given: the clocks go forward at 01:00 on 28 Mar 2027, and back at 02:00 on 31 Oct 2027
        val beforeSpring = ZonedDateTime.of(LocalDateTime.of(2027, 3, 27, 12, 0), london)
        val beforeAutumn = ZonedDateTime.of(LocalDateTime.of(2027, 10, 30, 12, 0), london)
        val missing = on.copy(time = LocalTime.of(1, 30), quietStart = LocalTime.MIDNIGHT, quietEnd = LocalTime.MIDNIGHT)

        // when
        val spring = nextReminderAt(beforeSpring, on)
        val autumn = nextReminderAt(beforeAutumn, on)
        val skipped = nextReminderAt(beforeSpring, missing)

        // then
        Assert.assertEquals(LocalTime.of(9, 0), spring.toLocalTime())
        Assert.assertEquals(Duration.ofHours(20), Duration.between(beforeSpring, spring))
        Assert.assertEquals(Duration.ofHours(22), Duration.between(beforeAutumn, autumn))
        Assert.assertEquals(LocalTime.of(2, 30), skipped.toLocalTime())
    }

    @Test
    fun `the reminder follows the phone's time zone`() {
        // given
        val tokyo = ZonedDateTime.of(LocalDateTime.of(2026, 10, 15, 8, 0), ZoneId.of("Asia/Tokyo"))

        // when
        val next = nextReminderAt(tokyo, on)

        // then
        Assert.assertEquals(ZonedDateTime.of(LocalDateTime.of(2026, 10, 15, 9, 0), ZoneId.of("Asia/Tokyo")), next)
    }

    @Test
    fun `a due reminder posts only when it's on, allowed, on time, unfound and in the background`() {
        // given
        val due = ZonedDateTime.of(LocalDateTime.of(2026, 10, 15, 9, 5), london)

        // when
        fun decide(
            settings: ReminderSettings = on,
            now: ZonedDateTime = due,
            allowed: Boolean = true,
            found: Boolean = false,
            foreground: Boolean = false,
        ) = decideReminder(now, settings, allowed, found, foreground)

        // then
        Assert.assertEquals(ReminderDecision.Post, decide())
        Assert.assertEquals(ReminderDecision.Off, decide(settings = on.copy(enabled = false)))
        Assert.assertEquals(ReminderDecision.NotAllowed, decide(allowed = false))
        Assert.assertEquals(ReminderDecision.FoundToday, decide(found = true))
        Assert.assertEquals(ReminderDecision.InForeground, decide(foreground = true))
        Assert.assertEquals(ReminderDecision.OutOfWindow, decide(now = due.withHour(8)))
        Assert.assertEquals(ReminderDecision.OutOfWindow, decide(now = due.withHour(11).withMinute(0)))
    }

    @Test
    fun `a reminder pushed to the end of quiet hours goes out then, and never inside them`() {
        // given
        val late = on.copy(time = LocalTime.of(23, 30))
        val atEight = ZonedDateTime.of(LocalDateTime.of(2026, 10, 16, 8, 1), london)
        val atMidnight = ZonedDateTime.of(LocalDateTime.of(2026, 10, 15, 23, 31), london)

        // when
        val morning = decideReminder(atEight, late, notificationsAllowed = true, foundToday = false, inForeground = false)
        val night = decideReminder(atMidnight, late, notificationsAllowed = true, foundToday = false, inForeground = false)

        // then
        Assert.assertEquals(ReminderDecision.Post, morning)
        Assert.assertEquals(ReminderDecision.OutOfWindow, night)
    }

    @Test
    fun `the app offers reminders unless they work already, or Android's prompt was refused this week`() {
        // given
        val now = Instant.parse("2026-10-15T09:00:00Z")
        val refused = ReminderSettings(deniedAt = now.minus(Duration.ofDays(6)))

        // when
        val off = ReminderSettings().mayOffer(now, notificationsAllowed = false)
        val working = on.mayOffer(now, notificationsAllowed = true)
        val blockedInSettings = on.mayOffer(now, notificationsAllowed = false)
        val paused = refused.mayOffer(now, notificationsAllowed = false)
        val pauseOver = refused.mayOffer(now.plus(Duration.ofDays(1)), notificationsAllowed = false)

        // then
        Assert.assertTrue(off)
        Assert.assertFalse(working)
        Assert.assertTrue(blockedInSettings)
        Assert.assertFalse(paused)
        Assert.assertTrue(pauseOver)
    }
}
