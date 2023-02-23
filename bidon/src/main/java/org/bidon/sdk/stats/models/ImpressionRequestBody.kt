package org.bidon.sdk.stats.models

import org.bidon.sdk.auction.models.BannerRequestBody
import org.bidon.sdk.auction.models.InterstitialRequestBody
import org.bidon.sdk.auction.models.RewardedRequestBody
import org.bidon.sdk.utils.json.JsonParsers
import org.bidon.sdk.utils.json.JsonSerializer
import org.bidon.sdk.utils.json.jsonObject
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal data class ImpressionRequestBody(
    val auctionId: String,
    val auctionConfigurationId: Int,
    val impressionId: String,
    val demandId: String,
    val adUnitId: String?,
    val ecpm: Double,
    val banner: BannerRequestBody?,
    val interstitial: InterstitialRequestBody?,
    val rewarded: RewardedRequestBody?,
)

internal class ImpressionRequestBodySerializer : JsonSerializer<ImpressionRequestBody> {
    override fun serialize(data: ImpressionRequestBody): JSONObject {
        return jsonObject {
            "auction_id" hasValue data.auctionId
            "auction_configuration_id" hasValue data.auctionConfigurationId
            "imp_id" hasValue data.impressionId
            "demand_id" hasValue data.demandId
            "ad_unit_id" hasValue data.adUnitId
            "ecpm" hasValue data.ecpm
            "banner" hasValue JsonParsers.serializeOrNull(data.banner)
            "interstitial" hasValue JsonParsers.serializeOrNull(data.interstitial)
            "rewarded" hasValue JsonParsers.serializeOrNull(data.rewarded)
        }
    }
}