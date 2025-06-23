package com.applovin.mediation.adapters.rewarded

import android.app.Activity
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.adapter.MaxAdapterError
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener
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

class BidonRewardedTest {

    private lateinit var bidonRewarded: BidonRewarded

    private lateinit var mockActivity: Activity
    private lateinit var mockListener: MaxRewardedAdapterListener
    private lateinit var mockParameters: MaxAdapterResponseParameters
    private lateinit var mockAdKeeper: AdKeeper<RewardedAdInstance>

    @Before
    fun setup() {
        mockkLog()

        mockActivity = mockk()
        mockListener = mockk(relaxed = true)
        mockParameters = mockk(relaxed = true)
        mockAdKeeper = mockk()

        mockkObject(AdKeepers)

        every { AdKeepers.getKeeper<RewardedAdInstance>(any(), any()) } returns mockAdKeeper
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
        val mockConsumedAd = mockk<RewardedAdInstance>()
        every { mockConsumedAd.setListener(any()) } just Runs
        every { mockAdKeeper.consumeAd(any()) } returns mockConsumedAd

        // Mock listener methods
        every { mockListener.onRewardedAdLoaded() } just Runs
        every { mockListener.onRewardedAdLoadFailed(any()) } just Runs
        every { mockListener.onRewardedAdDisplayFailed(any()) } just Runs

        // Mock RewardedAdInstance creation
        mockkConstructor(RewardedAdInstance::class)
        every { anyConstructed<RewardedAdInstance>().load(any()) } just Runs
        every { anyConstructed<RewardedAdInstance>().addExtra(any(), any()) } just Runs

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
            every { getBoolean("unicorn", false) } returns true
            every { getAsDouble("ecpm") } returns 1.0
        }

        // When
        bidonRewarded.loadRewardedAd(mockParameters, null, mockListener)

        // Then
        verify { mockListener.onRewardedAdLoadFailed(MaxAdapterError.MISSING_ACTIVITY) }
    }

    @Test
    fun `loadRewardedAd with unicorn true should load new ad and register ecpm`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns true
            every { getAsDouble("ecpm") } returns 2.0
            every { getString("auction_key", null) } returns "test_auction_key"
        }
        every { mockAdKeeper.lastRegisteredEcpm() } returns null

        // When
        bidonRewarded.loadRewardedAd(mockParameters, mockActivity, mockListener)

        // Then
        verify { AdKeepers.getKeeper<RewardedAdInstance>("test_ad_unit_id", MaxAdFormat.REWARDED) }
        verify { mockAdKeeper.registerEcpm(2.0) }
        verify { anyConstructed<RewardedAdInstance>().load(mockActivity) }
        verify { anyConstructed<RewardedAdInstance>().addExtra("previous_auction_price", null) }
    }

    @Test
    fun `loadRewardedAd with unicorn true and last ecpm should pass it as extra`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns true
            every { getAsDouble("ecpm") } returns 2.0
            every { getString("auction_key", null) } returns "test_auction_key"
        }
        every { mockAdKeeper.lastRegisteredEcpm() } returns 1.5

        // When
        bidonRewarded.loadRewardedAd(mockParameters, mockActivity, mockListener)

        // Then
        verify { anyConstructed<RewardedAdInstance>().addExtra("previous_auction_price", 1.5) }
        verify { anyConstructed<RewardedAdInstance>().load(mockActivity) }
    }

    @Test
    fun `loadRewardedAd with unicorn false and no available ad should fail`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns false
            every { getBoolean("should_load", false) } returns false
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

        // When
        bidonRewarded.showRewardedAd(mockParameters, mockActivity, mockListener)

        // Then
        verify { mockListener.onRewardedAdDisplayFailed(MaxAdapterError.AD_DISPLAY_FAILED) }
    }

    @Test
    fun `loadRewardedAd with should_load true should load new ad and register ecpm`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns false
            every { getBoolean("should_load", false) } returns true
            every { getAsDouble("ecpm") } returns 2.0
            every { getString("auction_key", null) } returns "test_auction_key"
        }
        every { mockAdKeeper.lastRegisteredEcpm() } returns null

        // When
        bidonRewarded.loadRewardedAd(mockParameters, mockActivity, mockListener)

        // Then
        verify { AdKeepers.getKeeper<RewardedAdInstance>("test_ad_unit_id", MaxAdFormat.REWARDED) }
        verify { mockAdKeeper.registerEcpm(2.0) }
        verify { anyConstructed<RewardedAdInstance>().load(mockActivity) }
        verify { anyConstructed<RewardedAdInstance>().addExtra("previous_auction_price", null) }
    }

    @Test
    fun `loadRewardedAd with should_load true and last ecpm should pass it as extra`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns false
            every { getBoolean("should_load", false) } returns true
            every { getAsDouble("ecpm") } returns 2.0
            every { getString("auction_key", null) } returns "test_auction_key"
        }
        every { mockAdKeeper.lastRegisteredEcpm() } returns 1.5

        // When
        bidonRewarded.loadRewardedAd(mockParameters, mockActivity, mockListener)

        // Then
        verify { anyConstructed<RewardedAdInstance>().addExtra("previous_auction_price", 1.5) }
        verify { anyConstructed<RewardedAdInstance>().load(mockActivity) }
    }

    @Test
    fun `loadRewardedAd with should_load false should consume ad from keeper`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns false
            every { getBoolean("should_load", false) } returns false
            every { getAsDouble("ecpm") } returns 1.0
        }

        // When
        bidonRewarded.loadRewardedAd(mockParameters, mockActivity, mockListener)

        // Then
        verify { mockAdKeeper.consumeAd(1.0) }
        verify { mockListener.onRewardedAdLoaded() }
    }

    @Test
    fun `loadRewardedAd with should_load false and no available ad should fail`() {
        // Given
        every { mockParameters.customParameters } returns mockk {
            every { getBoolean("unicorn", false) } returns false
            every { getBoolean("should_load", false) } returns false
            every { getAsDouble("ecpm") } returns 1.0
        }
        every { mockAdKeeper.consumeAd(any()) } returns null

        // When
        bidonRewarded.loadRewardedAd(mockParameters, mockActivity, mockListener)

        // Then
        verify { mockListener.onRewardedAdLoadFailed(MaxAdapterError.NO_FILL) }
    }
}
