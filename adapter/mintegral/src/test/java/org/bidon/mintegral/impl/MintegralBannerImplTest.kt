package org.bidon.mintegral.impl

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.bidon.mintegral.MintegralBannerAuctionParam
import org.bidon.mintegral.MintegralDemandId
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.jsonObject
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 21/11/2023.
 */
class MintegralBannerImplTest {
    private val activity = mockk<Activity>()
    private val testee by lazy {
        MintegralBannerImpl().apply {
            addDemandId(MintegralDemandId)
        }
    }

    @Test
    fun `parse interstitial AdUnit RTB`() {
        val auctionParamsScope by lazy {
            AdAuctionParamSource(
                activity = activity,
                pricefloor = 2.5,
                adUnit = AdUnit(
                    demandId = "mintegral",
                    pricefloor = 2.9,
                    label = "label123",
                    bidType = BidType.RTB,
                    ext = jsonObject {
                        "placement_id" hasValue "placemet_id4"
                        "unit_id" hasValue "unit_id4"
                        "payload" hasValue "payload123"
                    }.toString(),
                    timeout = 5000,
                    uid = "uid123"
                ),
                optBannerFormat = BannerFormat.Banner,
                optContainerWidth = 140f,
            )
        }
        val actual = testee.getAuctionParam(auctionParamsScope).getOrThrow()

        require(actual is MintegralBannerAuctionParam)
        assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "mintegral",
                pricefloor = 2.9,
                label = "label123",
                bidType = BidType.RTB,
                ext = jsonObject {
                    "placement_id" hasValue "placemet_id4"
                    "unit_id" hasValue "unit_id4"
                    "payload" hasValue "payload123"
                }.toString(),
                timeout = 5000,
                uid = "uid123"
            )
        )
        assertThat(actual.payload).isEqualTo("payload123")
        assertThat(actual.unitId).isEqualTo("unit_id4")
        assertThat(actual.placementId).isEqualTo("placemet_id4")
        assertThat(actual.price).isEqualTo(2.9)
    }
}