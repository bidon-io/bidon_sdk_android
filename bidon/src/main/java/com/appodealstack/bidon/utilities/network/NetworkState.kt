package com.appodealstack.bidon.utilities.network

import android.content.Context
import com.appodealstack.bidon.utilities.network.state.NetworkStateObserverImpl
import kotlinx.coroutines.flow.StateFlow

/**
 * Use [NetworkStatus] to determine current Network State.
 *
 * Example of using:
 *
 * NetworkStatus.networkStateFlow.onEach { networkState->
 *      when(networkState) {
 *          NetworkState.Enabled -> startRequest()
 *          NetworkState.Disabled -> cancelRequest()
 *          NetworkState.NotInitialized,
 *          NetworkState.ConnectivityManagerError -> // do nothing
 *      }
 * }.launchIn(scope)
 *
 * or wait for enabled network using suspend fun [StateFlow].first():
 *
 * 1. NetworkStatus.networkStateFlow.first { it == NetworkState.Enabled }
 * 2. startRequest()
 *
 */
object NetworkStatus : NetworkStateObserver by NetworkStateObserverImpl()

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

interface NetworkStateObserver {
    val networkStateFlow: StateFlow<NetworkState>

    fun isConnected(): Boolean
    fun init(applicationContext: Context)

    /**
     * Use subscribe/unsubscribe only from JAVA-classes. For .kt use flow [networkStateFlow].
     */
    fun subscribe(listener: ConnectionListener)
    fun unsubscribe(listener: ConnectionListener)

    interface ConnectionListener {
        fun onConnectionUpdated(isConnected: Boolean)
    }
}