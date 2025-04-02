package org.bidon.sdk.auction.models

import org.bidon.sdk.utils.json.JsonParser
import org.bidon.sdk.utils.json.JsonParsers
import org.json.JSONObject

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal data class AuctionResponse(
    val adUnits: List<AdUnit>?,
    val noBids: List<AdUnit>?,
    val pricefloor: Double,
    val auctionId: String,
    val auctionTimeout: Long,
    val auctionConfigurationId: Long?,
    val auctionConfigurationUid: String?,
    val externalWinNotificationsEnabled: Boolean,
)

internal class AuctionResponseParser : JsonParser<AuctionResponse> {
    override fun parseOrNull(jsonString: String): AuctionResponse? = runCatching {
        val json = JSONObject(jsonString)
        AuctionResponse(
            adUnits = JsonParsers.parseList(json.optJSONArray("ad_units")),
            noBids = JsonParsers.parseList(json.optJSONArray("no_bids")),
            pricefloor = json.optDouble("auction_pricefloor"),
            auctionId = json.getString("auction_id"),
            auctionTimeout = json.optLong("auction_timeout", auctionTimeoutDefault),
            auctionConfigurationId = json.optLong("auction_configuration_id"),
            auctionConfigurationUid = json.optString("auction_configuration_uid"),
            externalWinNotificationsEnabled = json.optBoolean("external_win_notifications", false),
        )
    }.getOrNull()
}

private const val auctionTimeoutDefault = 30_000L