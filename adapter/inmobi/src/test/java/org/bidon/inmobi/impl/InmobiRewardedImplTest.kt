package org.bidon.inmobi.impl

import android.app.Activity
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import org.bidon.inmobi.InmobiDemandId
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.auction.models.AdUnit
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
                optBannerFormat = null,
                optContainerWidth = 140f,
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
                timeout = 5000,
                uid = "uid123"
            )
        )
        Truth.assertThat(actual.placementId).isEqualTo(42L)
        Truth.assertThat(actual.price).isEqualTo(3.5)
    }
}