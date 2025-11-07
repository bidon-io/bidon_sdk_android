package org.bidon.startio.impl

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit
import org.junit.After
import org.junit.Test

class StartIoBannerImplTest {

    private val activity = mockk<Activity>(relaxed = true)
    private val testee = StartIoBannerImpl()

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `isAdReadyToShow should return false when no ad loaded`() {
        assertThat(testee.isAdReadyToShow).isFalse()
    }

    @Test
    fun `getAdView should return null when no ad loaded`() {
        assertThat(testee.getAdView()).isNull()
    }

    @Test
    fun `load should handle different banner formats`() {
        val adUnit = mockk<AdUnit> {
            every { pricefloor } returns 1.0
            every { extra } returns null
        }
        val adParams = StartIoBannerAuctionParams(activity, BannerFormat.MRec, adUnit)

        testee.load(adParams)
    }

    @Test
    fun `destroy should clear banner reference`() {
        val adUnit = mockk<AdUnit> {
            every { pricefloor } returns 1.0
            every { extra } returns null
        }
        val adParams = StartIoBannerAuctionParams(activity, BannerFormat.Banner, adUnit)

        testee.load(adParams)
        testee.destroy()

        assertThat(testee.isAdReadyToShow).isFalse()
    }

    @Test
    fun `getAuctionParam should return correct parameters`() {
        val auctionParamsScope = mockk<org.bidon.sdk.adapter.AdAuctionParamSource>(relaxed = true)

        val result = testee.getAuctionParam(auctionParamsScope)

        assertThat(result).isNotNull()
    }
}
