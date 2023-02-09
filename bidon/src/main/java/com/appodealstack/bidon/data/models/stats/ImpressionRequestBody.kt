package com.appodealstack.bidon.data.models.stats

import com.appodealstack.bidon.data.json.JsonParsers
import com.appodealstack.bidon.data.json.JsonSerializer
import com.appodealstack.bidon.data.json.jsonObject
import com.appodealstack.bidon.data.models.auction.BannerRequestBody
import com.appodealstack.bidon.data.models.auction.InterstitialRequestBody
import com.appodealstack.bidon.data.models.auction.RewardedRequestBody
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