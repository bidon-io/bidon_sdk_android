package org.bidon.vungle.impl

import android.app.Activity
import com.google.common.truth.Truth
import io.mockk.mockk
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.jsonObject
import org.bidon.vungle.VungleDemandId
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 21/11/2023.
 */
class VungleInterstitialImplTest {
    private val activity = mockk<Activity>()
    private val testee by lazy {
        VungleInterstitialImpl().apply {
            addDemandId(VungleDemandId)
        }
    }

    @Test
    fun `parse interstitial AdUnit RTB`() {
        val auctionParamsScope by lazy {
            AdAuctionParamSource(
                activity = activity,
                pricefloor = 2.7,
                adUnit = AdUnit(
                    demandId = "vungle",
                    pricefloor = 2.7,
                    label = "label123",
                    bidType = BidType.RTB,
                    ext = jsonObject {
                        "placement_id" hasValue "placement_id4"
                        "payload" hasValue "test_payload"
                    }.toString(),
                    timeout = 5000,
                    uid = "uid123"
                ),
                optBannerFormat = BannerFormat.MRec,
                optContainerWidth = 140f,
            )
        }
        val actual = testee.getAuctionParam(auctionParamsScope).getOrThrow()

        require(actual is VungleFullscreenAuctionParams)
        Truth.assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "vungle",
                pricefloor = 2.7,
                label = "label123",
                bidType = BidType.RTB,
                ext = jsonObject {
                    "placement_id" hasValue "placement_id4"
                    "payload" hasValue "test_payload"
                }.toString(),
                timeout = 5000,
                uid = "uid123"
            )
        )
        Truth.assertThat(actual.placementId).isEqualTo("placement_id4")
        Truth.assertThat(actual.payload).isEqualTo("test_payload")
        Truth.assertThat(actual.price).isEqualTo(2.7)
    }
}