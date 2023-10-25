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
    val auctionConfigurationUid: String?,
    val externalWinNotificationsEnabled: Boolean,
)

internal class AuctionResponseParser : JsonParser<AuctionResponse> {
    override fun parseOrNull(jsonString: String): AuctionResponse? = runCatching {
        val json = JSONObject(jsonString)
        AuctionResponse(
            rounds = JsonParsers.parseList(json.optJSONArray("rounds")),
            auctionId = json.getString("auction_id"),
            pricefloor = json.optDouble("pricefloor"),
            auctionConfigurationUid = json.optString("auction_configuration_uid"),
            adUnits = JsonParsers.parseList(json.optJSONArray("ad_units")),
            token = json.optString("token"),
            externalWinNotificationsEnabled = json.optBoolean("external_win_notifications", false),
        )
    }.getOrNull()
}
