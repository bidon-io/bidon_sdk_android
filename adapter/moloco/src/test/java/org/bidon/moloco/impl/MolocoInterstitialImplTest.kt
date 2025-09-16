package org.bidon.moloco.impl

import android.app.Activity
import com.moloco.sdk.publisher.InterstitialAd
import com.moloco.sdk.publisher.MediationInfo
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.MolocoAdError
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.jsonObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MolocoInterstitialImplTest {

    private val activity = mockk<Activity>(relaxed = true)
    private val testee = MolocoInterstitialImpl()

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
            pricefloor = 3.0,
            adUnit = AdUnit(
                demandId = "moloco",
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
    }

    @Test
    fun `getAuctionParam should handle missing ad_unit_id`() {
        val auctionParamsScope = AdAuctionParamSource(
            activity = activity,
            pricefloor = 3.0,
            adUnit = AdUnit(
                demandId = "moloco",
                pricefloor = 3.0,
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
    fun `load should create interstitial successfully`() {
        val mockInterstitial = mockk<InterstitialAd>(relaxed = true) {
            every { isLoaded } returns true
        }
        every {
            Moloco.createInterstitial(any<MediationInfo>(), any<String>(), any(), any())
        } answers {
            val callback = arg<(InterstitialAd?, MolocoAdError.AdCreateError?) -> Unit>(3)
            callback.invoke(mockInterstitial, null)
        }

        val adParams = MolocoFullscreenAuctionParams(
            adUnit = AdUnit(
                demandId = "moloco",
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

        testee.load(adParams)

        verify {
            Moloco.createInterstitial(
                any<MediationInfo>(),
                eq("test_ad_unit_id"),
                any(),
                any()
            )
        }
        assertTrue(testee.isAdReadyToShow)
    }

    @Test
    fun `load should handle creation failure`() {
        val mockError = mockk<MolocoAdError.AdCreateError> {
            every { description } returns "Creation failed"
        }
        every {
            Moloco.createInterstitial(any<MediationInfo>(), any<String>(), any(), any())
        } answers {
            val callback = arg<(InterstitialAd?, MolocoAdError.AdCreateError?) -> Unit>(3)
            callback.invoke(null, mockError)
        }

        val adParams = MolocoFullscreenAuctionParams(
            adUnit = AdUnit(
                demandId = "moloco",
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

        testee.load(adParams)

        assertFalse(testee.isAdReadyToShow)
    }

    @Test
    fun `destroy should clean up resources`() {
        testee.destroy()

        assertFalse(testee.isAdReadyToShow)
    }
}
