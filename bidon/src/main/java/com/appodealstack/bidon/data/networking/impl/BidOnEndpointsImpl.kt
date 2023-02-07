package com.appodealstack.bidon.data.networking.impl

import com.appodealstack.bidon.data.networking.BidOnEndpoints
import com.appodealstack.bidon.data.networking.NetworkSettings
import com.appodealstack.bidon.domain.common.ext.toHexString
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
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
        list.add("https://b.${getCryptoEndpoint(year)}.com")
        list.add("https://b.${getCryptoEndpoint(month)}.com")
        list.add("https://b.${getCryptoEndpoint(week)}.com")
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
