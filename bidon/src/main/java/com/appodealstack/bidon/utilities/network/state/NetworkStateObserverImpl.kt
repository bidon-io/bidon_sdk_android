package com.appodealstack.bidon.utilities.network.state

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import com.appodealstack.bidon.utilities.network.NetworkState
import com.appodealstack.bidon.utilities.network.NetworkStateObserver
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow

internal class NetworkStateObserverImpl : NetworkStateObserver {
    private var connectivityManager: ConnectivityManager? = null
    private val listeners = Collections.synchronizedSet(
        mutableSetOf<NetworkStateObserver.ConnectionListener>()
    )

    override val networkStateFlow = MutableStateFlow(NetworkState.NotInitialized)

    override fun init(applicationContext: Context) {
        if (networkStateFlow.value != NetworkState.NotInitialized) return

        val connectivityManager =
            (applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                .also { this.connectivityManager = it } ?: return

        networkStateFlow.value = NetworkState.Disabled

        val callback: ConnectivityManager.NetworkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                syncState()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                syncState()
            }

            override fun onUnavailable() {
                super.onUnavailable()
                syncState()
            }
        }
        try {
            connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
        } catch (e: Throwable) {
            networkStateFlow.value = NetworkState.ConnectivityManagerError
        }
    }

    override fun isConnected(): Boolean = connectivityManager?.activeNetworkInfo?.isConnected == true

    override fun subscribe(listener: NetworkStateObserver.ConnectionListener) {
        listeners.add(listener)
    }

    override fun unsubscribe(listener: NetworkStateObserver.ConnectionListener) {
        listeners.remove(listener)
    }

    private fun syncState() {
        networkStateFlow.value = if (isConnected()) {
            listeners.forEach {
                it.onConnectionUpdated(isConnected = true)
            }
            NetworkState.Enabled
        } else {
            listeners.forEach {
                it.onConnectionUpdated(isConnected = false)
            }
            NetworkState.Disabled
        }
    }

    inner class NetworkReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            syncState()
        }
    }
}