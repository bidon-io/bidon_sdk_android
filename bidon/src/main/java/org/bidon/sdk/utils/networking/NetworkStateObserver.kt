package org.bidon.sdk.utils.networking

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * Created by Bidon Team on 06/02/2023.
 *
 *
 * Use [NetworkStateObserver.networkStateFlow] to determine current Network State.
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