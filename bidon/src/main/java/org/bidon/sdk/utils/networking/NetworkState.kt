package org.bidon.sdk.utils.networking

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal enum class NetworkState {
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
