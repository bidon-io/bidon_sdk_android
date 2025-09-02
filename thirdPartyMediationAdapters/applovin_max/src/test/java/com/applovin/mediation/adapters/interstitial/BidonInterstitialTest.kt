package com.applovin.mediation.adapters.interstitial

import android.app.Activity
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.adapter.MaxAdapterError
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener
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

class BidonInterstitialTest {

    private lateinit var bidonInterstitial: BidonInterstitial

    private lateinit var mockActivity: Activity
    private lateinit var mockListener: MaxInterstitialAdapterListener
    private lateinit var mockParameters: MaxAdapterResponseParameters
    private lateinit var mockAdKeeper: AdKeeper<InterstitialAdInstance>

    @Before
    fun setup() {
        mockkLog()

        mockActivity = mockk()
        mockListener = mockk(relaxed = true)
        mockParameters = mockk(relaxed = true)
        mockAdKeeper = mockk()

        mockkObject(AdKeepers)

        every { AdKeepers.getKeeper<InterstitialAdInstance>(any(), any()) } returns mockAdKeeper
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns false
            every { getBoolean("should_load", false) } returns false
            every { getAsDouble("ecpm") } returns 1.0
        }
        every { mockParameters.adUnitId } returns "test_ad_unit_id"

        every { mockAdKeeper.lastRegisteredEcpm() } returns null
        every { mockAdKeeper.registerEcpm(any()) } just Runs
        every { mockAdKeeper.keepAd(any()) } returns null

        // Mock consumed ad instance
        val mockConsumedAd = mockk<InterstitialAdInstance>()
        every { mockConsumedAd.setListener(any()) } just Runs
        every { mockConsumedAd.uid } returns "test-uid"
        every { mockConsumedAd.bidType } returns "test-bid-type"
        every { mockConsumedAd.ecpm } returns 1.0
        every { mockConsumedAd.demandId } returns "test-demand-id"
        every { mockConsumedAd.notifyWin() } just Runs
        every { mockAdKeeper.consumeAd(any()) } returns mockConsumedAd

        // Mock listener methods
        every { mockListener.onInterstitialAdLoaded() } just Runs
        every { mockListener.onInterstitialAdLoadFailed(any()) } just Runs
        every { mockListener.onInterstitialAdDisplayFailed(any()) } just Runs

        // Mock InterstitialAdInstance creation
        mockkConstructor(InterstitialAdInstance::class)
        every { anyConstructed<InterstitialAdInstance>().load(any()) } just Runs
        every { anyConstructed<InterstitialAdInstance>().addExtra(any(), any()) } just Runs

