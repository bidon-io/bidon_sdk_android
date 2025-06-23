package com.applovin.mediation.adapters.banner

import android.app.Activity
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.adapter.MaxAdapterError
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters
import com.applovin.mediation.adapters.ext.getAsDouble
import com.applovin.mediation.adapters.keeper.AdKeeper
import com.applovin.mediation.adapters.keeper.AdKeepers
import com.applovin.mediation.adapters.mockk.mockkLog
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
        mockkLog()

        mockActivity = mockk(relaxed = true)
        mockListener = mockk(relaxed = true)
        mockParameters = mockk(relaxed = true)
        mockAdKeeper = mockk()

        mockkObject(AdKeepers)

        // Mock AppLovin MAX parameters
        every { mockParameters.adUnitId } returns "test_ad_unit_id"

        // Mock AdKeepers
        every { AdKeepers.getKeeper<BannerAdInstance>(any(), MaxAdFormat.BANNER) } returns mockAdKeeper
        every { mockAdKeeper.lastRegisteredEcpm() } returns null
        every { mockAdKeeper.registerEcpm(any()) } just Runs
        every { mockAdKeeper.keepAd(any()) } returns null

        // Mock consumed ad instance
        val mockConsumedAd = mockk<BannerAdInstance>()
        every { mockConsumedAd.setListener(any()) } just Runs
        every { mockConsumedAd.show() } just Runs
        every { mockConsumedAd.bannerAd } returns mockk()
        every { mockAdKeeper.consumeAd(any()) } returns mockConsumedAd

        // Mock BannerAdInstance creation
        mockkConstructor(BannerAdInstance::class)
        every { anyConstructed<BannerAdInstance>().load(any()) } just Runs
        every { anyConstructed<BannerAdInstance>().addExtra(any(), any()) } just Runs

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
            every { getBoolean("unicorn", false) } returns true
            every { getBoolean("should_load", false) } returns false
            every { getAsDouble("ecpm") } returns 1.0
        }

        // When
        bidonBanner.loadAdViewAd(mockParameters, MaxAdFormat.BANNER, null, mockListener)

        // Then
        verify { mockListener.onAdViewAdLoadFailed(MaxAdapterError.MISSING_ACTIVITY) }
    }

    @Test
    fun `loadAdViewAd with unicorn true should load new ad and register ecpm`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns true
            every { getBoolean("should_load", false) } returns false
            every { getAsDouble("ecpm") } returns 2.0
            every { getString("auction_key", null) } returns "test_auction_key"
        }
        every { mockAdKeeper.lastRegisteredEcpm() } returns null

        // When
        bidonBanner.loadAdViewAd(mockParameters, MaxAdFormat.BANNER, mockActivity, mockListener)

        // Then
        verify { AdKeepers.getKeeper<BannerAdInstance>("test_ad_unit_id", MaxAdFormat.BANNER) }
        verify { mockAdKeeper.registerEcpm(2.0) }
        verify { anyConstructed<BannerAdInstance>().load(mockActivity) }
        verify { anyConstructed<BannerAdInstance>().addExtra("previous_auction_price", null) }
    }

    @Test
    fun `loadAdViewAd with unicorn true and last ecpm should pass it as extra`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns true
            every { getBoolean("should_load", false) } returns false
            every { getAsDouble("ecpm") } returns 2.0
            every { getString("auction_key", null) } returns "test_auction_key"
        }
        every { mockAdKeeper.lastRegisteredEcpm() } returns 1.5

        // When
        bidonBanner.loadAdViewAd(mockParameters, MaxAdFormat.BANNER, mockActivity, mockListener)

        // Then
        verify { anyConstructed<BannerAdInstance>().addExtra("previous_auction_price", 1.5) }
        verify { anyConstructed<BannerAdInstance>().load(mockActivity) }
    }

    @Test
    fun `loadAdViewAd with unicorn false and no available ad should fail`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns false
            every { getBoolean("should_load", false) } returns false
            every { getAsDouble("ecpm") } returns 1.0
        }
        every { mockAdKeeper.consumeAd(any()) } returns null

        // When
        bidonBanner.loadAdViewAd(mockParameters, MaxAdFormat.BANNER, mockActivity, mockListener)

        // Then
        verify { mockListener.onAdViewAdLoadFailed(MaxAdapterError.NO_FILL) }
    }

    @Test
    fun `loadAdViewAd with should_load true should load new ad and register ecpm`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns false
            every { getBoolean("should_load", false) } returns true
            every { getAsDouble("ecpm") } returns 2.0
            every { getString("auction_key", null) } returns "test_auction_key"
        }
        every { mockAdKeeper.lastRegisteredEcpm() } returns null

        // When
        bidonBanner.loadAdViewAd(mockParameters, MaxAdFormat.BANNER, mockActivity, mockListener)

        // Then
        verify { AdKeepers.getKeeper<BannerAdInstance>("test_ad_unit_id", MaxAdFormat.BANNER) }
        verify { mockAdKeeper.registerEcpm(2.0) }
        verify { anyConstructed<BannerAdInstance>().load(mockActivity) }
        verify { anyConstructed<BannerAdInstance>().addExtra("previous_auction_price", null) }
    }

    @Test
    fun `loadAdViewAd with both should_load and unicorn true should prioritize should_load`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns true
            every { getBoolean("should_load", false) } returns true
            every { getAsDouble("ecpm") } returns 2.0
            every { getString("auction_key", null) } returns "test_auction_key"
        }
        every { mockAdKeeper.lastRegisteredEcpm() } returns null

        // When
        bidonBanner.loadAdViewAd(mockParameters, MaxAdFormat.BANNER, mockActivity, mockListener)

        // Then
        verify { AdKeepers.getKeeper<BannerAdInstance>("test_ad_unit_id", MaxAdFormat.BANNER) }
        verify { mockAdKeeper.registerEcpm(2.0) }
        verify { anyConstructed<BannerAdInstance>().load(mockActivity) }
        verify { anyConstructed<BannerAdInstance>().addExtra("previous_auction_price", null) }
    }

    @Test
    fun `loadAdViewAd with should_load false and unicorn true should load new ad`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns true
            every { getBoolean("should_load", false) } returns false
            every { getAsDouble("ecpm") } returns 2.0
            every { getString("auction_key", null) } returns "test_auction_key"
        }
        every { mockAdKeeper.lastRegisteredEcpm() } returns null

        // When
        bidonBanner.loadAdViewAd(mockParameters, MaxAdFormat.BANNER, mockActivity, mockListener)

        // Then
        verify { AdKeepers.getKeeper<BannerAdInstance>("test_ad_unit_id", MaxAdFormat.BANNER) }
        verify { mockAdKeeper.registerEcpm(2.0) }
        verify { anyConstructed<BannerAdInstance>().load(mockActivity) }
        verify { anyConstructed<BannerAdInstance>().addExtra("previous_auction_price", null) }
    }

    @Test
    fun `loadAdViewAd with both should_load and unicorn false should consume ad from keeper`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns false
            every { getBoolean("should_load", false) } returns false
            every { getAsDouble("ecpm") } returns 1.0
        }

        // When
        bidonBanner.loadAdViewAd(mockParameters, MaxAdFormat.BANNER, mockActivity, mockListener)

        // Then
        verify { mockAdKeeper.consumeAd(1.0) }
        verify { mockListener.onAdViewAdLoaded(any()) }
    }
}
