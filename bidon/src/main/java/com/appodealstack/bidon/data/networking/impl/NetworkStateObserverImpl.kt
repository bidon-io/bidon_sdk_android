package com.appodealstack.bidon.data.networking.impl

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import com.appodealstack.bidon.data.networking.NetworkState
import com.appodealstack.bidon.data.networking.NetworkStateObserver
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class NetworkStateObserverImpl : NetworkStateObserver {
    private var connectivityManager: ConnectivityManager? = null
    private var instantlyIsConnected = AtomicBoolean(false)
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

    override fun isConnected(): Boolean = instantlyIsConnected.get()

    override fun subscribe(listener: NetworkStateObserver.ConnectionListener) {
        listeners.add(listener)
    }

    override fun unsubscribe(listener: NetworkStateObserver.ConnectionListener) {
        listeners.remove(listener)
    }

    private fun syncState() {
        networkStateFlow.value = if (checkConnected()) {
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

    private fun checkConnected(): Boolean {
        @Suppress("DEPRECATION")
        return (connectivityManager?.activeNetworkInfo?.isConnected == true).also {
            instantlyIsConnected.set(it)
        }
    }

    inner class NetworkReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            syncState()
        }
    }
}
