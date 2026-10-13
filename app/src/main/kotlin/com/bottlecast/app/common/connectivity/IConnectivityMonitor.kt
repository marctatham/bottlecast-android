package com.bottlecast.app.common.connectivity

import kotlinx.coroutines.flow.StateFlow

/** Whether the phone has a network connection, for the offline banners and disabled actions. */
interface IConnectivityMonitor {

    /** True while the default network can reach the internet. */
    val isOnline: StateFlow<Boolean>
}
