package com.appodealstack.bidon.auction.models

import com.appodealstack.bidon.utils.json.JsonParser
import com.appodealstack.bidon.utils.json.JsonParsers
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal data class AuctionResponse(
    val rounds: List<Round>?,
    val lineItems: List<LineItem>?,
    val minPrice: Double?,
    val fillTimeout: Long?,
    val token: String?,
    val auctionId: String?,
    val auctionConfigurationId: Int?,
)

internal class AuctionResponseParser : JsonParser<AuctionResponse> {
    override fun parseOrNull(jsonString: String): AuctionResponse? = runCatching {
        val json = JSONObject(jsonString)
        AuctionResponse(
            rounds = JsonParsers.parseList(json.optJSONArray("rounds")),
            auctionId = json.optString("auction_id"),
            minPrice = json.optDouble("min_price"),
            auctionConfigurationId = json.optInt("auction_configuration_id"),
            fillTimeout = json.optLong("fill_timeout").takeIf { json.has("fill_timeout") },
            lineItems = JsonParsers.parseList(json.optJSONArray("line_items")),
            token = json.optString("token")
        )
    }.getOrNull()
}
