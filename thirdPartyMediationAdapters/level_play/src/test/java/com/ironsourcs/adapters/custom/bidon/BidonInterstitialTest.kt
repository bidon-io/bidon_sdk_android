package com.ironsourcs.adapters.custom.bidon

import android.app.Activity
import com.ironsource.adapters.custom.bidon.BidonCustomInterstitial
import com.ironsource.adapters.custom.bidon.ext.AD_IS_NULL_ERROR
import com.ironsource.adapters.custom.bidon.ext.NO_FILL_ERROR
import com.ironsource.adapters.custom.bidon.interstitial.InterstitialAdInstance
import com.ironsource.adapters.custom.bidon.keeper.AdKeeper
import com.ironsource.adapters.custom.bidon.keeper.AdKeepers
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class BidonInterstitialTest {

    private lateinit var bidonInterstitial: BidonCustomInterstitial

    private lateinit var mockActivity: Activity
    private lateinit var mockListener: InterstitialAdListener
    private lateinit var mockParameters: AdData
    private lateinit var mockAdKeeper: AdKeeper<InterstitialAdInstance>

    @Before
    fun setup() {
        mockActivity = mockk()
        mockListener = mockk(relaxed = true)
        mockParameters = mockk(relaxed = true)
        mockAdKeeper = mockk()

        mockkObject(AdKeepers)

        every { AdKeepers.getInterstitialKeeper("testAdUnitId") } returns mockAdKeeper
        every { mockAdKeeper.lastRegisteredEcpm() } returns null
        every { mockAdKeeper.registerEcpm(any()) } just Runs
        every { mockAdKeeper.keepAd(any()) } returns null

        // Mock consumed ad instance
        val mockConsumedAd = mockk<InterstitialAdInstance>()
        every { mockConsumedAd.setListener(any()) } just Runs
        every { mockConsumedAd.ecpm } returns 1.0
        every { mockConsumedAd.demandId } returns "test-demand-id"
        every { mockConsumedAd.isReady } returns true
        every { mockConsumedAd.notifyWin() } just Runs
        every { mockAdKeeper.consumeAd(any()) } returns mockConsumedAd

        // Mock listener methods
        every { mockListener.onAdLoadSuccess() } just Runs
        every { mockListener.onAdLoadFailed(any(), any(), any()) } just Runs
        every { mockListener.onAdShowFailed(any(), any()) } just Runs

        // Mock InterstitialAdInstance creation
        mockkConstructor(InterstitialAdInstance::class)
        every { anyConstructed<InterstitialAdInstance>().load(any()) } just Runs

        bidonInterstitial = BidonCustomInterstitial(mockk())
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `loadInterstitialAd with ext true should load new ad and register ecpm`() {
        // Given
        val mockAdUnitData = mockk<Map<String, Any>>()
        every { mockAdUnitData["adUnitId"] } returns "testAdUnitId"

        val mockConfig = mockk<Map<String, Any>>()
        every { mockConfig["should_load"] } returns "true"
        every { mockConfig["price"] } returns "2.0"
        every { mockConfig["auctionKey"] } returns "test_auctionKey"
        every { mockConfig["instanceName"] } returns "test_instanceName"
        every { mockParameters.configuration } returns mockConfig
        every { mockParameters.adUnitData } returns mockAdUnitData
        every { mockAdKeeper.lastRegisteredEcpm() } returns null

        // When
        bidonInterstitial.loadAd(mockParameters, mockActivity, mockListener)

        // Then
        verify { mockAdKeeper.registerEcpm(2.0) }
        verify { anyConstructed<InterstitialAdInstance>().load(mockActivity) }
        verify { anyConstructed<InterstitialAdInstance>().addExtra("previous_auction_price", null) }
    }

    @Test
    fun `loadInterstitialAd with ext true and last ecpm should pass it as extra`() {
        // Given
        val mockAdUnitData = mockk<Map<String, Any>>()
        every { mockAdUnitData["adUnitId"] } returns "testAdUnitId"

        val mockConfig = mockk<Map<String, Any>>()
        every { mockConfig["should_load"] } returns "true"
        every { mockConfig["price"] } returns "2.0"
        every { mockConfig["auctionKey"] } returns "test_auctionKey"
        every { mockConfig["instanceName"] } returns "test_instanceName"
        every { mockParameters.configuration } returns mockConfig
        every { mockParameters.adUnitData } returns mockAdUnitData
        every { mockAdKeeper.lastRegisteredEcpm() } returns 1.5

        // When
        bidonInterstitial.loadAd(mockParameters, mockActivity, mockListener)

        // Then
        verify { anyConstructed<InterstitialAdInstance>().addExtra("previous_auction_price", 1.5) }
        verify { anyConstructed<InterstitialAdInstance>().load(mockActivity) }
    }

    @Test
    fun `loadInterstitialAd with ext false should consume ad from keeper`() {
        // Given
        val mockAdUnitData = mockk<Map<String, Any>>()
        every { mockAdUnitData["adUnitId"] } returns "testAdUnitId"

        val mockConfig = mockk<Map<String, Any>>()
        every { mockConfig["should_load"] } returns "false"
        every { mockConfig["price"] } returns "1.0"
        every { mockConfig["instanceName"] } returns "test_instanceName"
        every { mockParameters.configuration } returns mockConfig
        every { mockParameters.adUnitData } returns mockAdUnitData

        // When
        bidonInterstitial.loadAd(mockParameters, mockActivity, mockListener)

        // Then
        verify { mockAdKeeper.consumeAd(1.0) }
        verify { mockListener.onAdLoadSuccess() }
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `loadInterstitialAd with ext false and no available ad should fail`() = runTest {
        // Given
        val mockAdUnitData = mockk<Map<String, Any>>()
        every { mockAdUnitData["adUnitId"] } returns "testAdUnitId"

        val mockConfig = mockk<Map<String, Any>>()
        every { mockConfig["should_load"] } returns "false"
        every { mockConfig["price"] } returns "1.0"
        every { mockConfig["instanceName"] } returns "test_instanceName"
        every { mockParameters.configuration } returns mockConfig
        every { mockParameters.adUnitData } returns mockAdUnitData
        every { mockAdKeeper.consumeAd(any()) } returns null

        // When
        bidonInterstitial.loadAd(mockParameters, mockActivity, mockListener)

        // Then - runTest automatically waits for all coroutines to complete
        verify {
            mockListener.onAdLoadFailed(AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL, NO_FILL_ERROR, any())
        }
    }

    @Test
    fun `showInterstitialAd with not ready ad should fail`() {
        // Given

        // When
        bidonInterstitial.showAd(mockParameters, mockListener)

        // Then
        verify { mockListener.onAdShowFailed(AD_IS_NULL_ERROR, any()) }
    }
}
