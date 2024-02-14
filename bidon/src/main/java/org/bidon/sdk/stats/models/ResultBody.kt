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
    @field:JsonName("winner_id")
    val demandId: String?,
    @field:JsonName("bid_type")
    val bidType: String?,
    @field:JsonName("ecpm")
    val ecpm: Double?,
    @field:JsonName("ad_unit_id")
    val adUnitId: String?,
    @field:JsonName("line_item_uid")
    val lineItemUid: String?,
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