        bidonInterstitial = BidonInterstitial()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `loadInterstitialAd with null activity should fail with missing activity error`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns true
            every { getAsDouble("ecpm") } returns 1.0
        }

        // When
        bidonInterstitial.loadInterstitialAd(mockParameters, null, mockListener)

        // Then
        verify { mockListener.onInterstitialAdLoadFailed(MaxAdapterError.MISSING_ACTIVITY) }
    }

    @Test
    fun `loadInterstitialAd with unicorn true should load new ad and register ecpm`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns true
            every { getAsDouble("ecpm") } returns 2.0
            every { getString("auction_key", null) } returns "test_auction_key"
        }
        every { mockAdKeeper.lastRegisteredEcpm() } returns null

        // When
        bidonInterstitial.loadInterstitialAd(mockParameters, mockActivity, mockListener)

        // Then
        verify { AdKeepers.getKeeper<InterstitialAdInstance>("test_ad_unit_id", MaxAdFormat.INTERSTITIAL) }
        verify { mockAdKeeper.registerEcpm(2.0) }
        verify { anyConstructed<InterstitialAdInstance>().load(mockActivity) }
        verify { anyConstructed<InterstitialAdInstance>().addExtra("previous_auction_price", null) }
    }

    @Test
    fun `loadInterstitialAd with unicorn true and last ecpm should pass it as extra`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns true
            every { getAsDouble("ecpm") } returns 2.0
            every { getString("auction_key", null) } returns "test_auction_key"
        }
        every { mockAdKeeper.lastRegisteredEcpm() } returns 1.5

        // When
        bidonInterstitial.loadInterstitialAd(mockParameters, mockActivity, mockListener)

        // Then
        verify { anyConstructed<InterstitialAdInstance>().addExtra("previous_auction_price", 1.5) }
        verify { anyConstructed<InterstitialAdInstance>().load(mockActivity) }
    }

    @Test
    fun `loadInterstitialAd with unicorn false and no available ad should fail`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns false
            every { getBoolean("should_load", false) } returns false
            every { getAsDouble("ecpm") } returns 1.0
        }
        every { mockAdKeeper.consumeAd(any()) } returns null

        // When
        bidonInterstitial.loadInterstitialAd(mockParameters, mockActivity, mockListener)

        // Then
        verify { mockListener.onInterstitialAdLoadFailed(MaxAdapterError.NO_FILL) }
    }

    @Test
    fun `showInterstitialAd with not ready ad should fail`() {
        // Given

        // When
        bidonInterstitial.showInterstitialAd(mockParameters, mockActivity, mockListener)

        // Then
        verify { mockListener.onInterstitialAdDisplayFailed(MaxAdapterError.AD_DISPLAY_FAILED) }
    }

    @Test
    fun `loadInterstitialAd with should_load true should load new ad and register ecpm`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns false
            every { getBoolean("should_load", false) } returns true
            every { getAsDouble("ecpm") } returns 2.0
            every { getString("auction_key", null) } returns "test_auction_key"
        }
        every { mockAdKeeper.lastRegisteredEcpm() } returns null

        // When
        bidonInterstitial.loadInterstitialAd(mockParameters, mockActivity, mockListener)

        // Then
        verify { AdKeepers.getKeeper<InterstitialAdInstance>("test_ad_unit_id", MaxAdFormat.INTERSTITIAL) }
        verify { mockAdKeeper.registerEcpm(2.0) }
        verify { anyConstructed<InterstitialAdInstance>().load(mockActivity) }
        verify { anyConstructed<InterstitialAdInstance>().addExtra("previous_auction_price", null) }
    }

    @Test
    fun `loadInterstitialAd with should_load true and last ecpm should pass it as extra`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns false
            every { getBoolean("should_load", false) } returns true
            every { getAsDouble("ecpm") } returns 2.0
            every { getString("auction_key", null) } returns "test_auction_key"
        }
        every { mockAdKeeper.lastRegisteredEcpm() } returns 1.5

        // When
        bidonInterstitial.loadInterstitialAd(mockParameters, mockActivity, mockListener)

        // Then
        verify { anyConstructed<InterstitialAdInstance>().addExtra("previous_auction_price", 1.5) }
        verify { anyConstructed<InterstitialAdInstance>().load(mockActivity) }
    }

    @Test
    fun `loadInterstitialAd with should_load false should consume ad from keeper`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns false
            every { getBoolean("should_load", false) } returns false
            every { getAsDouble("ecpm") } returns 1.0
        }

        // When
        bidonInterstitial.loadInterstitialAd(mockParameters, mockActivity, mockListener)

        // Then
        verify { mockAdKeeper.consumeAd(1.0) }
        verify { mockListener.onInterstitialAdLoaded(any()) }
    }

    @Test
    fun `loadInterstitialAd with should_load false and no available ad should fail`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns false
            every { getBoolean("should_load", false) } returns false
            every { getAsDouble("ecpm") } returns 1.0
        }
        every { mockAdKeeper.consumeAd(any()) } returns null

        // When
        bidonInterstitial.loadInterstitialAd(mockParameters, mockActivity, mockListener)

        // Then
        verify { mockListener.onInterstitialAdLoadFailed(MaxAdapterError.NO_FILL) }
    }
}
