package com.applovin.mediation.adapters.rewarded

import android.app.Activity
import com.applovin.mediation.adapter.MaxAdapterError
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener
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

class BidonRewardedTest {

    private lateinit var bidonRewarded: BidonRewarded
    private lateinit var mockActivity: Activity
    private lateinit var mockListener: MaxRewardedAdapterListener
    private lateinit var mockParameters: MaxAdapterResponseParameters
    private lateinit var mockAdKeeper: AdKeeper<RewardedAdInstance>

    @Before
    fun setup() {
        mockActivity = mockk()
        mockListener = mockk(relaxed = true)
        mockParameters = mockk(relaxed = true)
        mockAdKeeper = mockk()

        mockkObject(AdKeepers)

        every { AdKeepers.rewarded } returns mockAdKeeper
        every { mockAdKeeper.lastRegisteredEcpm() } returns null
        every { mockAdKeeper.registerEcpm(any()) } just Runs
        every { mockAdKeeper.keepAd(any()) } returns null

        // Mock consumed ad instance
        val mockConsumedAd = mockk<RewardedAdInstance>()
        every { mockConsumedAd.setListener(any()) } just Runs
        every { mockAdKeeper.consumeAd(any()) } returns mockConsumedAd

        // Mock listener methods
        every { mockListener.onRewardedAdLoaded() } just Runs
        every { mockListener.onRewardedAdLoadFailed(any()) } just Runs
        every { mockListener.onRewardedAdDisplayFailed(any()) } just Runs

        // Mock RewardedAdInstance construction
        mockkConstructor(RewardedAdInstance::class)
        every { anyConstructed<RewardedAdInstance>().load(any()) } just Runs

        bidonRewarded = BidonRewarded()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `loadRewardedAd with null activity should fail with missing activity error`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn") } returns true
            every { getAsDouble("ecpm") } returns 1.0
        }

        // When
        bidonRewarded.loadRewardedAd(mockParameters, null, mockListener)

        // Then
        verify { mockListener.onRewardedAdLoadFailed(MaxAdapterError.MISSING_ACTIVITY) }
    }

    @Test
    fun `loadRewardedAd with unicorn true and no last ecpm should register ecpm and load ad`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn") } returns true
            every { getAsDouble("ecpm") } returns 2.0
            every { getString("auction_key", null) } returns "auction123"
        }
        every { mockAdKeeper.lastRegisteredEcpm() } returns null

        // When
        bidonRewarded.loadRewardedAd(mockParameters, mockActivity, mockListener)

        // Then
        verify { mockAdKeeper.registerEcpm(2.0) }
        verify { anyConstructed<RewardedAdInstance>().addExtra("previous_auction_price", null) }
        verify { anyConstructed<RewardedAdInstance>().load(mockActivity) }
    }

    @Test
    fun `loadRewardedAd with unicorn true and last ecpm should pass previous ecpm as extra`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn") } returns true
            every { getAsDouble("ecpm") } returns 3.0
            every { getString("auction_key", null) } returns "auction456"
        }
        every { mockAdKeeper.lastRegisteredEcpm() } returns 1.5

        // When
        bidonRewarded.loadRewardedAd(mockParameters, mockActivity, mockListener)

        // Then
        verify { mockAdKeeper.registerEcpm(3.0) }
        verify { anyConstructed<RewardedAdInstance>().addExtra("previous_auction_price", 1.5) }
        verify { anyConstructed<RewardedAdInstance>().load(mockActivity) }
    }

    @Test
    fun `loadRewardedAd with unicorn false should consume ad from keeper`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn") } returns false
            every { getAsDouble("ecpm") } returns 1.0
        }

        // When
        bidonRewarded.loadRewardedAd(mockParameters, mockActivity, mockListener)

        verify { mockAdKeeper.consumeAd(1.0) }
        verify { mockListener.onRewardedAdLoaded() }
    }

    @Test
    fun `loadRewardedAd with unicorn false and no ad available should fail`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn") } returns false
            every { getAsDouble("ecpm") } returns 1.0
        }
        every { mockAdKeeper.consumeAd(any()) } returns null

        // When
        bidonRewarded.loadRewardedAd(mockParameters, mockActivity, mockListener)

        // Then
        verify { mockListener.onRewardedAdLoadFailed(MaxAdapterError.NO_FILL) }
    }

    @Test
    fun `showRewardedAd with not ready ad should fail`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn") } returns false
            every { getAsDouble("ecpm") } returns 1.0
        }

        val mockInstance = mockk<RewardedAdInstance>()
        every { mockInstance.isReady } returns false
        every { mockAdKeeper.consumeAd(any()) } returns mockInstance

        // When
        bidonRewarded.showRewardedAd(mockParameters, mockActivity, mockListener)

        // Then
        verify { mockListener.onRewardedAdDisplayFailed(MaxAdapterError.AD_DISPLAY_FAILED) }
    }
}
