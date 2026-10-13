package com.bottlecast.app.feature.reveal.domain

import org.junit.Assert
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Unit tests for [castAgo], the byline's "cast 3 days ago", counted in local calendar days. */
class CastAgoTest {

    private val london = ZoneId.of("Europe/London")
    private val now = ZonedDateTime.of(2026, 10, 13, 9, 0, 0, 0, london)

    @Test
    fun `earlier today, and late yesterday, by the local calendar`() {
        Assert.assertEquals(CastAgo.Today, castAgo(instant("2026-10-13T00:30:00+01:00"), now))
        Assert.assertEquals(CastAgo.Yesterday, castAgo(instant("2026-10-12T23:30:00+01:00"), now))
    }

    @Test
    fun `days, then months, then years`() {
        Assert.assertEquals(CastAgo.Days(3), castAgo(instant("2026-10-10T12:00:00+01:00"), now))
        Assert.assertEquals(CastAgo.Days(29), castAgo(instant("2026-09-14T12:00:00+01:00"), now))
        Assert.assertEquals(CastAgo.Months(1), castAgo(instant("2026-09-13T12:00:00+01:00"), now))
        Assert.assertEquals(CastAgo.Months(11), castAgo(instant("2025-10-14T12:00:00+01:00"), now))
        Assert.assertEquals(CastAgo.Years(3), castAgo(instant("2023-10-11T12:00:00+01:00"), now))
    }

    @Test
    fun `a bottle cast a moment from now, by a fast clock, was cast today`() {
        Assert.assertEquals(CastAgo.Today, castAgo(instant("2026-10-13T09:05:00+01:00"), now))
    }

    @Test
    fun `it counts the reader's days, wherever the writer was`() {
        // 23:30 in London is already tomorrow in Tokyo
        val castAt = instant("2026-10-12T23:30:00+01:00")
        val tokyo = now.withZoneSameInstant(ZoneId.of("Asia/Tokyo"))

        Assert.assertEquals(CastAgo.Today, castAgo(castAt, tokyo))
    }

    private fun instant(text: String): Instant = ZonedDateTime.parse(text).toInstant()
}
