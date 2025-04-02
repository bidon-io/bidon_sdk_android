package org.bidon.bidmachine.impl

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.bidon.bidmachine.BMFullscreenAuctionParams
import org.bidon.bidmachine.BidMachineDemandId
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.jsonObject
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 21/11/2023.
 */
class BMInterstitialAdImplTest {
    private val activity = mockk<Activity> {
        every { applicationContext } returns mockk()
    }
    private val testee by lazy {
        BMInterstitialAdImpl().apply {
            addDemandId(BidMachineDemandId)
        }
    }

    @Test
    fun `parse fullscreen AdUnit RTB`() = runTest {
        val auctionParamsScope by lazy {
            AdAuctionParamSource(
                activity = activity,
                pricefloor = 2.75,
                adUnit = AdUnit(
                    demandId = "bidmachine",
                    pricefloor = 2.75,
                    label = "label123",
                    bidType = BidType.RTB,
                    timeout = 5000,
                    uid = "uid123",
                    ext = jsonObject {
                        "payload" hasValue "payload123"
                    }.toString(),
                ),
                optBannerFormat = null,
                optContainerWidth = null,
            )
        }

        val actual = testee.getAuctionParam(auctionParamsScope).getOrThrow()

        require(actual is BMFullscreenAuctionParams)
        assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "bidmachine",
                pricefloor = 2.75,
                label = "label123",
                bidType = BidType.RTB,
                timeout = 5000,
                uid = "uid123",
                ext = jsonObject {
                    "payload" hasValue "payload123"
                }.toString(),
            )
        )
        assertThat(actual.price).isEqualTo(2.75)
        assertThat(actual.payload).isEqualTo("payload123")
    }

    @Test
    fun `parse fullscreen AdUnit CPM`() = runTest {
        val auctionParamsScope by lazy {
            AdAuctionParamSource(
                activity = activity,
                pricefloor = 2.75,
                adUnit = AdUnit(
                    demandId = "bidmachine",
                    pricefloor = 2.75,
                    label = "label888",
                    bidType = BidType.CPM,
                    ext = null,
                    timeout = 5000,
                    uid = "uid123"
                ),
                optBannerFormat = null,
                optContainerWidth = null,
            )
        }

        val actual = testee.getAuctionParam(auctionParamsScope).getOrThrow()

        // parse fullscreen AdUnit CPM
        require(actual is BMFullscreenAuctionParams)
        assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "bidmachine",
                pricefloor = 2.75,
                label = "label888",
                bidType = BidType.CPM,
                ext = null,
                timeout = 5000,
                uid = "uid123"
            )
        )
        assertThat(actual.price).isEqualTo(2.75)
        assertThat(actual.payload).isNull()
    }
}