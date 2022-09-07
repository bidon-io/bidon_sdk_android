package com.appodealstack.bidon.data.networking

/**
 * Endpoint Manager
 */
interface BidOnEndpoints {
    val activeEndpoint: String

    fun init(defaultBaseUrl: String, loadedUrls: Set<String>)
    fun popNextEndpoint(): String?
}
