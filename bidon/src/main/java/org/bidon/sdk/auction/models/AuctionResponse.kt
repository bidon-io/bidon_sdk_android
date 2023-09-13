package org.bidon.sdk.auction.models

import org.bidon.sdk.utils.json.JsonParser
import org.bidon.sdk.utils.json.JsonParsers
import org.json.JSONObject

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal data class AuctionResponse(
    val rounds: List<RoundRequest>?,
    val lineItems: List<LineItem>?,
    val pricefloor: Double?,
    val token: String?,
    val auctionId: String,
    @Deprecated("Use auctionConfigurationUid instead")
    val auctionConfigurationId: Int?,
    val auctionConfigurationUid: ULong?,
    val externalWinNotificationsEnabled: Boolean,
)

internal class AuctionResponseParser : JsonParser<AuctionResponse> {
    override fun parseOrNull(jsonString: String): AuctionResponse? = runCatching {
        val json = JSONObject(jsonString)
        AuctionResponse(
            rounds = JsonParsers.parseList(json.optJSONArray("rounds")),
            auctionId = json.getString("auction_id"),
            pricefloor = json.optDouble("pricefloor"),
            auctionConfigurationId = json.optInt("auction_configuration_id"),
            auctionConfigurationUid = json.optLong("auction_configuration_uid").toULong(),
            lineItems = JsonParsers.parseList(json.optJSONArray("line_items")),
            token = json.optString("token"),
            externalWinNotificationsEnabled = json.optBoolean("external_win_notifications", false),
        )
    }.getOrNull()
}
