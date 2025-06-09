package com.applovin.mediation.adapters.keeper

import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.adapters.banner.BannerAdInstance
import com.applovin.mediation.adapters.interstitial.InterstitialAdInstance
import com.applovin.mediation.adapters.rewarded.RewardedAdInstance
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdKeepersTest {

    private val adUnitId = "test_ad_unit_id"

    @Before
    fun setup() {
        // Clear keepers before each test
        AdKeepers.keepers.clear()
    }

    @Test
    fun `getKeeper returns same instance for same ad unit id and format`() {
        val keeper1 = AdKeepers.getKeeper<BannerAdInstance>(adUnitId, MaxAdFormat.BANNER)
        val keeper2 = AdKeepers.getKeeper<BannerAdInstance>(adUnitId, MaxAdFormat.BANNER)

        assertSame(keeper1, keeper2)
    }

    @Test
    fun `getKeeper returns different instances for different ad unit ids`() {
        val keeper1 = AdKeepers.getKeeper<BannerAdInstance>("ad_unit_1", MaxAdFormat.BANNER)
        val keeper2 = AdKeepers.getKeeper<BannerAdInstance>("ad_unit_2", MaxAdFormat.BANNER)

        assertNotNull(keeper1)
        assertNotNull(keeper2)
        assert(keeper1 !== keeper2)
    }

    @Test
    fun `getKeeper creates correct keeper type for each format`() {
        // Given
        val bannerKeeper = AdKeepers.getKeeper<BannerAdInstance>("banner_$adUnitId", MaxAdFormat.BANNER)
        val mrecKeeper = AdKeepers.getKeeper<BannerAdInstance>("mrec_$adUnitId", MaxAdFormat.MREC)
        val leaderKeeper = AdKeepers.getKeeper<BannerAdInstance>("leader_$adUnitId", MaxAdFormat.LEADER)
        val interstitialKeeper = AdKeepers.getKeeper<InterstitialAdInstance>("interstitial_$adUnitId", MaxAdFormat.INTERSTITIAL)
        val rewardedKeeper = AdKeepers.getKeeper<RewardedAdInstance>("rewarded_$adUnitId", MaxAdFormat.REWARDED)

        // Then
        assertNotNull(bannerKeeper)
        assertNotNull(mrecKeeper)
        assertNotNull(leaderKeeper)
        assertNotNull(interstitialKeeper)
        assertNotNull(rewardedKeeper)

        // Verify keeper types
        assertTrue(bannerKeeper is AdKeeperImpl<BannerAdInstance>)
        assertTrue(mrecKeeper is AdKeeperImpl<BannerAdInstance>)
        assertTrue(leaderKeeper is AdKeeperImpl<BannerAdInstance>)
        assertTrue(interstitialKeeper is AdKeeperImpl<InterstitialAdInstance>)
        assertTrue(rewardedKeeper is AdKeeperImpl<RewardedAdInstance>)

        // Verify different instances for different formats
        assertNotSame(bannerKeeper, mrecKeeper)
        assertNotSame(mrecKeeper, leaderKeeper)
        assertNotSame(leaderKeeper, interstitialKeeper)
        assertNotSame(interstitialKeeper, rewardedKeeper)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getKeeper throws exception for unsupported format`() {
        AdKeepers.getKeeper<BannerAdInstance>(adUnitId, MaxAdFormat.NATIVE)
    }
}