package com.bottlecast.app.feature.shore.domain

import org.junit.Assert
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The local-calendar tide (spec §3.3): a new bottle at local midnight, and the countdown's formats.
 */
class TidesTest {

    @Test
    fun `countdown shows hours and minutes from an hour up`() {
        // given
        val untilNextTide = Duration.ofHours(3).plusMinutes(12).plusSeconds(40)

        // when
        val countdown = countdownFor(untilNextTide)

        // then
        Assert.assertEquals(TideCountdown.HoursMinutes(hours = 3, minutes = 12), countdown)
    }

    @Test
    fun `countdown shows minutes under an hour`() {
        // given
        val untilNextTide = Duration.ofMinutes(59).plusSeconds(59)

        // when
        val countdown = countdownFor(untilNextTide)

        // then
        Assert.assertEquals(TideCountdown.Minutes(minutes = 59), countdown)
    }

    @Test
    fun `countdown shows the last full minute`() {
        // given
        val untilNextTide = Duration.ofMinutes(1)

        // when
        val countdown = countdownFor(untilNextTide)

        // then
        Assert.assertEquals(TideCountdown.Minutes(minutes = 1), countdown)
    }

    @Test
    fun `countdown says any moment under a minute`() {
        // given
        val untilNextTide = Duration.ofSeconds(59)

        // when
        val countdown = countdownFor(untilNextTide)

        // then
        Assert.assertEquals(TideCountdown.AnyMoment, countdown)
    }

    @Test
    fun `countdown says any moment at midnight`() {
        // given
        val untilNextTide = Duration.ZERO

        // when
        val countdown = countdownFor(untilNextTide)

        // then
        Assert.assertEquals(TideCountdown.AnyMoment, countdown)
    }

    @Test
    fun `at midnight a whole day lies ahead`() {
        // given
        val now = ZonedDateTime.of(2026, 10, 13, 0, 0, 0, 0, LONDON)

        // when
        val tide = tideAt(now)

        // then
        Assert.assertEquals(Tide(untilNextTide = Duration.ofHours(24), dayElapsed = 0f), tide)
    }

    @Test
    fun `at noon half the day has passed`() {
        // given
        val now = ZonedDateTime.of(2026, 10, 13, 12, 0, 0, 0, LONDON)

        // when
        val tide = tideAt(now)

        // then
        Assert.assertEquals(Tide(untilNextTide = Duration.ofHours(12), dayElapsed = 0.5f), tide)
    }

    @Test
    fun `a minute before midnight the next tide is a minute away`() {
        // given
        val now = ZonedDateTime.of(2026, 10, 13, 23, 59, 0, 0, LONDON)

        // when
        val tide = tideAt(now)

        // then
        Assert.assertEquals(Duration.ofMinutes(1), tide.untilNextTide)
        Assert.assertEquals(1439f / 1440f, tide.dayElapsed, DELTA)
    }

    @Test
    fun `on the 23-hour day the day's real length counts`() {
        // given: the clocks go forward at 01:00, so noon is 11 hours into a 23-hour day
        val now = ZonedDateTime.of(2026, 3, 29, 12, 0, 0, 0, LONDON)

        // when
        val tide = tideAt(now)

        // then
        Assert.assertEquals(Duration.ofHours(12), tide.untilNextTide)
        Assert.assertEquals(11f / 23f, tide.dayElapsed, DELTA)
    }

    @Test
    fun `on the 25-hour day the day's real length counts`() {
        // given: the clocks go back at 02:00, so noon is 13 hours into a 25-hour day
        val now = ZonedDateTime.of(2026, 10, 25, 12, 0, 0, 0, LONDON)

        // when
        val tide = tideAt(now)

        // then
        Assert.assertEquals(Duration.ofHours(12), tide.untilNextTide)
        Assert.assertEquals(13f / 25f, tide.dayElapsed, DELTA)
    }

    private companion object {
        val LONDON: ZoneId = ZoneId.of("Europe/London")
        const val DELTA = 0.0001f
    }
}
