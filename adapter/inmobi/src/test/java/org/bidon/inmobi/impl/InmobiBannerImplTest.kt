package org.bidon.inmobi.impl

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.bidon.inmobi.InmobiDemandId
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.jsonObject
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 21/11/2023.
 */
class InmobiBannerImplTest {
    private val activity = mockk<Activity>()
    private val testee by lazy {
        InmobiBannerImpl().apply {
            addDemandId(InmobiDemandId)
        }
    }

    @Test
    fun `parse banner AdUnit CPM`() {
        val auctionParamsScope by lazy {
            AdAuctionParamSource(
                activity = activity,
                pricefloor = 2.5,
                adUnit = AdUnit(
                    demandId = "inmobi",
                    pricefloor = 3.5,
                    label = "label888",
                    bidType = BidType.CPM,
                    ext = jsonObject {
                        "placement_id" hasValue 42L
                    }.toString(),
                    timeout = 5000,
                    uid = "uid123"
                ),
                optBannerFormat = BannerFormat.MRec,
                optContainerWidth = 140f,
            )
        }
        val actual = testee.getAuctionParam(auctionParamsScope).getOrThrow()

        require(actual is InmobiBannerAuctionParams)
        assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "inmobi",
                pricefloor = 3.5,
                label = "label888",
                bidType = BidType.CPM,
                ext = jsonObject {
                    "placement_id" hasValue 42L
                }.toString(),
                timeout = 5000,
                uid = "uid123"
            )
        )
        assertThat(actual.placementId).isEqualTo(42L)
        assertThat(actual.price).isEqualTo(3.5)
    }
}