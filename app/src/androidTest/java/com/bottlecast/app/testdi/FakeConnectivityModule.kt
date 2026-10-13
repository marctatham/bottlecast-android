package com.bottlecast.app.testdi

import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.connectivity.di.ConnectivityModule
import com.bottlecast.app.fakes.FakeConnectivityMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

// keeps instrumented tests independent of the emulator's network
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [ConnectivityModule::class]
)
interface FakeConnectivityModule {

    @Binds
    fun bindConnectivityMonitor(fakeMonitor: FakeConnectivityMonitor): IConnectivityMonitor
}
