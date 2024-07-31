package org.bidon.bigoads.impl

import android.app.Activity
import com.google.common.truth.Truth
import io.mockk.mockk
import org.bidon.bigoads.BigoAdsDemandId
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.jsonObject
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 21/11/2023.
 */
class BigoAdsBannerImplTest {
    private val activity = mockk<Activity>()
    private val testee by lazy {
        BigoAdsBannerImpl().apply {
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
                    label = "label123",
                    bidType = BidType.RTB,
                    ext = jsonObject {
                        "slot_id" hasValue "slot_id4"
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

        require(actual is BigoBannerAuctionParams)
        Truth.assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "bigoads",
                pricefloor = 2.7,
                label = "label123",
                bidType = BidType.RTB,
                ext = jsonObject {
                    "slot_id" hasValue "slot_id4"
                    "payload" hasValue "test_payload"
                }.toString(),
                timeout = 5000,
                uid = "uid123"
            )
        )
        Truth.assertThat(actual.slotId).isEqualTo("slot_id4")
        Truth.assertThat(actual.payload).isEqualTo("test_payload")
        Truth.assertThat(actual.price).isEqualTo(2.7)
    }
}