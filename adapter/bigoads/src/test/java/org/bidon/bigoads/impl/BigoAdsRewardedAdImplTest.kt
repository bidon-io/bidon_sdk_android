package org.bidon.bigoads.impl

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.bidon.bigoads.BigoAdsDemandId
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.jsonObject
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 21/11/2023.
 */
class BigoAdsRewardedAdImplTest {
    private val activity = mockk<Activity>()
    private val testee by lazy {
        BigoAdsRewardedAdImpl().apply {
            addDemandId(BigoAdsDemandId)
        }
    }

    @Test
    fun `parse banner AdUnit RTB`() {
        val auctionParamsScope by lazy {
            AdAuctionParamSource(
                activity = activity,
                pricefloor = 2.7,
                adUnit = AdUnit(
                    demandId = "bigoads",
                    pricefloor = 2.7,
                    label = "label888",
                    bidType = BidType.RTB,
                    ext = jsonObject {
                        "slot_id" hasValue "ad_unit_id888"
                        "payload" hasValue "test_payload"
                    }.toString(),
                    timeout = 5000,
                    uid = "uid123"
                ),
                optBannerFormat = null,
                optContainerWidth = null,
            )
        }
        val actual = testee.getAuctionParam(auctionParamsScope).getOrThrow()

        require(actual is BigoAdsFullscreenAuctionParams)
        assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "bigoads",
                pricefloor = 2.7,
                label = "label888",
                bidType = BidType.RTB,
                ext = jsonObject {
                    "slot_id" hasValue "ad_unit_id888"
                    "payload" hasValue "test_payload"
                }.toString(),
                timeout = 5000,
                uid = "uid123"
            )
        )
        assertThat(actual.slotId).isEqualTo("ad_unit_id888")
        assertThat(actual.payload).isEqualTo("test_payload")
        assertThat(actual.price).isEqualTo(2.7)
    }
}