package org.bidon.taurusx

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.taurusx.tax.api.TaurusXAds
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.bidon.sdk.regulation.Coppa
import org.bidon.sdk.regulation.Regulation
import org.bidon.taurusx.impl.TaurusXBannerImpl
import org.bidon.taurusx.impl.TaurusXInterstitialImpl
import org.bidon.taurusx.impl.TaurusXRewardedImpl
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TaurusXAdapterTest {

    private val context = mockk<Context>(relaxed = true)
    private val testee = TaurusXAdapter()

    @Before
    fun setUp() {
        mockkStatic(TaurusXAds::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `demandId should return correct value`() {
        assertThat(testee.demandId).isEqualTo(TaurusXDemandId)
    }

    @Test
    fun `parseConfigParam should parse app_id correctly`() {
        val json = """{"app_id": "test_app_id_123", "channel": "test_channel"}"""

        val result = testee.parseConfigParam(json)

        assertThat(result.appId).isEqualTo("test_app_id_123")
        assertThat(result.channel).isEqualTo("test_channel")
        assertThat(result.placementIds).isEmpty()
    }

    @Test
    fun `parseConfigParam should parse placement_ids correctly`() {
        val json = """
        {
            "app_id": "test_app_id",
            "channel": "test_channel",
            "placement_ids": [
                {"placement_id": "banner_123", "format": "banner"},
                {"placement_id": "interstitial_456", "format": "interstitial"}
            ]
        }
        """.trimIndent()

        val result = testee.parseConfigParam(json)

        assertThat(result.appId).isEqualTo("test_app_id")
        assertThat(result.channel).isEqualTo("test_channel")
        assertThat(result.placementIds).hasSize(2)
        assertThat(result.placementIds[0].adUnitId).isEqualTo("banner_123")
        assertThat(result.placementIds[0].adFormat).isEqualTo("banner")
        assertThat(result.placementIds[1].adUnitId).isEqualTo("interstitial_456")
        assertThat(result.placementIds[1].adFormat).isEqualTo("interstitial")
    }

    @Test
    fun `parseConfigParam should handle missing parameters`() {
        val json = """{"other_param": "value"}"""

        val result = testee.parseConfigParam(json)

        assertThat(result.appId).isEmpty()
        assertThat(result.channel).isEmpty()
        assertThat(result.placementIds).isEmpty()
    }

    @Test
    fun `init should succeed when SDK is already initialized`() = runTest {
        every { TaurusXAds.isInitialized() } returns true
        val params = TaurusXParams("test_app_id", "test_channel", emptyList())

        testee.init(context, params)

        verify(exactly = 0) { TaurusXAds.init(any(), any()) }
    }

    @Test
    fun `init should fail when app_id is blank`() = runTest {
        every { TaurusXAds.isInitialized() } returns false
        val params = TaurusXParams("", "test_channel", emptyList())

        val exception = assertFailsWith<IllegalArgumentException> {
            testee.init(context, params)
        }

        assertTrue(exception.message!!.contains("appId is empty or blank"))
    }

    @Test
    fun `init should fail when channel is blank`() = runTest {
        every { TaurusXAds.isInitialized() } returns false
        val params = TaurusXParams("test_app_id", "", emptyList())

        val exception = assertFailsWith<IllegalArgumentException> {
            testee.init(context, params)
        }

        assertTrue(exception.message!!.contains("channel is empty or blank"))
    }

    @Test
    fun `init should initialize SDK when not initialized`() = runTest {
        every { TaurusXAds.isInitialized() } returns false
        every { TaurusXAds.setChannel(any()) } returns Unit
        every { TaurusXAds.init(any(), any()) } returns Unit
        val placementIds = listOf(
            TaurusXPlacement("banner_123", "banner"),
            TaurusXPlacement("interstitial_456", "interstitial")
        )
        val params = TaurusXParams("test_app_id", "test_channel", placementIds)

        testee.init(context, params)

        verify { TaurusXAds.setChannel("test_channel") }
        verify { TaurusXAds.init(context, "test_app_id") }
    }

    @Test
    fun `updateRegulation should set GDPR privacy when applicable`() {
        every { TaurusXAds.setGDPRDataCollection(any()) } returns Unit
        val regulation = mockk<Regulation> {
            every { gdprApplies } returns true
            every { hasGdprConsent } returns true
            every { ccpaApplies } returns false
            every { coppa } returns Coppa.Default
        }

        testee.updateRegulation(regulation)

        verify { TaurusXAds.setGDPRDataCollection(0) }
    }

    @Test
    fun `updateRegulation should set GDPR privacy when no consent`() {
        every { TaurusXAds.setGDPRDataCollection(any()) } returns Unit
        val regulation = mockk<Regulation> {
            every { gdprApplies } returns true
            every { hasGdprConsent } returns false
            every { ccpaApplies } returns false
            every { coppa } returns Coppa.Default
        }

        testee.updateRegulation(regulation)

        verify { TaurusXAds.setGDPRDataCollection(1) }
    }

    @Test
    fun `updateRegulation should set CCPA privacy when applicable`() {
        every { TaurusXAds.setCCPADoNotSell(any()) } returns Unit
        val regulation = mockk<Regulation> {
            every { gdprApplies } returns false
            every { ccpaApplies } returns true
            every { hasCcpaConsent } returns true
            every { coppa } returns Coppa.Default
        }

        testee.updateRegulation(regulation)

        verify { TaurusXAds.setCCPADoNotSell(0) }
    }

    @Test
    fun `updateRegulation should set CCPA privacy when no consent`() {
        every { TaurusXAds.setCCPADoNotSell(any()) } returns Unit
        val regulation = mockk<Regulation> {
            every { gdprApplies } returns false
            every { ccpaApplies } returns true
            every { hasCcpaConsent } returns false
            every { coppa } returns Coppa.Default
        }

        testee.updateRegulation(regulation)

        verify { TaurusXAds.setCCPADoNotSell(1) }
    }

    @Test
    fun `updateRegulation should set COPPA when applicable`() {
        every { TaurusXAds.setCOPPAIsAgeRestrictedUser(any()) } returns Unit
        val regulation = mockk<Regulation> {
            every { gdprApplies } returns false
            every { ccpaApplies } returns false
            every { coppa } returns Coppa.Yes
            every { coppaApplies } returns true
        }

        testee.updateRegulation(regulation)

        verify { TaurusXAds.setCOPPAIsAgeRestrictedUser(1) }
    }

    @Test
    fun `updateRegulation should set COPPA when not applicable`() {
        every { TaurusXAds.setCOPPAIsAgeRestrictedUser(any()) } returns Unit
        val regulation = mockk<Regulation> {
            every { gdprApplies } returns false
            every { ccpaApplies } returns false
            every { coppa } returns Coppa.No
            every { coppaApplies } returns false
        }

        testee.updateRegulation(regulation)

        verify { TaurusXAds.setCOPPAIsAgeRestrictedUser(0) }
    }

    @Test
    fun `banner should return TaurusXBannerImpl instance`() {
        val banner = testee.banner()
        assertThat(banner).isInstanceOf(TaurusXBannerImpl::class.java)
    }

    @Test
    fun `interstitial should return TaurusXInterstitialImpl instance`() {
        val interstitial = testee.interstitial()
        assertThat(interstitial).isInstanceOf(TaurusXInterstitialImpl::class.java)
    }

    @Test
    fun `rewarded should return TaurusXRewardedImpl instance`() {
        val rewarded = testee.rewarded()
        assertThat(rewarded).isInstanceOf(TaurusXRewardedImpl::class.java)
    }
}
