package com.appodealstack.bidon.data.networking

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
