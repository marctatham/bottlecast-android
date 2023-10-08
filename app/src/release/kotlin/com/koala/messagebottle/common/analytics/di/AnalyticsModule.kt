package com.bottlecast.app.common.analytics.di

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.bottlecast.app.analytics.FirebaseTracker
import com.bottlecast.app.analytics.ITracker
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
    ): ITracker = FirebaseTracker(firebaseAnalytics)

}
