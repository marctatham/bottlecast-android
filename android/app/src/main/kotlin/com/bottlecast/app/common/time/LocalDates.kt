package com.bottlecast.app.common.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

// wakes a moment after midnight, so the new date has surely begun
private const val MIDNIGHT_SLACK_MILLIS = 1_000L

/** The local date now, then again just after each local midnight, in whatever zone the clock is in then. */
fun Clock.localDates(): Flow<LocalDate> = flow {
    while (true) {
        val now = ZonedDateTime.now(this@localDates)
        emit(now.toLocalDate())
        val nextDay = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        delay(Duration.between(now, nextDay).toMillis() + MIDNIGHT_SLACK_MILLIS)
    }
}.distinctUntilChanged()
