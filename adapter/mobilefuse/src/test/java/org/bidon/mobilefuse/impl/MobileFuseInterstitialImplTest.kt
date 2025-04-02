package org.bidon.mobilefuse.impl

import android.app.Activity
import com.google.common.truth.Truth
import io.mockk.mockk
import org.bidon.mobilefuse.MobileFuseDemandId
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.jsonObject
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 21/11/2023.
 */
class MobileFuseInterstitialImplTest {
    private val activity = mockk<Activity>()
    private val testee by lazy {
        MobileFuseInterstitialImpl().apply {
            addDemandId(MobileFuseDemandId)
        }
    }

    @Test
    fun `parse interstitial AdUnit RTB`() {
        val auctionParamsScope by lazy {
            AdAuctionParamSource(
                activity = activity,
                pricefloor = 2.7,
                adUnit = AdUnit(
                    demandId = "mobilefuse",
                    pricefloor = 2.7,
                    label = "label123",
                    bidType = BidType.RTB,
                    ext = jsonObject {
                        "placement_id" hasValue "placement_id4"
                        "signaldata" hasValue "signaldata_payload123"
                    }.toString(),
                    timeout = 5000,
                    uid = "uid123"
                ),
                optBannerFormat = null,
                optContainerWidth = null,
            )
        }
        val actual = testee.getAuctionParam(auctionParamsScope).getOrThrow()

        require(actual is MobileFuseFullscreenAuctionParams)
        Truth.assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "mobilefuse",
                pricefloor = 2.7,
                label = "label123",
                bidType = BidType.RTB,
                ext = jsonObject {
                    "placement_id" hasValue "placement_id4"
                    "signaldata" hasValue "signaldata_payload123"
                }.toString(),
                timeout = 5000,
                uid = "uid123"
            )
        )
        Truth.assertThat(actual.placementId).isEqualTo("placement_id4")
        Truth.assertThat(actual.signalData).isEqualTo("signaldata_payload123")
        Truth.assertThat(actual.price).isEqualTo(2.7)
    }
}