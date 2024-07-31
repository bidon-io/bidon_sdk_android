package org.bidon.sdk.bidding

import org.json.JSONObject

internal interface BiddingConfig {
    val tokenTimeout: Long
}

internal interface BiddingConfigSynchronizer {
    fun parse(rootJsonResponse: String)
}

internal class BiddingConfigImpl(
    override var tokenTimeout: Long = tokenTimeoutDefault
) : BiddingConfig, BiddingConfigSynchronizer {
    override fun parse(rootJsonResponse: String) {
        runCatching {
            tokenTimeout = JSONObject(rootJsonResponse)
                .optJSONObject("bidding")
                ?.optLong("token_timeout_ms", tokenTimeoutDefault)
                ?: tokenTimeoutDefault
        }
    }
}

private const val tokenTimeoutDefault = 10_000L
