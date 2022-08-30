package com.appodealstack.bidon.utilities.network

/**
 * Marker interface shows should use request signature (header with unique request id)
 */
internal interface UniqueRequest

/**
 * It determines if the request ables to retry if an error occurs
 */
internal interface Retriable {
    val isRetryEnabled: Boolean
}

/**
 * If request should keep cached value
 */
internal interface Cacheable<T> {
    suspend fun getCachedValue(): T?
    suspend fun saveCachedValue(value: T?)
}

internal interface UrlOverridable {
    val overriddenUrl: String
}
