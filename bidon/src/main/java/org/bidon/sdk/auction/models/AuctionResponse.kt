package org.bidon.sdk.auction.models

import org.bidon.sdk.utils.json.JsonParser
import org.bidon.sdk.utils.json.JsonParsers
import org.json.JSONObject

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal data class AuctionResponse(
    val rounds: List<RoundRequest>?,
    val adUnits: List<AdUnit>?,
    val pricefloor: Double?,
    val token: String?,
    val auctionId: String,
    val auctionConfigurationId: Long?,
    val auctionConfigurationUid: String?,
    val externalWinNotificationsEnabled: Boolean,
)

internal class AuctionResponseParser : JsonParser<AuctionResponse> {
    override fun parseOrNull(jsonString: String): AuctionResponse? = runCatching {
        val json = JSONObject(jsonString)
        AuctionResponse(
            rounds = JsonParsers.parseList(json.optJSONArray("rounds")),
            adUnits = JsonParsers.parseList(json.optJSONArray("ad_units")),
            pricefloor = json.optDouble("pricefloor"),
            token = json.optString("token"),
            auctionId = json.getString("auction_id"),
            auctionConfigurationId = json.optLong("auction_configuration_id"),
            auctionConfigurationUid = json.optString("auction_configuration_uid"),
            externalWinNotificationsEnabled = json.optBoolean("external_win_notifications", false),
        )
    }.getOrNull()
}
