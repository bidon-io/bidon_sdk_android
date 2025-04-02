package org.bidon.gam.impl

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.bidon.gam.GamBannerAuctionParams
import org.bidon.gam.GamFullscreenAdAuctionParams
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit
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
                pricefloor = 2.6,
                adUnit = AdUnit(
                    demandId = "gam",
                    pricefloor = 2.6,
                    label = "label123",
                    bidType = BidType.RTB,
                    ext = jsonObject {
                        "ad_unit_id" hasValue "ad_unit_id4"
                        "payload" hasValue "test_payload"
                    }.toString(),
                    timeout = 5000,
                    uid = "uid123"
                ),
                optBannerFormat = BannerFormat.Banner,
                optContainerWidth = 140f,
            )
        }
        val actual = testee.invoke(
            auctionParamsScope = auctionParamsScope,
            adType = AdType.Banner,
        ).getOrThrow()

        require(actual is GamBannerAuctionParams.Bidding)
        assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "gam",
                pricefloor = 2.6,
                label = "label123",
                bidType = BidType.RTB,
                ext = jsonObject {
                    "ad_unit_id" hasValue "ad_unit_id4"
                    "payload" hasValue "test_payload"
                }.toString(),
                timeout = 5000,
                uid = "uid123"
            )
        )
        assertThat(actual.price).isEqualTo(2.6)
        assertThat(actual.payload).isEqualTo("test_payload")
        assertThat(actual.adUnitId).isEqualTo("ad_unit_id4")
    }

    @Test
    fun `parse banner AdUnit CPM`() {
        val auctionParamsScope by lazy {
            AdAuctionParamSource(
                activity = activity,
                pricefloor = 3.5,
                adUnit = AdUnit(
                    demandId = "gam",
                    pricefloor = 3.5,
                    label = "label888",
                    bidType = BidType.CPM,
                    ext = jsonObject {
                        "ad_unit_id" hasValue "ad_unit_id888"
                    }.toString(),
                    timeout = 5000,
                    uid = "uid123"
                ),
                optBannerFormat = BannerFormat.MRec,
                optContainerWidth = 140f,
            )
        }
        val actual = testee.invoke(
            auctionParamsScope = auctionParamsScope,
            adType = AdType.Banner,
        ).getOrThrow()

        require(actual is GamBannerAuctionParams.Network)
        assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "gam",
                pricefloor = 3.5,
                label = "label888",
                bidType = BidType.CPM,
                ext = jsonObject {
                    "ad_unit_id" hasValue "ad_unit_id888"
                }.toString(),
                timeout = 5000,
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
                pricefloor = 2.6,
                adUnit = AdUnit(
                    demandId = "gam",
                    pricefloor = 2.6,
                    label = "label123",
                    bidType = BidType.RTB,
                    ext = jsonObject {
                        "ad_unit_id" hasValue "ad_unit_id4"
                        "payload" hasValue "test_payload"
                    }.toString(),
                    timeout = 5000,
                    uid = "uid123"
                ),
                optBannerFormat = BannerFormat.MRec,
                optContainerWidth = 140f,
            )
        }
        val actual = testee.invoke(
            auctionParamsScope = auctionParamsScope,
            adType = AdType.Rewarded,
        ).getOrThrow()

        require(actual is GamFullscreenAdAuctionParams.Bidding)
        assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "gam",
                pricefloor = 2.6,
                label = "label123",
                bidType = BidType.RTB,
                ext = jsonObject {
                    "ad_unit_id" hasValue "ad_unit_id4"
                    "payload" hasValue "test_payload"
                }.toString(),
                timeout = 5000,
                uid = "uid123"
            )
        )
        assertThat(actual.price).isEqualTo(2.6)
        assertThat(actual.payload).isEqualTo("test_payload")
        assertThat(actual.adUnitId).isEqualTo("ad_unit_id4")
    }

    @Test
    fun `parse fullscreen AdUnit CPM`() {
        val auctionParamsScope by lazy {
            AdAuctionParamSource(
                activity = activity,
                pricefloor = 2.75,
                adUnit = AdUnit(
                    demandId = "gam",
                    pricefloor = 3.5,
                    label = "label888",
                    bidType = BidType.CPM,
                    ext = jsonObject {
                        "ad_unit_id" hasValue "ad_unit_id888"
                    }.toString(),
                    timeout = 5000,
                    uid = "uid123"
                ),
                optBannerFormat = null,
                optContainerWidth = null,
            )
        }
        val actual = testee.invoke(
            auctionParamsScope = auctionParamsScope,
            adType = AdType.Interstitial,
        ).getOrThrow()

        // parse fullscreen AdUnit CPM
        require(actual is GamFullscreenAdAuctionParams.Network)
        assertThat(actual.adUnit).isEqualTo(
            AdUnit(
                demandId = "gam",
                pricefloor = 3.5,
                label = "label888",
                bidType = BidType.CPM,
                ext = jsonObject {
                    "ad_unit_id" hasValue "ad_unit_id888"
                }.toString(),
                timeout = 5000,
                uid = "uid123"
            )
        )
        assertThat(actual.price).isEqualTo(3.5)
        assertThat(actual.adUnitId).isEqualTo("ad_unit_id888")
    }
}