package com.bottlecast.app.common.time.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.time.TimeSource

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {

    /** The wall clock in the phone's time zone, followed as it changes, for local dates and tides. */
    @Provides
    fun providesClock(): Clock = SystemZoneClock

    /** A monotonic clock, for durations such as how long the bottle has been held. */
    @Provides
    fun providesTimeSource(): TimeSource = TimeSource.Monotonic
}

/**
 * The system clock in whatever time zone the phone is in now. `Clock.systemDefaultZone()` keeps the zone
 * it was created in, so the tide would miss a time-zone change while the app runs.
 */
object SystemZoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = Instant.now()
}
