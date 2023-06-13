package org.bidon.sdk.utils.networking.impl

import org.bidon.sdk.utils.networking.BidonEndpoints
import org.bidon.sdk.utils.networking.NetworkSettings
import java.util.*

/**
 * Created by Bidon Team on 07/02/2023.
 */
internal class BidonEndpointsImpl : BidonEndpoints {
    private val hosts: Queue<String> = LinkedList()
    private var defaultEndpoint: String = NetworkSettings.BidonBaseUrl

    override val activeEndpoint: String get() = hosts.peek() ?: defaultEndpoint

    override fun init(defaultBaseUrl: String, loadedUrls: Set<String>) {
        this.defaultEndpoint = defaultBaseUrl
        hosts.add(defaultBaseUrl)
        hosts.addAll(loadedUrls.distinct())
    }

    override fun popNextEndpoint(): String? {
        hosts.poll()
        return hosts.peek()
    }
}