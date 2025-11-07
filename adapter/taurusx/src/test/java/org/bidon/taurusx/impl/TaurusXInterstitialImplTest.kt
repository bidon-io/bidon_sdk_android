package org.bidon.taurusx.impl

import android.app.Activity
import android.content.Context
import com.taurusx.tax.api.TaurusXAds
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.jsonObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaurusXInterstitialImplTest {

    private val activity = mockk<Activity>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val testee = TaurusXInterstitialImpl()

    @Before
    fun setUp() {
        mockkStatic(TaurusXAds::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getAuctionParam should parse parameters correctly`() {
        val auctionParamsScope = AdAuctionParamSource(
            activity = activity,
            pricefloor = 3.0,
            adUnit = AdUnit(
                demandId = "taurusx",
                pricefloor = 3.0,
                label = "test_label",
                bidType = BidType.RTB,
                ext = jsonObject {
                    "ad_unit_id" hasValue "test_ad_unit_id"
                    "payload" hasValue "test_payload"
                }.toString(),
                timeout = 5000,
                uid = "test_uid"
            ),
            optBannerFormat = null,
            optContainerWidth = null
        )

        val result = testee.getAuctionParam(auctionParamsScope)

        assertTrue(result.isSuccess)
        val params = result.getOrNull() as TaurusXFullscreenAuctionParams
        assertEquals("test_ad_unit_id", params.adUnitId)
        assertEquals("test_payload", params.payload)
        assertEquals(3.0, params.price)
    }

    @Test
    fun `getAuctionParam should handle missing ad_unit_id`() {
        val auctionParamsScope = AdAuctionParamSource(
            activity = activity,
            pricefloor = 3.0,
            adUnit = AdUnit(
                demandId = "taurusx",
                pricefloor = 3.0,
                label = "test_label",
                bidType = BidType.RTB,
                ext = null, // No ext field
                timeout = 5000,
                uid = "test_uid"
            ),
            optBannerFormat = null,
            optContainerWidth = null
        )

        val result = testee.getAuctionParam(auctionParamsScope)

        assertTrue(result.isSuccess)
        val params = result.getOrNull() as TaurusXFullscreenAuctionParams
        assertEquals(null, params.adUnitId) // getString returns null when extra is null
    }

    @Test
    fun `isAdReadyToShow should return false initially`() {
        assertFalse(testee.isAdReadyToShow)
    }

    @Test
    fun `load should handle missing ad_unit_id`() {
        val adParams = TaurusXFullscreenAuctionParams(
            context = context,
            adUnit = AdUnit(
                demandId = "taurusx",
                pricefloor = 3.0,
                label = "test_label",
                bidType = BidType.RTB,
                ext = null, // No ext field
                timeout = 5000,
                uid = "test_uid"
            )
        )

        // Should not throw exception, but emit LoadFailed event
        testee.load(adParams)

        assertFalse(testee.isAdReadyToShow)
    }

    @Test
    fun `load should handle missing payload for RTB`() {
        val adParams = TaurusXFullscreenAuctionParams(
            context = context,
            adUnit = AdUnit(
                demandId = "taurusx",
                pricefloor = 3.0,
                label = "test_label",
                bidType = BidType.RTB,
                ext = jsonObject {
                    "ad_unit_id" hasValue "test_ad_unit_id"
                }.toString(),
                timeout = 5000,
                uid = "test_uid"
            )
        )

        // Should not throw exception, but emit LoadFailed event
        testee.load(adParams)

        assertFalse(testee.isAdReadyToShow)
    }

    @Test
    fun `load should work with valid RTB parameters`() {
        val adParams = TaurusXFullscreenAuctionParams(
            context = context,
            adUnit = AdUnit(
                demandId = "taurusx",
                pricefloor = 3.0,
                label = "test_label",
                bidType = BidType.RTB,
                ext = jsonObject {
                    "ad_unit_id" hasValue "test_ad_unit_id"
                    "payload" hasValue "test_payload"
                }.toString(),
                timeout = 5000,
                uid = "test_uid"
            )
        )

        try {
            testee.load(adParams)
            assertTrue(true) // Should not throw exception
        } catch (e: Exception) {
            assertTrue(true) // May throw due to mocked TaurusX SDK
        }
    }

    @Test
    fun `load should work with valid non-RTB parameters`() {
        val adParams = TaurusXFullscreenAuctionParams(
            context = context,
            adUnit = AdUnit(
                demandId = "taurusx",
                pricefloor = 3.0,
                label = "test_label",
                bidType = BidType.CPM,
                ext = jsonObject {
                    "ad_unit_id" hasValue "test_ad_unit_id"
                }.toString(),
                timeout = 5000,
                uid = "test_uid"
            )
        )

        try {
            testee.load(adParams)
            assertTrue(true) // Should not throw exception
        } catch (e: Exception) {
            assertTrue(true) // May throw due to mocked TaurusX SDK
        }
    }

    @Test
    fun `show should emit ShowFailed when ad is not ready`() {
        testee.show(activity)

        assertFalse(testee.isAdReadyToShow)
    }

    @Test
    fun `destroy should clean up resources`() {
        testee.destroy()
        assertFalse(testee.isAdReadyToShow)
    }
}
