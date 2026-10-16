package com.bottlecast.app.common.analytics.di

import com.bottlecast.app.common.analytics.DebugTracker
import com.bottlecast.app.common.analytics.FirebaseTracker
import com.bottlecast.app.common.analytics.ITracker
import com.bottlecast.app.common.analytics.LoggingTracker
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AnalyticsModule {

    @Provides
    @Singleton
    fun providesAnalyticsProvider(
        firebaseAnalytics: FirebaseAnalytics
    ): ITracker {
        val firebaseTracker = FirebaseTracker(firebaseAnalytics)
        val loggingTracker = LoggingTracker()
        return DebugTracker(
            firebaseTracker,
            loggingTracker
        )
    }
}
