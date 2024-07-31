package org.bidon.inmobi.impl

import android.app.Activity
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import org.bidon.inmobi.InmobiDemandId
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
class InmobiRewardedImplTest {
    private val activity = mockk<Activity> {
        every { applicationContext } returns mockk()
    }
    private val testee by lazy {
        InmobiRewardedImpl().apply {
            addDemandId(InmobiDemandId)
        }
    }

    @Test
    fun `parse rewarded AdUnit CPM`() {
        val auctionParamsScope by lazy {
            AdAuctionParamSource(
                activity = activity,
                pricefloor = 2.5,
                timeout = 1000,
                adUnits = listOf(
                    AdUnit(
                        demandId = "inmobi",
                        pricefloor = 3.5,
                        label = "label888",
                        bidType = BidType.CPM,
                        ext = jsonObject {
                            "placement_id" hasValue 42L
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
                        demandId = "amazon",
                        pricefloor = 2.7,
                        label = "label123",
                        bidType = BidType.RTB,
                        ext = jsonObject {
                            "slot_uuid" hasValue "slot_uuid4"
                            "format" hasValue "BANNER"
                        }.toString(),
                        uid = "uid123"
                    ),
                    impressionId = "impressionId123",
                )
            )
        }
        val actual = testee.getAuctionParam(auctionParamsScope).getOrThrow()

        require(actual is InmobiFullscreenAuctionParams)
        Truth.assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "inmobi",
                pricefloor = 3.5,
                label = "label888",
                bidType = BidType.CPM,
                ext = jsonObject {
                    "placement_id" hasValue 42L
                }.toString(),
                uid = "uid123"
            )
        )
        Truth.assertThat(actual.placementId).isEqualTo(42L)
        Truth.assertThat(actual.price).isEqualTo(3.5)
    }
}