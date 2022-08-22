package com.appodealstack.bidon.utilities.network.endpoint

import com.appodealstack.bidon.core.ext.toHexString
import com.appodealstack.bidon.utilities.network.BidOnEndpoints
import com.appodealstack.bidon.utilities.network.NetworkSettings
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.*

internal class BidOnEndpointsImpl : BidOnEndpoints {
    private val hosts: Queue<String> = LinkedList()
    private var defaultEndpoint: String = NetworkSettings.BaseBidOnUrl

    override val activeEndpoint: String get() = hosts.peek() ?: defaultEndpoint

    override fun init(defaultBaseUrl: String, loadedUrls: Set<String>) {
        this.defaultEndpoint = defaultBaseUrl
        val result = loadedUrls + getGeneratedHostList()
        hosts.add(defaultBaseUrl)
        hosts.addAll(result.distinct())
    }

    override fun popNextEndpoint(): String? {
        hosts.poll()
        return hosts.peek()
    }

    private fun getGeneratedHostList(): List<String> {
        val now = Date()
        val list = mutableListOf<String>()
        val year = SimpleDateFormat("yyyy", Locale.ENGLISH).format(now)
        val month = SimpleDateFormat("yyyyMM", Locale.ENGLISH).format(now)
        val week = SimpleDateFormat("yyyyMMww", Locale.ENGLISH).format(now)
        list.add("https://a.${getCryptoEndpoint(year)}.com")
        list.add("https://a.${getCryptoEndpoint(month)}.com")
        list.add("https://a.${getCryptoEndpoint(week)}.com")
        return list
    }

    private fun getCryptoEndpoint(base: String): String {
        val hashEndpoint = getHashAlgorithm(bytes = base.toByteArray())
        return hashEndpoint?.toHexString() ?: "appbaqend"
    }

    private fun getHashAlgorithm(algorithm: String = "SHA-224", bytes: ByteArray): ByteArray? {
        return try {
            val digest = MessageDigest.getInstance(algorithm)
            digest.update(bytes)
            digest.digest()
        } catch (e: NoSuchAlgorithmException) {
            null
        }
    }
}
