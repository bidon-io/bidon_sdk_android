package org.bidon.sdk.stats.models

import org.bidon.sdk.auction.models.BannerRequest
import org.bidon.sdk.auction.models.InterstitialRequest
import org.bidon.sdk.auction.models.RewardedRequest
import org.bidon.sdk.config.models.json_scheme_utils.assertEquals
import org.bidon.sdk.config.models.json_scheme_utils.expectedJsonStructure
import org.bidon.sdk.utils.serializer.serialize
import org.junit.Test

/**
 * Created by Bidon Team on 03/03/2023.
 */
internal class ResultBodySerializerTest {
    @Test
    fun `it should serialize FAILURE`() {
        val actual = ResultBody(
            status = "FAIL",
            auctionFinishTs = 1020,
            auctionStartTs = 1000,
            price = null,
            bidType = BidType.CPM.code,
            banner = BannerRequest(BannerRequest.StatFormat.BANNER_320x50.code),
            interstitial = InterstitialRequest,
            rewarded = RewardedRequest,
            winnerDemandId = "admob",
            winnerAdUnitUid = "id123",
            winnerAdUnitLabel = "label123",
        ).serialize()

        actual.assertEquals(
            expectedJsonStructure {
                "status" hasValue "FAIL"
                "auction_start_ts" hasValue 1000
                "auction_finish_ts" hasValue 1020
                "bid_type" hasValue "CPM"
                "winner_demand_id" hasValue "admob"
                "winner_ad_unit_uid" hasValue "id123"
                "winner_ad_unit_label" hasValue "label123"
                "banner" hasJson expectedJsonStructure {
                    "format" hasValue "BANNER"
                }
                "interstitial" hasJson expectedJsonStructure {}
                "rewarded" hasJson expectedJsonStructure {}
            }
        )
    }
}