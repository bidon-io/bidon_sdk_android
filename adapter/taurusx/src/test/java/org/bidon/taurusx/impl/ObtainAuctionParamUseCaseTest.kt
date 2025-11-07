package org.bidon.taurusx.impl

import android.app.Activity
import io.mockk.mockk
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.jsonObject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObtainAuctionParamUseCaseTest {

    private val activity = mockk<Activity>(relaxed = true)
    private val testee = ObtainAuctionParamUseCase()

    @Test
    fun `getFullscreenParam should create TaurusXFullscreenAuctionParams correctly`() {
        val auctionParamsScope = AdAuctionParamSource(
            activity = activity,
            pricefloor = 3.5,
            adUnit = AdUnit(
                demandId = "taurusx",
                pricefloor = 3.5,
                label = "test_label",
                bidType = BidType.RTB,
                ext = jsonObject {
                    "ad_unit_id" hasValue "test_interstitial_id"
                    "payload" hasValue "test_payload_data"
                }.toString(),
                timeout = 5000,
                uid = "test_uid"
            ),
            optBannerFormat = null,
            optContainerWidth = null
        )

        val result = testee.getFullscreenParam(auctionParamsScope)

        assertTrue(result.isSuccess)
        val params = result.getOrNull() as TaurusXFullscreenAuctionParams
        assertEquals("test_interstitial_id", params.adUnitId)
        assertEquals("test_payload_data", params.payload)
        assertEquals(3.5, params.price)
        assertEquals(activity.applicationContext, params.context)
    }

    @Test
    fun `getFullscreenParam should handle missing parameters`() {
        val auctionParamsScope = AdAuctionParamSource(
            activity = activity,
            pricefloor = 2.0,
            adUnit = AdUnit(
                demandId = "taurusx",
                pricefloor = 2.0,
                label = "test_label",
                bidType = BidType.CPM,
                ext = null, // No ext field
                timeout = 5000,
                uid = "test_uid"
            ),
            optBannerFormat = null,
            optContainerWidth = null
        )

        val result = testee.getFullscreenParam(auctionParamsScope)

        assertTrue(result.isSuccess)
        val params = result.getOrNull() as TaurusXFullscreenAuctionParams
        assertEquals(null, params.adUnitId) // getString returns null when extra is null
        assertEquals(null, params.payload) // optString returns null when extra is null
        assertEquals(2.0, params.price)
    }

    @Test
    fun `getBannerParam should create TaurusXBannerAuctionParams correctly`() {
        val auctionParamsScope = AdAuctionParamSource(
            activity = activity,
            pricefloor = 1.5,
            adUnit = AdUnit(
                demandId = "taurusx",
                pricefloor = 1.5,
                label = "test_banner_label",
                bidType = BidType.RTB,
                ext = jsonObject {
                    "ad_unit_id" hasValue "test_banner_id"
                    "payload" hasValue "test_banner_payload"
                }.toString(),
                timeout = 3000,
                uid = "test_banner_uid"
            ),
            optBannerFormat = BannerFormat.Banner,
            optContainerWidth = 320f
        )

        val result = testee.getBannerParam(auctionParamsScope)

        assertTrue(result.isSuccess)
        val params = result.getOrNull() as TaurusXBannerAuctionParams
        assertEquals("test_banner_id", params.adUnitId)
        assertEquals("test_banner_payload", params.payload)
        assertEquals(1.5, params.price)
        assertEquals(activity, params.activity)
    }

    @Test
    fun `getBannerParam should handle MRec format`() {
        val auctionParamsScope = AdAuctionParamSource(
            activity = activity,
            pricefloor = 2.5,
            adUnit = AdUnit(
                demandId = "taurusx",
                pricefloor = 2.5,
                label = "test_mrec_label",
                bidType = BidType.RTB,
                ext = jsonObject {
                    "ad_unit_id" hasValue "test_mrec_id"
                    "payload" hasValue "test_mrec_payload"
                }.toString(),
                timeout = 3000,
                uid = "test_mrec_uid"
            ),
            optBannerFormat = BannerFormat.MRec,
            optContainerWidth = 300f
        )

        val result = testee.getBannerParam(auctionParamsScope)

        assertTrue(result.isSuccess)
        val params = result.getOrNull() as TaurusXBannerAuctionParams
        assertEquals("test_mrec_id", params.adUnitId)
    }

    @Test
    fun `getBannerParam should handle missing parameters`() {
        val auctionParamsScope = AdAuctionParamSource(
            activity = activity,
            pricefloor = 1.0,
            adUnit = AdUnit(
                demandId = "taurusx",
                pricefloor = 1.0,
                label = "test_label",
                bidType = BidType.CPM,
                ext = null, // No ext field
                timeout = 3000,
                uid = "test_uid"
            ),
            optBannerFormat = BannerFormat.Banner,
            optContainerWidth = 320f
        )

        val result = testee.getBannerParam(auctionParamsScope)

        assertTrue(result.isSuccess)
        val params = result.getOrNull() as TaurusXBannerAuctionParams
        assertEquals(null, params.adUnitId) // getString returns null when extra is null
        assertEquals(null, params.payload) // optString returns null when extra is null
        assertEquals(1.0, params.price)
    }
}
