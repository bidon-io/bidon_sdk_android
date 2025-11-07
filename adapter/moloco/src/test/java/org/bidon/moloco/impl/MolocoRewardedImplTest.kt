package org.bidon.moloco.impl

import android.app.Activity
import com.moloco.sdk.publisher.Moloco
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MolocoRewardedImplTest {

    private val activity = mockk<Activity>(relaxed = true)
    private val testee = MolocoRewardedImpl()

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
            pricefloor = 4.0,
            adUnit = AdUnit(
                demandId = "moloco",
                pricefloor = 4.0,
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
    }

    @Test
    fun `getAuctionParam should handle missing ad_unit_id`() {
        val auctionParamsScope = AdAuctionParamSource(
            activity = activity,
            pricefloor = 4.0,
            adUnit = AdUnit(
                demandId = "moloco",
                pricefloor = 4.0,
                label = "test_label",
                bidType = BidType.RTB,
                ext = jsonObject {
                    "payload" hasValue "test_payload"
                }.toString(),
                timeout = 5000,
                uid = "test_uid"
            ),
            optBannerFormat = null,
            optContainerWidth = null
        )

        val result = testee.getAuctionParam(auctionParamsScope)

        assertTrue(result.isFailure)
    }

    @Test
    fun `isAdReadyToShow should return false initially`() {
        assertFalse(testee.isAdReadyToShow)
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
