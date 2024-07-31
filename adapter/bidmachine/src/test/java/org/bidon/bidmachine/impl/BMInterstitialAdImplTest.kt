package org.bidon.bidmachine.impl

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import io.bidmachine.BidMachine
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.bidon.bidmachine.BMFullscreenAuctionParams
import org.bidon.bidmachine.BidMachineDemandId
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.BidResponse
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.jsonObject
import org.junit.Before
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

    @Before
    fun before() {
        mockkStatic(BidMachine::class)
        every { BidMachine.getBidToken(any()) } returns "token123"
    }

    @Test
    fun `parse fullscreen AdUnit RTB`() = runTest {
        val auctionParamsScope by lazy {
            AdAuctionParamSource(
                activity = activity,
                pricefloor = 2.75,
                timeout = 1000,
                adUnits = listOf(
                    AdUnit(
                        demandId = "bidmachine",
                        pricefloor = 3.5,
                        label = "label888",
                        bidType = BidType.CPM,
                        ext = jsonObject {
                            "ad_unit_id" hasValue "ad_unit_id888"
                        }.toString(),
                        uid = "uid123"
                    ),
                    AdUnit(
                        demandId = "bidmachine",
                        pricefloor = 4.0,
                        label = "label111",
                        bidType = BidType.CPM,
                        ext = jsonObject {
                            "ad_unit_id" hasValue "ad_unit_id111"
                        }.toString(),
                        uid = "uid111"
                    ),
                ),
                onAdUnitsConsumed = {},
                optBannerFormat = BannerFormat.MRec,
                optContainerWidth = 140f,
                bidResponse = BidResponse(
                    id = "id",
                    price = 2.85,
                    ext = jsonObject {
                        "payload" hasValue "payload123"
                    }.toString(),
                    adUnit = AdUnit(
                        demandId = "bidmachine",
                        pricefloor = null,
                        label = "label123",
                        bidType = BidType.RTB,
                        ext = null,
                        uid = "uid123"
                    ),
                    impressionId = "impressionId123",
                )
            )
        }

        testee.getToken(mockk(), mockk(), emptyList())
        val actual = testee.getAuctionParam(auctionParamsScope).getOrThrow()

        require(actual is BMFullscreenAuctionParams)
        assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "bidmachine",
                pricefloor = null,
                label = "label123",
                bidType = BidType.RTB,
                ext = null,
                uid = "uid123"
            )
        )
        assertThat(actual.price).isEqualTo(2.85)
        assertThat(actual.payload).isEqualTo("payload123")
    }

    @Test
    fun `parse fullscreen AdUnit CPM`() = runTest {
        val auctionParamsScope by lazy {
            AdAuctionParamSource(
                activity = activity,
                pricefloor = 2.75,
                timeout = 1000,
                adUnits = listOf(
                    AdUnit(
                        demandId = "bidmachine",
                        pricefloor = null,
                        label = "label888",
                        bidType = BidType.CPM,
                        ext = null,
                        uid = "uid123"
                    ),
                    AdUnit(
                        demandId = "demand888",
                        pricefloor = 4.0,
                        label = "label111",
                        bidType = BidType.CPM,
                        ext = jsonObject {
                            "ad_unit_id" hasValue "ad_unit_id111"
                        }.toString(),
                        uid = "uid111"
                    ),
                ),
                onAdUnitsConsumed = {},
                optBannerFormat = null,
                optContainerWidth = null,
                bidResponse = BidResponse(
                    id = "id",
                    price = 2.75,
                    ext = jsonObject {
                        "payload" hasValue "payload123"
                    }.toString(),
                    adUnit = AdUnit(
                        demandId = "bidmachine",
                        pricefloor = null,
                        label = "label123",
                        bidType = BidType.RTB,
                        ext = null,
                        uid = "uid123"
                    ),
                    impressionId = "impressionId123",
                )
            )
        }

        val actual = testee.getAuctionParam(auctionParamsScope).getOrThrow()

        // parse fullscreen AdUnit CPM
        require(actual is BMFullscreenAuctionParams)
        assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "bidmachine",
                pricefloor = null,
                label = "label888",
                bidType = BidType.CPM,
                ext = null,
                uid = "uid123"
            )
        )
        assertThat(actual.price).isEqualTo(2.75)
        assertThat(actual.payload).isNull()
    }
}