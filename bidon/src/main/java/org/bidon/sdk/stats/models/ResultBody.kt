package org.bidon.sdk.stats.models

import org.bidon.sdk.auction.models.BannerRequest
import org.bidon.sdk.auction.models.InterstitialRequest
import org.bidon.sdk.auction.models.RewardedRequest
import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Bidon Team on 03/03/2023.
 */
internal data class ResultBody(
    @field:JsonName("status")
    val status: String,
    @field:JsonName("round_id")
    val roundId: String?,
    @field:JsonName("winner_demand_id")
    val winnerDemandId: String?,
    @field:JsonName("bid_type")
    val bidType: String?,
    @field:JsonName("price")
    val price: Double?,
    @field:JsonName("winner_ad_unit_uid")
    val winnerAdUnitUid: String?,
    @field:JsonName("winner_ad_unit_label")
    val winnerAdUnitLabel: String?,
    @field:JsonName("auction_start_ts")
    val auctionStartTs: Long,
    @field:JsonName("auction_finish_ts")
    val auctionFinishTs: Long,

    @field:JsonName("banner")
    val banner: BannerRequest?,
    @field:JsonName("interstitial")
    val interstitial: InterstitialRequest?,
    @field:JsonName("rewarded")
    val rewarded: RewardedRequest?
) : Serializable
