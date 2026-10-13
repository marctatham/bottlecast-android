package com.bottlecast.app.fakes

import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stands in for the phone's network: online unless a test says otherwise, so instrumented tests
 * don't depend on the emulator's connection.
 */
@Singleton
class FakeConnectivityMonitor @Inject constructor() : IConnectivityMonitor {

    override val isOnline: MutableStateFlow<Boolean> = MutableStateFlow(true)
}
