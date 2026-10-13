package com.bottlecast.app.common.connectivity

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Follows the default network for the app's lifetime. Online means the network offers internet;
 * it doesn't wait for Android to validate it, so a captive portal or a network that blocks the
 * check doesn't disable the app's actions (Firestore reports its own failures).
 */
class ConnectivityMonitor(connectivityManager: ConnectivityManager) : IConnectivityMonitor {

    private val _isOnline: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // whether the callback has reported yet; once it has, it knows better than the starting value
    private var reported = false

    init {
        connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                report(capabilities.hasInternet())
            }

            override fun onLost(network: Network) {
                report(false)
            }
        })
        // No callback arrives while there's no network at all, so the starting value is read as
        // well. Reading it after registering means a change in between still reaches the callback.
        val online = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork).hasInternet()
        synchronized(this) {
            if (!reported) _isOnline.value = online
        }
    }

    // callbacks arrive on the connectivity thread
    private fun report(online: Boolean) = synchronized(this) {
        reported = true
        _isOnline.value = online
    }

    private fun NetworkCapabilities?.hasInternet(): Boolean =
        this?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}
