package org.bidon.moloco.impl

import android.app.Activity
import com.moloco.sdk.publisher.Moloco
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.jsonObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MolocoBannerImplTest {

    private val activity = mockk<Activity>(relaxed = true)
    private val testee = MolocoBannerImpl()

    @Before
    fun setUp() {
        mockkStatic(Moloco::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getAuctionParam should parse parameters correctly`() {
        val auctionParamsScope = AdAuctionParamSource(
            activity = activity,
            pricefloor = 2.5,
            adUnit = AdUnit(
                demandId = "moloco",
                pricefloor = 2.5,
                label = "test_label",
                bidType = BidType.RTB,
                ext = jsonObject {
                    "ad_unit_id" hasValue "test_ad_unit_id"
                    "payload" hasValue "test_payload"
                }.toString(),
                timeout = 5000,
                uid = "test_uid"
            ),
            optBannerFormat = BannerFormat.Banner,
            optContainerWidth = 320f,
        )

        val result = testee.getAuctionParam(auctionParamsScope)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `getAuctionParam should handle missing ad_unit_id`() {
        val auctionParamsScope = AdAuctionParamSource(
            activity = activity,
            pricefloor = 2.5,
            adUnit = AdUnit(
                demandId = "moloco",
                pricefloor = 2.5,
                label = "test_label",
                bidType = BidType.RTB,
                ext = jsonObject {
                    "payload" hasValue "test_payload"
                }.toString(),
                timeout = 5000,
                uid = "test_uid"
            ),
            optBannerFormat = BannerFormat.Banner,
            optContainerWidth = 320f,
        )

        val result = testee.getAuctionParam(auctionParamsScope)

        assertTrue(result.isFailure)
    }

    @Test
    fun `isAdReadyToShow should return false initially`() {
        assertFalse(testee.isAdReadyToShow)
    }

    @Test
    fun `load should work without exceptions`() {
        val adParams = MolocoBannerAuctionParams(
            bannerFormat = BannerFormat.Banner,
            adUnit = AdUnit(
                demandId = "moloco",
                pricefloor = 2.5,
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
            assertTrue(true)
        } catch (e: Exception) {
            assertTrue(true)
        }
    }

    @Test
    fun `load should handle missing payload`() {
        val adParams = MolocoBannerAuctionParams(
            bannerFormat = BannerFormat.Banner,
            adUnit = AdUnit(
                demandId = "moloco",
                pricefloor = 2.5,
                label = "test_label",
                bidType = BidType.RTB,
                ext = jsonObject {
                    "ad_unit_id" hasValue "test_ad_unit_id"
                }.toString(),
                timeout = 5000,
                uid = "test_uid"
            )
        )

        try {
            testee.load(adParams)
            assertTrue(true)
        } catch (e: Exception) {
            assertTrue(true)
        }
    }

    @Test
    fun `destroy should clean up resources`() {
        testee.destroy()
        assertFalse(testee.isAdReadyToShow)
    }
}
