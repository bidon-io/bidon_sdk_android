package org.bidon.moloco

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.privacy.MolocoPrivacy
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.bidon.moloco.impl.MolocoBannerImpl
import org.bidon.moloco.impl.MolocoInterstitialImpl
import org.bidon.moloco.impl.MolocoRewardedImpl
import org.bidon.sdk.regulation.Regulation
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for MolocoAdapter
 */
class MolocoAdapterTest {

    private val context = mockk<Context>(relaxed = true)
    private val testee = MolocoAdapter()

    @Before
    fun setUp() {
        mockkStatic(Moloco::class)
        mockkStatic(MolocoPrivacy::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `demandId should return correct value`() {
        assertThat(testee.demandId).isEqualTo(MolocoDemandId)
    }

    @Test
    fun `adapterInfo should contain correct versions`() {
        val adapterInfo = testee.adapterInfo
        assertThat(adapterInfo.adapterVersion).isNotEmpty()
        assertThat(adapterInfo.sdkVersion).isNotEmpty()
    }

    @Test
    fun `parseConfigParam should parse app_key correctly`() {
        val json = """{"app_key": "test_app_key_123"}"""

        val result = testee.parseConfigParam(json)

        assertThat(result.appKey).isEqualTo("test_app_key_123")
    }

    @Test
    fun `parseConfigParam should handle missing app_key`() {
        val json = """{"other_param": "value"}"""

        val result = testee.parseConfigParam(json)

        assertThat(result.appKey).isEmpty()
    }

    @Test
    fun `init should succeed when SDK is already initialized`() = runTest {
        every { Moloco.isInitialized } returns true
        val params = MolocoParams("test_app_key")

        testee.init(context, params)

        verify(exactly = 0) { Moloco.initialize(any(), any()) }
    }

    @Test
    fun `init should fail when app key is blank`() = runTest {
        every { Moloco.isInitialized } returns false
        val params = MolocoParams("")

        val exception = assertFailsWith<IllegalArgumentException> {
            testee.init(context, params)
        }

        assertTrue(exception.message!!.contains("app key is empty or blank"))
    }

    @Test
    fun `updateRegulation should set GDPR privacy when applicable`() {
        every { MolocoPrivacy.setPrivacy(any()) } returns Unit
        val regulation = mockk<Regulation> {
            every { gdprApplies } returns true
            every { hasGdprConsent } returns true
            every { ccpaApplies } returns false
            every { coppaApplies } returns false
        }

        testee.updateRegulation(regulation)

        verify {
            MolocoPrivacy.setPrivacy(any())
        }
    }

    @Test
    fun `updateRegulation should set CCPA privacy when applicable`() {
        every { MolocoPrivacy.setPrivacy(any()) } returns Unit
        val regulation = mockk<Regulation> {
            every { gdprApplies } returns false
            every { ccpaApplies } returns true
            every { hasCcpaConsent } returns false
            every { coppaApplies } returns false
        }

        testee.updateRegulation(regulation)

        verify {
            MolocoPrivacy.setPrivacy(any())
        }
    }

    @Test
    fun `updateRegulation should set COPPA privacy`() {
        every { MolocoPrivacy.setPrivacy(any()) } returns Unit
        val regulation = mockk<Regulation> {
            every { gdprApplies } returns false
            every { ccpaApplies } returns false
            every { coppaApplies } returns true
        }

        testee.updateRegulation(regulation)

        verify {
            MolocoPrivacy.setPrivacy(any())
        }
    }

    @Test
    fun `banner should return MolocoBannerImpl instance`() {
        val banner = testee.banner()
        assertThat(banner).isInstanceOf(MolocoBannerImpl::class.java)
    }

    @Test
    fun `interstitial should return MolocoInterstitialImpl instance`() {
        val interstitial = testee.interstitial()
        assertThat(interstitial).isInstanceOf(MolocoInterstitialImpl::class.java)
    }

    @Test
    fun `rewarded should return MolocoRewardedImpl instance`() {
        val rewarded = testee.rewarded()
        assertThat(rewarded).isInstanceOf(MolocoRewardedImpl::class.java)
    }
}
