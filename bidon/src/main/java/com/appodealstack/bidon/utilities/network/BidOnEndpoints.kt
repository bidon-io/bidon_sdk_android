package com.appodealstack.bidon.utilities.network

/**
 * Endpoint Manager
 */
interface BidOnEndpoints {
    val activeEndpoint: String

    fun init(defaultBaseUrl: String, loadedUrls: Set<String>)
    fun popNextEndpoint(): String?
}
