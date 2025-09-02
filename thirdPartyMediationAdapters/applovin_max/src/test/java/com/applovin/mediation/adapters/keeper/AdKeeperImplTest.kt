package com.applovin.mediation.adapters.keeper

import com.applovin.mediation.adapters.logger.AppLovinSdkLogger
import com.applovin.mediation.adapters.logger.Logger
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.bidon.sdk.ads.Ad
import org.junit.Before
import org.junit.Test

class AdKeeperImplTest {
    private lateinit var adKeeper: AdKeeperImpl<TestAdInstance>
    private lateinit var mockLogger: Logger

    @Before
    fun setup() {
        mockLogger = mockk()
        mockkObject(AppLovinSdkLogger)
        every { AppLovinSdkLogger.log(any(), any()) } just Runs
        adKeeper = AdKeeperImpl("Test")
    }

    @Test
    fun `registerEcpm should add ecpm to registered set and update lastRegisteredEcpm`() {
        // When
        adKeeper.registerEcpm(1.0)
        adKeeper.registerEcpm(2.0)
        adKeeper.registerEcpm(3.0)

        // Then
        assertThat(adKeeper.lastRegisteredEcpm()).isEqualTo(3.0)
    }

    @Test
    fun `keepAd with no current ad should keep new ad`() {
        // Given
        val newAd = TestAdInstance(2.0)

        // When
        val rejectedAd = adKeeper.keepAd(newAd)

        // Then
        assertThat(rejectedAd).isNull()
    }

    @Test
    fun `keepAd with higher ecpm should replace current ad`() {
        // Given
        val currentAd = TestAdInstance(1.0)
        val newAd = TestAdInstance(2.0)
        adKeeper.keepAd(currentAd)

        // When
        val rejectedAd = adKeeper.keepAd(newAd)

        // Then
        assertThat(rejectedAd).isEqualTo(currentAd)
    }

    @Test
    fun `keepAd with lower ecpm should reject new ad`() {
        // Given
        val currentAd = TestAdInstance(2.0)
        val newAd = TestAdInstance(1.0)
        adKeeper.keepAd(currentAd)

        // When
        val rejectedAd = adKeeper.keepAd(newAd)

        // Then
        assertThat(rejectedAd).isEqualTo(newAd)
    }

