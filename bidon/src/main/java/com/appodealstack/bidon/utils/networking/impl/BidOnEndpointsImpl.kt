package com.appodealstack.bidon.utils.networking.impl

import com.appodealstack.bidon.utils.networking.BidOnEndpoints
import com.appodealstack.bidon.utils.networking.NetworkSettings
import java.util.*

/**
 * Created by Aleksei Cherniaev on 07/02/2023.
 */
internal class BidOnEndpointsImpl : BidOnEndpoints {
    private val hosts: Queue<String> = LinkedList()
    private var defaultEndpoint: String = NetworkSettings.BidOnBaseUrl

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