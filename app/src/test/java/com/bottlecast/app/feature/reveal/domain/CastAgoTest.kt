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
        // given
        val earlierToday = instant("2026-10-13T00:30:00+01:00")
        val lateYesterday = instant("2026-10-12T23:30:00+01:00")

        // when
        val today = castAgo(earlierToday, now)
        val yesterday = castAgo(lateYesterday, now)

        // then
        Assert.assertEquals(CastAgo.Today, today)
        Assert.assertEquals(CastAgo.Yesterday, yesterday)
    }

    @Test
    fun `days, then months, then years`() {
        // given
        val casts = listOf(
            "2026-10-10T12:00:00+01:00" to CastAgo.Days(3),
            "2026-09-14T12:00:00+01:00" to CastAgo.Days(29),
            "2026-09-13T12:00:00+01:00" to CastAgo.Months(1),
            "2025-10-14T12:00:00+01:00" to CastAgo.Months(11),
            "2023-10-11T12:00:00+01:00" to CastAgo.Years(3),
        )

        // when
        val ages = casts.map { (castAt, _) -> castAgo(instant(castAt), now) }

        // then
        Assert.assertEquals(casts.map { it.second }, ages)
    }

    @Test
    fun `a bottle cast a moment from now, by a fast clock, was cast today`() {
        // given
        val castAt = instant("2026-10-13T09:05:00+01:00")

        // when
        val age = castAgo(castAt, now)

        // then
        Assert.assertEquals(CastAgo.Today, age)
    }

    @Test
    fun `it counts the reader's days, wherever the writer was`() {
        // given: 23:30 in London is already tomorrow in Tokyo
        val castAt = instant("2026-10-12T23:30:00+01:00")
        val tokyo = now.withZoneSameInstant(ZoneId.of("Asia/Tokyo"))

        // when
        val age = castAgo(castAt, tokyo)

        // then
        Assert.assertEquals(CastAgo.Today, age)
    }

    private fun instant(text: String): Instant = ZonedDateTime.parse(text).toInstant()
}