    @Test
    fun `consumeAd with no ad available should return null`() {
        // When
        val result = adKeeper.consumeAd(1.0)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `consumeAd with ad in range should return ad, clear it and call notifyWin`() {
        // Given
        val ad = mockk<TestAdInstance>()
        every { ad.ecpm } returns 2.0
        every { ad.demandId } returns "default-demand-id"
        every { ad.isReady } returns true
        every { ad.uid } returns "test-uid"
        every { ad.bidType } returns "test-bid-type"
        every { ad.notifyWin() } just Runs

        adKeeper.keepAd(ad)
        adKeeper.registerEcpm(1.0)
        adKeeper.registerEcpm(3.0)

        // When
        val result = adKeeper.consumeAd(2.0)

        // Then
        assertThat(result).isEqualTo(ad)
        verify(exactly = 1) { ad.notifyWin() }
        assertThat(adKeeper.consumeAd(2.0)).isNull() // Should be cleared
    }

    @Test
    fun `consumeAd with ad below range should return null`() {
        // Given
        val ad = TestAdInstance(1.0)
        adKeeper.keepAd(ad)
        adKeeper.registerEcpm(2.0)
        adKeeper.registerEcpm(3.0)

        // When
        val result = adKeeper.consumeAd(2.0)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `consumeAd with ad above range should return null`() {
        // Given
        val ad = TestAdInstance(4.0)
        adKeeper.keepAd(ad)
        adKeeper.registerEcpm(1.0)
        adKeeper.registerEcpm(2.0)

        // When
        val result = adKeeper.consumeAd(2.0)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `consumeAd with single registered ecpm should use it as both bounds and call notifyWin`() {
        // Given
        val ad = mockk<TestAdInstance>()
        every { ad.ecpm } returns 2.0
        every { ad.demandId } returns "default-demand-id"
        every { ad.isReady } returns true
        every { ad.uid } returns "test-uid"
        every { ad.bidType } returns "test-bid-type"
        every { ad.notifyWin() } just Runs

        adKeeper.keepAd(ad)
        adKeeper.registerEcpm(2.0)

        // When
        val result = adKeeper.consumeAd(2.0)

        // Then
        assertThat(result).isEqualTo(ad)
        verify(exactly = 1) { ad.notifyWin() }
    }

    @Test
    fun `consumeAd with no registered ecpm should use requested ecpm as bounds and call notifyWin`() {
        // Given
        val ad = mockk<TestAdInstance>()
        every { ad.ecpm } returns 2.0
        every { ad.demandId } returns "default-demand-id"
        every { ad.isReady } returns true
        every { ad.uid } returns "test-uid"
        every { ad.bidType } returns "test-bid-type"
        every { ad.notifyWin() } just Runs

        adKeeper.keepAd(ad)

        // When
        val result = adKeeper.consumeAd(2.0)

        // Then
        assertThat(result).isEqualTo(ad)
        verify(exactly = 1) { ad.notifyWin() }
    }

    @Test
    fun `consumeAd should not call notifyWin when ad is outside range`() {
        // Given
        val ad = mockk<TestAdInstance>()
        every { ad.ecpm } returns 4.0
        every { ad.demandId } returns "default-demand-id"
        every { ad.isReady } returns true
        every { ad.uid } returns "test-uid"
        every { ad.bidType } returns "test-bid-type"
        every { ad.notifyWin() } just Runs

        adKeeper.keepAd(ad)
        adKeeper.registerEcpm(1.0)
        adKeeper.registerEcpm(2.0)

        // When
        val result = adKeeper.consumeAd(2.0)

        // Then
        assertThat(result).isNull()
        verify(exactly = 0) { ad.notifyWin() }
    }

    @Test
    fun `keepAd should call notifyLoss on replaced ad with correct winner info`() {
        // Given
        val currentAd = mockk<TestAdInstance>()
        every { currentAd.ecpm } returns 1.0
        every { currentAd.demandId } returns "current-demand-id"
        every { currentAd.isReady } returns true
        every { currentAd.uid } returns "current-uid"
        every { currentAd.bidType } returns "current-bid-type"
        every { currentAd.notifyLoss(any(), any()) } just Runs

        val newAd = mockk<TestAdInstance>()
        every { newAd.ecpm } returns 2.0
        every { newAd.demandId } returns "new-demand-id"
        every { newAd.isReady } returns true
        every { newAd.uid } returns "new-uid"
        every { newAd.bidType } returns "new-bid-type"

        adKeeper.keepAd(currentAd)

        // When
        val rejectedAd = adKeeper.keepAd(newAd)

        // Then
        assertThat(rejectedAd).isEqualTo(currentAd)
        verify(exactly = 1) { currentAd.notifyLoss("new-demand-id", 2.0) }
    }

    @Test
    fun `keepAd should not call notifyLoss when new ad has lower ecpm`() {
        // Given
        val currentAd = mockk<TestAdInstance>()
        every { currentAd.ecpm } returns 2.0
        every { currentAd.demandId } returns "current-demand-id"
        every { currentAd.isReady } returns true
        every { currentAd.uid } returns "current-uid"
        every { currentAd.bidType } returns "current-bid-type"
        every { currentAd.notifyLoss(any(), any()) } just Runs

        val newAd = mockk<TestAdInstance>()
        every { newAd.ecpm } returns 1.0
        every { newAd.demandId } returns "new-demand-id"
        every { newAd.isReady } returns true
        every { newAd.uid } returns "new-uid"
        every { newAd.bidType } returns "new-bid-type"
        every { newAd.notifyLoss(any(), any()) } just Runs

        adKeeper.keepAd(currentAd)

        // When
        val rejectedAd = adKeeper.keepAd(newAd)

        // Then
        assertThat(rejectedAd).isEqualTo(newAd)
        verify(exactly = 0) { currentAd.notifyLoss(any(), any()) }
        verify(exactly = 0) { newAd.notifyLoss(any(), any()) }
    }

    @Test
    fun `keepAd should not call notifyLoss when no previous ad exists`() {
        // Given
        val newAd = mockk<TestAdInstance>()
        every { newAd.ecpm } returns 2.0
        every { newAd.demandId } returns "new-demand-id"
        every { newAd.isReady } returns true
        every { newAd.uid } returns "new-uid"
        every { newAd.bidType } returns "new-bid-type"
        every { newAd.notifyLoss(any(), any()) } just Runs

        // When
        val rejectedAd = adKeeper.keepAd(newAd)

        // Then
        assertThat(rejectedAd).isNull()
        verify(exactly = 0) { newAd.notifyLoss(any(), any()) }
    }

    @Test
    fun `multiple keepAd calls should properly notify losses in sequence`() {
        // Given
        val ad1 = mockk<TestAdInstance>()
        every { ad1.ecpm } returns 1.0
        every { ad1.demandId } returns "demand-1"
        every { ad1.isReady } returns true
        every { ad1.uid } returns "uid-1"
        every { ad1.bidType } returns "bid-type-1"
        every { ad1.notifyLoss(any(), any()) } just Runs

        val ad2 = mockk<TestAdInstance>()
        every { ad2.ecpm } returns 2.0
        every { ad2.demandId } returns "demand-2"
        every { ad2.isReady } returns true
        every { ad2.uid } returns "uid-2"
        every { ad2.bidType } returns "bid-type-2"
        every { ad2.notifyLoss(any(), any()) } just Runs

        val ad3 = mockk<TestAdInstance>()
        every { ad3.ecpm } returns 3.0
        every { ad3.demandId } returns "demand-3"
        every { ad3.isReady } returns true
        every { ad3.uid } returns "uid-3"
        every { ad3.bidType } returns "bid-type-3"
        every { ad3.notifyLoss(any(), any()) } just Runs

        // When
        val rejected1 = adKeeper.keepAd(ad1)
        val rejected2 = adKeeper.keepAd(ad2)
        val rejected3 = adKeeper.keepAd(ad3)

        // Then
        assertThat(rejected1).isNull()
        assertThat(rejected2).isEqualTo(ad1)
        assertThat(rejected3).isEqualTo(ad2)

        verify(exactly = 1) { ad1.notifyLoss("demand-2", 2.0) }
        verify(exactly = 1) { ad2.notifyLoss("demand-3", 3.0) }
        verify(exactly = 0) { ad3.notifyLoss(any(), any()) }
    }

    private open class TestAdInstance(
        override val ecpm: Double,
        override val demandId: String = "default-demand-id",
        override val isReady: Boolean = true,
        override val uid: String = "test-uid",
        override val bidType: String = "test-bid-type",
    ) : AdInstance {
        override fun applyAdInfo(ad: Ad): AdInstance = this
        override fun notifyWin() {}
        override fun notifyLoss(winnerDemandId: String, winnerPrice: Double) {}
        override fun destroy() {}
    }
}
