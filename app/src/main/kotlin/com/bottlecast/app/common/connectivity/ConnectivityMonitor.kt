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
class ConnectivityMonitor(private val connectivityManager: ConnectivityManager) : IConnectivityMonitor {

    // no callback arrives while there's no network at all, so read the starting value up front
    private val _isOnline: MutableStateFlow<Boolean> = MutableStateFlow(
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork).hasInternet()
    )
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                _isOnline.value = capabilities.hasInternet()
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
            }
        })
    }

    private fun NetworkCapabilities?.hasInternet(): Boolean =
        this?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}
