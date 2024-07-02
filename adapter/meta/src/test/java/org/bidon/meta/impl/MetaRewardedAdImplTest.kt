package org.bidon.meta.impl

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.bidon.meta.MetaDemandId
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.auction.models.AdUnit
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
                pricefloor = 3.5,
                adUnit = AdUnit(
                    demandId = "meta",
                    pricefloor = 3.5,
                    label = "label888",
                    bidType = BidType.CPM,
                    ext = jsonObject {
                        "placement_id" hasValue "ad_unit_id888"
                        "payload" hasValue "payload123"
                    }.toString(),
                    timeout = 5000,
                    uid = "uid123"
                ),
                optBannerFormat = null,
                optContainerWidth = null,
            )
        }
        val actual = testee.getAuctionParam(auctionParamsScope).getOrThrow()

        require(actual is MetaFullscreenAuctionParams)
        assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "meta",
                pricefloor = 3.5,
                label = "label888",
                bidType = BidType.CPM,
                ext = jsonObject {
                    "placement_id" hasValue "ad_unit_id888"
                    "payload" hasValue "payload123"
                }.toString(),
                timeout = 5000,
                uid = "uid123"
            )
        )
        assertThat(actual.placementId).isEqualTo("ad_unit_id888")
        assertThat(actual.price).isEqualTo(3.5)
        assertThat(actual.payload).isEqualTo("payload123")
    }
}