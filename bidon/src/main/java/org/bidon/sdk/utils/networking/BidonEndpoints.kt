package org.bidon.sdk.utils.networking

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 *
 * Endpoint Manager
 */
interface BidonEndpoints {
    val activeEndpoint: String

    fun init(defaultBaseUrl: String, loadedUrls: Set<String>)
    fun popNextEndpoint(): String?
}
