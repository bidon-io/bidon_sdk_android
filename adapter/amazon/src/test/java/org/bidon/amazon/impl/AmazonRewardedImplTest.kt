package org.bidon.amazon.impl

import android.app.Activity
import com.google.common.truth.Truth
import io.mockk.mockk
import org.bidon.amazon.SlotType
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.jsonObject
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 21/11/2023.
 */
class AmazonRewardedImplTest {
    private val activity = mockk<Activity>()
    private val testee by lazy { AmazonRewardedImpl(listOf()) }

    @Test
    fun `parse banner AdUnit RTB`() {
        val auctionParamsScope by lazy {
            AdAuctionParamSource(
                activity = activity,
                pricefloor = 2.5,
                adUnit = AdUnit(
                    demandId = "amazon",
                    pricefloor = 2.7,
                    label = "label123",
                    bidType = BidType.RTB,
                    ext = jsonObject {
                        "slot_uuid" hasValue "slot_uuid4"
                        "format" hasValue "REWARDED"
                    }.toString(),
                    timeout = 5000,
                    uid = "uid123"
                ),
                optBannerFormat = null,
                optContainerWidth = null,
            )
        }
        val actual = testee.getAuctionParam(auctionParamsScope).getOrThrow()

        require(actual is FullscreenAuctionParams)
        Truth.assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "amazon",
                pricefloor = 2.7,
                label = "label123",
                bidType = BidType.RTB,
                ext = jsonObject {
                    "slot_uuid" hasValue "slot_uuid4"
                    "format" hasValue "REWARDED"
                }.toString(),
                timeout = 5000,
                uid = "uid123"
            )
        )
        Truth.assertThat(actual.slotUuid).isEqualTo("slot_uuid4")
        Truth.assertThat(actual.price).isEqualTo(2.7)
        Truth.assertThat(actual.format).isEqualTo(SlotType.REWARDED_AD)
    }
}