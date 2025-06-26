package com.ironsourcs.adapters.custom.bidon

import com.google.common.truth.Truth.assertThat
import com.ironsource.adapters.custom.bidon.keeper.AdInstance
import com.ironsource.adapters.custom.bidon.keeper.AdKeeperImpl
import com.ironsource.adapters.custom.bidon.keeper.DEFAULT_DEMAND_ID
import com.ironsource.adapters.custom.bidon.logger.LevelPLaySdkLogger
import com.ironsource.adapters.custom.bidon.logger.Logger
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import org.bidon.sdk.ads.Ad
import org.junit.Before
import org.junit.Test

class AdKeeperImplTest {
    private lateinit var adKeeper: AdKeeperImpl<TestAdInstance>
    private lateinit var mockLogger: Logger

    @Before
    fun setup() {
        mockLogger = mockk()
        mockkObject(LevelPLaySdkLogger)
        every { LevelPLaySdkLogger.log(any(), any()) } just Runs
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
    fun `consumeAd with ad in range should return ad and clear it`() {
        // Given
        val ad = TestAdInstance(2.0)
        adKeeper.keepAd(ad)
        adKeeper.registerEcpm(1.0)
        adKeeper.registerEcpm(3.0)

        // When
        val result = adKeeper.consumeAd(2.0)

        // Then
        assertThat(result).isEqualTo(ad)
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
    fun `consumeAd with single registered ecpm should use it as both bounds`() {
        // Given
        val ad = TestAdInstance(2.0)
        adKeeper.keepAd(ad)
        adKeeper.registerEcpm(2.0)

        // When
        val result = adKeeper.consumeAd(2.0)

        // Then
        assertThat(result).isEqualTo(ad)
    }

    @Test
    fun `consumeAd with no registered ecpm should use requested ecpm as bounds`() {
        // Given
        val ad = TestAdInstance(2.0)
        adKeeper.keepAd(ad)

        // When
        val result = adKeeper.consumeAd(2.0)

        // Then
        assertThat(result).isEqualTo(ad)
    }

    private class TestAdInstance(
        override val ecpm: Double,
        override val demandId: String = DEFAULT_DEMAND_ID,
        override val isReady: Boolean = true,
    ) : AdInstance {
        override fun applyAdInfo(ad: Ad): AdInstance = this
        override fun notifyLoss(winnerDemandId: String, winnerPrice: Double) {}
        override fun destroy() {}
    }
}
