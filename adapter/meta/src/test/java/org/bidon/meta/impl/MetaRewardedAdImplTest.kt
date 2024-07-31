package org.bidon.meta.impl

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.bidon.meta.MetaDemandId
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.BidResponse
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.jsonObject
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 21/11/2023.
 */
class MetaRewardedAdImplTest {
    private val activity = mockk<Activity> {
        every { applicationContext } returns mockk()
    }
    private val testee by lazy {
        MetaRewardedAdImpl().apply {
            addDemandId(MetaDemandId)
        }
    }

    @Test
    fun `parse rewarded AdUnit RTB`() {
        val auctionParamsScope by lazy {
            AdAuctionParamSource(
                activity = activity,
                pricefloor = 2.5,
                timeout = 1000,
                adUnits = listOf(
                    AdUnit(
                        demandId = "admob",
                        pricefloor = 3.5,
                        label = "label888",
                        bidType = BidType.CPM,
                        ext = jsonObject {
                            "ad_unit_id" hasValue "ad_unit_id888"
                        }.toString(),
                        uid = "uid123"
                    ),
                    AdUnit(
                        demandId = "applovin",
                        pricefloor = 4.0,
                        label = "label111",
                        bidType = BidType.CPM,
                        ext = jsonObject {
                            "zone_id" hasValue "zone_id111"
                        }.toString(),
                        uid = "uid111"
                    ),
                ),
                onAdUnitsConsumed = {},
                optBannerFormat = BannerFormat.MRec,
                optContainerWidth = 140f,
                bidResponse = BidResponse(
                    id = "id",
                    price = 2.7,
                    ext = jsonObject {
                        "payload" hasValue "payload123"
                    }.toString(),
                    adUnit = AdUnit(
                        demandId = "meta",
                        pricefloor = 2.6,
                        label = "label123",
                        bidType = BidType.RTB,
                        ext = jsonObject {
                            "placement_id" hasValue "placement_id4"
                        }.toString(),
                        uid = "uid123"
                    ),
                    impressionId = "impressionId123",
                )
            )
        }
        val actual = testee.getAuctionParam(auctionParamsScope).getOrThrow()

        require(actual is MetaFullscreenAuctionParams)
        assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "meta",
                pricefloor = 2.6,
                label = "label123",
                bidType = BidType.RTB,
                ext = jsonObject {
                    "placement_id" hasValue "placement_id4"
                }.toString(),
                uid = "uid123"
            )
        )
        assertThat(actual.placementId).isEqualTo("placement_id4")
        assertThat(actual.price).isEqualTo(2.7)
        assertThat(actual.payload).isEqualTo("payload123")
    }
}