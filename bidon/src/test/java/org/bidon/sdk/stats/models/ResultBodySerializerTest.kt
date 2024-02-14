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
            roundId = "id13",
            demandId = null,
            adUnitId = null,
            auctionFinishTs = 1020,
            auctionStartTs = 1000,
            ecpm = null,
            bidType = BidType.CPM.code,
            lineItemUid = "123",
            banner = BannerRequest(BannerRequest.StatFormat.BANNER_320x50.code),
            interstitial = InterstitialRequest,
            rewarded = RewardedRequest,
        ).serialize()

        actual.assertEquals(
            expectedJsonStructure {
                "status" hasValue "FAIL"
                "round_id" hasValue "id13"
                "auction_start_ts" hasValue 1000
                "auction_finish_ts" hasValue 1020
                "bid_type" hasValue "cpm"
                "line_item_uid" hasValue "123"
                "banner" hasJson expectedJsonStructure {
                    "format" hasValue "BANNER"
                }
                "interstitial" hasJson expectedJsonStructure {}
                "rewarded" hasJson expectedJsonStructure {}
            }
        )
    }
}