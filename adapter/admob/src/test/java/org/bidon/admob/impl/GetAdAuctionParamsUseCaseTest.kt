package org.bidon.admob.impl

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.bidon.admob.AdmobBannerAuctionParams
import org.bidon.admob.AdmobFullscreenAdAuctionParams
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.BidResponse
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.jsonObject
import kotlin.test.Test

/**
 * Created by Aleksei Cherniaev on 21/11/2023.
 */
class GetAdAuctionParamsUseCaseTest {

    private val testee by lazy {
        GetAdAuctionParamsUseCase()
    }

    private val activity = mockk<Activity>()

    @Test
    fun `parse banner AdUnit RTB`() {
        val auctionParamsScope by lazy {
            AdAuctionParamSource(
                activity = activity,
                pricefloor = 2.75,
                timeout = 1000,
                adUnits = listOf(
                    AdUnit(
                        demandId = "admob",
                        pricefloor = 3.5,
                        label = "label888",
                        bidType = BidType.CPM,
                        ext = jsonObject {
                            "ad_unit_id" hasValue "ad_unit_id888"
                        }.toString(),
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
                optBannerFormat = BannerFormat.MRec,
                optContainerWidth = 140f,
                bidResponse = BidResponse(
                    id = "id",
                    price = 2.75,
                    ext = jsonObject {
                        "payload" hasValue "payload123"
                    }.toString(),
                    adUnit = AdUnit(
                        demandId = "admob",
                        pricefloor = 2.6,
                        label = "label123",
                        bidType = BidType.RTB,
                        ext = jsonObject {
                            "ad_unit_id" hasValue "ad_unit_id4"
                        }.toString(),
                        uid = "uid123"
                    ),
                    impressionId = "impressionId123",
                )
            )
        }
        val actual = testee.invoke(
            auctionParamsScope = auctionParamsScope,
            adType = AdType.Banner,
            bidType = BidType.RTB,
        ).getOrThrow()

        require(actual is AdmobBannerAuctionParams.Bidding)
        assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "admob",
                pricefloor = 2.6,
                label = "label123",
                bidType = BidType.RTB,
                ext = jsonObject {
                    "ad_unit_id" hasValue "ad_unit_id4"
                }.toString(),
                uid = "uid123"
            )
        )
        assertThat(actual.price).isEqualTo(2.75)
        assertThat(actual.payload).isEqualTo("payload123")
        assertThat(actual.adUnitId).isEqualTo("ad_unit_id4")
    }

    @Test
    fun `parse banner AdUnit CPM`() {
        val auctionParamsScope by lazy {
            AdAuctionParamSource(
                activity = activity,
                pricefloor = 2.75,
                timeout = 1000,
                adUnits = listOf(
                    AdUnit(
                        demandId = "admob",
                        pricefloor = 3.5,
                        label = "label888",
                        bidType = BidType.CPM,
                        ext = jsonObject {
                            "ad_unit_id" hasValue "ad_unit_id888"
                        }.toString(),
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
                optBannerFormat = BannerFormat.MRec,
                optContainerWidth = 140f,
                bidResponse = BidResponse(
                    id = "id",
                    price = 2.75,
                    ext = jsonObject {
                        "payload" hasValue "payload123"
                    }.toString(),
                    adUnit = AdUnit(
                        demandId = "admob",
                        pricefloor = 2.6,
                        label = "label123",
                        bidType = BidType.RTB,
                        ext = jsonObject {
                            "ad_unit_id" hasValue "ad_unit_id4"
                        }.toString(),
                        uid = "uid123"
                    ),
                    impressionId = "impressionId123",
                )
            )
        }
        val actual = testee.invoke(
            auctionParamsScope = auctionParamsScope,
            adType = AdType.Banner,
            bidType = BidType.CPM,
        ).getOrThrow()

        require(actual is AdmobBannerAuctionParams.Network)
        assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "admob",
                pricefloor = 3.5,
                label = "label888",
                bidType = BidType.CPM,
                ext = jsonObject {
                    "ad_unit_id" hasValue "ad_unit_id888"
                }.toString(),
                uid = "uid123"
            )
        )
        assertThat(actual.price).isEqualTo(3.5)
        assertThat(actual.adUnitId).isEqualTo("ad_unit_id888")
    }

    @Test
    fun `parse fullscreen AdUnit RTB`() {
        val auctionParamsScope by lazy {
            AdAuctionParamSource(
                activity = activity,
                pricefloor = 2.75,
                timeout = 1000,
                adUnits = listOf(
                    AdUnit(
                        demandId = "admob",
                        pricefloor = 3.5,
                        label = "label888",
                        bidType = BidType.CPM,
                        ext = jsonObject {
                            "ad_unit_id" hasValue "ad_unit_id888"
                        }.toString(),
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
                optBannerFormat = BannerFormat.MRec,
                optContainerWidth = 140f,
                bidResponse = BidResponse(
                    id = "id",
                    price = 2.75,
                    ext = jsonObject {
                        "payload" hasValue "payload123"
                    }.toString(),
                    adUnit = AdUnit(
                        demandId = "admob",
                        pricefloor = 2.6,
                        label = "label123",
                        bidType = BidType.RTB,
                        ext = jsonObject {
                            "ad_unit_id" hasValue "ad_unit_id4"
                        }.toString(),
                        uid = "uid123"
                    ),
                    impressionId = "impressionId123",
                )
            )
        }
        val actual = testee.invoke(
            auctionParamsScope = auctionParamsScope,
            adType = AdType.Rewarded,
            bidType = BidType.RTB,
        ).getOrThrow()

        require(actual is AdmobFullscreenAdAuctionParams.Bidding)
        assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "admob",
                pricefloor = 2.6,
                label = "label123",
                bidType = BidType.RTB,
                ext = jsonObject {
                    "ad_unit_id" hasValue "ad_unit_id4"
                }.toString(),
                uid = "uid123"
            )
        )
        assertThat(actual.price).isEqualTo(2.75)
        assertThat(actual.payload).isEqualTo("payload123")
        assertThat(actual.adUnitId).isEqualTo("ad_unit_id4")
    }

    @Test
    fun `parse fullscreen AdUnit CPM`() {
        val auctionParamsScope by lazy {
            AdAuctionParamSource(
                activity = activity,
                pricefloor = 2.75,
                timeout = 1000,
                adUnits = listOf(
                    AdUnit(
                        demandId = "admob",
                        pricefloor = 3.5,
                        label = "label888",
                        bidType = BidType.CPM,
                        ext = jsonObject {
                            "ad_unit_id" hasValue "ad_unit_id888"
                        }.toString(),
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
                        demandId = "admob",
                        pricefloor = 2.6,
                        label = "label123",
                        bidType = BidType.RTB,
                        ext = jsonObject {
                            "ad_unit_id" hasValue "ad_unit_id4"
                        }.toString(),
                        uid = "uid123"
                    ),
                    impressionId = "impressionId123",
                )
            )
        }
        val actual = testee.invoke(
            auctionParamsScope = auctionParamsScope,
            adType = AdType.Interstitial,
            bidType = BidType.CPM,
        ).getOrThrow()

        // parse fullscreen AdUnit CPM
        require(actual is AdmobFullscreenAdAuctionParams.Network)
        assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "admob",
                pricefloor = 3.5,
                label = "label888",
                bidType = BidType.CPM,
                ext = jsonObject {
                    "ad_unit_id" hasValue "ad_unit_id888"
                }.toString(),
                uid = "uid123"
            )
        )
        assertThat(actual.price).isEqualTo(3.5)
        assertThat(actual.adUnitId).isEqualTo("ad_unit_id888")
    }
}