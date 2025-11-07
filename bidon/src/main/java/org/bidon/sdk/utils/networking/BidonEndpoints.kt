package org.bidon.sdk.utils.networking

/**
 * Created by Bidon Team on 06/02/2023.
 *
 * Endpoint Manager
 */
internal interface BidonEndpoints {
    val activeEndpoint: String

    fun init(defaultBaseUrl: String, loadedUrls: Set<String>)
    fun popNextEndpoint(): String?
}
