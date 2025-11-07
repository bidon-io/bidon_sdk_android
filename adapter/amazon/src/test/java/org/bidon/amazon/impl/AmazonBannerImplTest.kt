package org.bidon.amazon.impl

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.bidon.amazon.AmazonBidManager
import org.bidon.amazon.AmazonDemandId
import org.bidon.amazon.SlotType
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.jsonObject
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 21/11/2023.
 */
class AmazonBannerImplTest {
    private val activity = mockk<Activity>()
    private val bidManager = mockk<AmazonBidManager>()
    private val testee by lazy {
        AmazonBannerImpl(bidManager).apply {
            addDemandId(AmazonDemandId)
        }
    }

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
                        "format" hasValue "BANNER"
                    }.toString(),
                    timeout = 5000,
                    uid = "uid123"
                ),
                optBannerFormat = BannerFormat.Banner,
                optContainerWidth = 140f,
            )
        }
        val actual = testee.getAuctionParam(auctionParamsScope).getOrThrow()

        require(actual is BannerAuctionParams)
        assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "amazon",
                pricefloor = 2.7,
                label = "label123",
                bidType = BidType.RTB,
                ext = jsonObject {
                    "slot_uuid" hasValue "slot_uuid4"
                    "format" hasValue "BANNER"
                }.toString(),
                timeout = 5000,
                uid = "uid123"
            )
        )
        assertThat(actual.slotUuid).isEqualTo("slot_uuid4")
        assertThat(actual.price).isEqualTo(2.7)
        assertThat(actual.format).isEqualTo(SlotType.BANNER)
    }
}