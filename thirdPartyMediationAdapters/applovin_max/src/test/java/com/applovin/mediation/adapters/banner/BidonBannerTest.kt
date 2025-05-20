package com.applovin.mediation.adapters.banner

import android.app.Activity
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.adapter.MaxAdapterError
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters
import com.applovin.mediation.adapters.ext.getAsDouble
import com.applovin.mediation.adapters.keeper.AdKeeper
import com.applovin.mediation.adapters.keeper.AdKeepers
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class BidonBannerTest {

    private lateinit var bidonBanner: BidonBanner

    private lateinit var mockActivity: Activity
    private lateinit var mockListener: MaxAdViewAdapterListener
    private lateinit var mockParameters: MaxAdapterResponseParameters
    private lateinit var mockAdKeeper: AdKeeper<BannerAdInstance>

    @Before
    fun setup() {
        mockActivity = mockk(relaxed = true)
        mockListener = mockk(relaxed = true)
        mockParameters = mockk(relaxed = true)
        mockAdKeeper = mockk()

        mockkObject(AdKeepers)

        every { AdKeepers.getBannerKeeper(any()) } returns mockAdKeeper
        every { mockAdKeeper.lastRegisteredEcpm() } returns null
        every { mockAdKeeper.registerEcpm(any()) } just Runs
        every { mockAdKeeper.keepAd(any()) } returns null

        // Mock consumed ad instance
        val mockConsumedAd = mockk<BannerAdInstance>()
        every { mockConsumedAd.setListener(any()) } just Runs
        every { mockConsumedAd.show() } just Runs
        every { mockConsumedAd.bannerAd } returns mockk()
        every { mockAdKeeper.consumeAd(any()) } returns mockConsumedAd

        // Mock BannerAdInstance constructor
        mockkConstructor(BannerAdInstance::class)
        every { anyConstructed<BannerAdInstance>().load(any()) } just Runs

        bidonBanner = BidonBanner()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `loadAdViewAd with null activity should fail with missing activity error`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn") } returns true
            every { getAsDouble("ecpm") } returns 1.0
        }

        // When
        bidonBanner.loadAdViewAd(mockParameters, MaxAdFormat.BANNER, null, mockListener)

        // Then
        verify { mockListener.onAdViewAdLoadFailed(MaxAdapterError.MISSING_ACTIVITY) }
    }

    @Test
    fun `loadAdViewAd with unicorn true and null last ecpm should load new ad`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn") } returns true
            every { getAsDouble("ecpm") } returns 2.0
            every { getString("auction_key", null) } returns "test_auction_key"
        }
        every { mockAdKeeper.lastRegisteredEcpm() } returns null

        // When
        bidonBanner.loadAdViewAd(mockParameters, MaxAdFormat.BANNER, mockActivity, mockListener)

        // Then
        verify { anyConstructed<BannerAdInstance>().load(mockActivity) }
        verify { mockAdKeeper.registerEcpm(2.0) }
        verify { anyConstructed<BannerAdInstance>().addExtra("previous_auction_price", null) }
    }

    @Test
    fun `loadAdViewAd with unicorn true and existing last ecpm should load new ad`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn") } returns true
            every { getAsDouble("ecpm") } returns 2.0
            every { getString("auction_key", null) } returns "test_auction_key"
        }
        every { mockAdKeeper.lastRegisteredEcpm() } returns 1.5

        // When
        bidonBanner.loadAdViewAd(mockParameters, MaxAdFormat.BANNER, mockActivity, mockListener)

        // Then
        verify { anyConstructed<BannerAdInstance>().load(mockActivity) }
        verify { mockAdKeeper.registerEcpm(2.0) }
        verify { anyConstructed<BannerAdInstance>().addExtra("previous_auction_price", 1.5) }
    }

    @Test
    fun `loadAdViewAd with unicorn false should consume ad from keeper`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn") } returns false
            every { getAsDouble("ecpm") } returns 1.0
        }

        // When
        bidonBanner.loadAdViewAd(mockParameters, MaxAdFormat.BANNER, mockActivity, mockListener)

        // Then
        verify { mockAdKeeper.consumeAd(1.0) }
        verify { mockListener.onAdViewAdLoaded(any()) }
    }

    @Test
    fun `loadAdViewAd with unicorn false and no available ad should fail`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn") } returns false
            every { getAsDouble("ecpm") } returns 1.0
        }
        every { mockAdKeeper.consumeAd(any()) } returns null

        // When
        bidonBanner.loadAdViewAd(mockParameters, MaxAdFormat.BANNER, mockActivity, mockListener)

        // Then
        verify { mockListener.onAdViewAdLoadFailed(MaxAdapterError.NO_FILL) }
    }
}
