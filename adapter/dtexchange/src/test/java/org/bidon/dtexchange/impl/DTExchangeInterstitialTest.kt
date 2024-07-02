package org.bidon.dtexchange.impl

import android.app.Activity
import com.google.common.truth.Truth
import io.mockk.mockk
import org.bidon.dtexchange.DTExchangeDemandId
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.jsonObject
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 21/11/2023.
 */
class DTExchangeInterstitialTest {
    private val activity = mockk<Activity>()
    private val testee by lazy {
        DTExchangeInterstitial().apply {
            addDemandId(DTExchangeDemandId)
        }
    }

    @Test
    fun `parse banner AdUnit CPM`() {
        val auctionParamsScope by lazy {
            AdAuctionParamSource(
                activity = activity,
                pricefloor = 2.5,
                adUnit = AdUnit(
                    demandId = "dtexchange",
                    pricefloor = 4.0,
                    label = "label111",
                    bidType = BidType.CPM,
                    ext = jsonObject {
                        "spot_id" hasValue "spot_id111"
                    }.toString(),
                    timeout = 5000,
                    uid = "uid111"
                ),
                optBannerFormat = null,
                optContainerWidth = null,
            )
        }
        val actual = testee.getAuctionParam(auctionParamsScope).getOrThrow()

        require(actual is DTExchangeAdAuctionParams)
        Truth.assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "dtexchange",
                pricefloor = 4.0,
                label = "label111",
                bidType = BidType.CPM,
                ext = jsonObject {
                    "spot_id" hasValue "spot_id111"
                }.toString(),
                timeout = 5000,
                uid = "uid111"
            )
        )
        Truth.assertThat(actual.spotId).isEqualTo("spot_id111")
        Truth.assertThat(actual.price).isEqualTo(4.0)
    }
}