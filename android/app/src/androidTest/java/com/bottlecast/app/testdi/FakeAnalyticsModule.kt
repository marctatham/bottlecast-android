package com.bottlecast.app.testdi

import com.bottlecast.app.common.analytics.ITracker
import com.bottlecast.app.common.analytics.di.AnalyticsModule
import com.bottlecast.app.fakes.FakeTracker
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AnalyticsModule::class]
)
interface FakeAnalyticsModule {

    @Binds
    fun bindTracker(fakeTracker: FakeTracker): ITracker
}
