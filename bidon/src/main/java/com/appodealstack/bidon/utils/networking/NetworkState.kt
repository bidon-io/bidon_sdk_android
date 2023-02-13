package com.appodealstack.bidon.utils.networking

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
enum class NetworkState {
    Enabled,
    Disabled,

    /**
     * [NetworkStatus.init()] was not invoked
     */
    NotInitialized,

    /**
     * SecurityException at android.net.IConnectivityManager$Stub$Proxy.listenForNetwork(IConnectivityManager.java:4703)
     * for Samsung devices with Android 11
     */
    ConnectivityManagerError
}
