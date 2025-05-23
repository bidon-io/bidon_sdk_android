package org.bidon.applovin.impl

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.bidon.applovin.ApplovinBannerAuctionParams
import org.bidon.applovin.ApplovinDemandId
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.jsonObject
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 21/11/2023.
 */
class ApplovinBannerImplTest {
    private val activity = mockk<Activity>()
    private val testee by lazy {
        ApplovinBannerImpl(mockk()).apply {
            addDemandId(ApplovinDemandId)
        }
    }

    @Test
    fun `parse banner AdUnit CPM`() {
        val auctionParamsScope by lazy {
            AdAuctionParamSource(
                activity = activity,
                pricefloor = 2.5,
                adUnit = AdUnit(
                    demandId = "applovin",
                    pricefloor = 4.0,
                    label = "label111",
                    bidType = BidType.CPM,
                    ext = jsonObject {
                        "zone_id" hasValue "zone_id111"
                    }.toString(),
                    timeout = 5000,
                    uid = "uid111"
                ),
                optBannerFormat = BannerFormat.MRec,
                optContainerWidth = 140f,
            )
        }
        val actual = testee.getAuctionParam(auctionParamsScope).getOrThrow()

        require(actual is ApplovinBannerAuctionParams)
        assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "applovin",
                pricefloor = 4.0,
                label = "label111",
                bidType = BidType.CPM,
                ext = jsonObject {
                    "zone_id" hasValue "zone_id111"
                }.toString(),
                timeout = 5000,
                uid = "uid111"
            )
        )
        assertThat(actual.zoneId).isEqualTo("zone_id111")
        assertThat(actual.price).isEqualTo(4.0)
    }
}