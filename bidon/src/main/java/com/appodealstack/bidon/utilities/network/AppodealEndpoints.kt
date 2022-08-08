package com.appodealstack.bidon.utilities.network

import com.appodealstack.bidon.utilities.network.endpoint.AppodealEndpointImpl

/**
 * Endpoint Manager
 */
object AppodealEndpoints : AppodealEndpoint by AppodealEndpointImpl()

interface AppodealEndpoint {
    val activeEndpoint: String

    fun init(defaultBaseUrl: String, loadedUrls: Set<String>)
    fun popNextEndpoint(): String?
}