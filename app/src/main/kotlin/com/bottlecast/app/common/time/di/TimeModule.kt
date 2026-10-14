package com.bottlecast.app.common.time.di

import com.bottlecast.app.common.time.SystemZoneClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
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
