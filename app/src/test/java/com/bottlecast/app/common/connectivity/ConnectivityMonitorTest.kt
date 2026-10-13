package com.bottlecast.app.common.connectivity

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verifyOrder
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities

// Robolectric, for real NetworkCallback and NetworkCapabilities classes
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], application = Application::class)
class ConnectivityMonitorTest {

    private val network = ShadowNetwork.newInstance(1)
    private val internet = ShadowNetworkCapabilities.newInstance().also {
        shadowOf(it).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    private val noInternet = ShadowNetworkCapabilities.newInstance()
    private val callback = slot<ConnectivityManager.NetworkCallback>()
    private val connectivityManager = mockk<ConnectivityManager> {
        every { registerDefaultNetworkCallback(capture(callback)) } just Runs
        every { activeNetwork } returns network
        every { getNetworkCapabilities(network) } returns internet
    }

    @Test
    fun `starts online when the default network offers internet`() {
        // given
        // when
        val monitor = ConnectivityMonitor(connectivityManager)

        // then
        Assert.assertTrue(monitor.isOnline.value)
    }

    @Test
    fun `starts offline when there's no network`() {
        // given
        every { connectivityManager.activeNetwork } returns null
        every { connectivityManager.getNetworkCapabilities(null) } returns null

        // when
        val monitor = ConnectivityMonitor(connectivityManager)

        // then
        Assert.assertFalse(monitor.isOnline.value)
    }

    @Test
    fun `goes offline when the default network is lost`() {
        // given
        val monitor = ConnectivityMonitor(connectivityManager)

        // when
        callback.captured.onLost(network)

        // then
        Assert.assertFalse(monitor.isOnline.value)
    }

    @Test
    fun `goes offline when the default network stops offering internet`() {
        // given
        val monitor = ConnectivityMonitor(connectivityManager)

        // when
        callback.captured.onCapabilitiesChanged(network, noInternet)

        // then
        Assert.assertFalse(monitor.isOnline.value)
    }

    @Test
    fun `listens before reading the starting value, so no change can fall in between`() {
        // given
        // when
        ConnectivityMonitor(connectivityManager)

        // then
        verifyOrder {
            connectivityManager.registerDefaultNetworkCallback(any())
            connectivityManager.getNetworkCapabilities(any())
        }
    }

    @Test
    fun `a network that arrives while the starting value is read isn't overwritten by it`() {
        // given
        every { connectivityManager.activeNetwork } returns null
        every { connectivityManager.getNetworkCapabilities(null) } answers {
            callback.captured.onCapabilitiesChanged(network, internet)
            null
        }

        // when
        val monitor = ConnectivityMonitor(connectivityManager)

        // then
        Assert.assertTrue(monitor.isOnline.value)
    }
}
