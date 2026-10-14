package com.bottlecast.app.testdi

import com.bottlecast.app.common.preferences.IOnboardingStore
import com.bottlecast.app.common.preferences.di.PreferencesModule
import com.bottlecast.app.fakes.FakeOnboardingStore
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

// Each test builds its own components, and a second DataStore on the same file would throw, so the
// phone's preferences stay out of instrumented tests.
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [PreferencesModule::class]
)
interface FakePreferencesModule {

    @Binds
    fun bindOnboardingStore(fakeStore: FakeOnboardingStore): IOnboardingStore
}
