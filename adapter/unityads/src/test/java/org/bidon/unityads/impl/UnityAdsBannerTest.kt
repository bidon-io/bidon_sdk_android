package org.bidon.unityads.impl

import android.app.Activity
import com.google.common.truth.Truth
import io.mockk.mockk
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.jsonObject
import org.bidon.unityads.UnityAdsDemandId
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 21/11/2023.
 */
class UnityAdsBannerTest {
    private val activity = mockk<Activity>()
    private val testee by lazy {
        UnityAdsBanner().apply {
            addDemandId(UnityAdsDemandId)
        }
    }

    @Test
    fun `parse banner AdUnit CPM`() {
        val auctionParamsScope by lazy {
            AdAuctionParamSource(
                activity = activity,
                pricefloor = 4.0,
                AdUnit(
                    demandId = "unityads",
                    pricefloor = 4.0,
                    label = "label111",
                    bidType = BidType.CPM,
                    ext = jsonObject {
                        "placement_id" hasValue "placement_id111"
                    }.toString(),
                    timeout = 5000,
                    uid = "uid111"
                ),
                optBannerFormat = BannerFormat.MRec,
                optContainerWidth = 140f,
            )
        }
        val actual = testee.getAuctionParam(auctionParamsScope).getOrThrow()

        require(actual is UnityAdsBannerAuctionParams)
        Truth.assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "unityads",
                pricefloor = 4.0,
                label = "label111",
                bidType = BidType.CPM,
                ext = jsonObject {
                    "placement_id" hasValue "placement_id111"
                }.toString(),
                timeout = 5000,
                uid = "uid111"
            )
        )
        Truth.assertThat(actual.placementId).isEqualTo("placement_id111")
        Truth.assertThat(actual.price).isEqualTo(4.0)
    }
}