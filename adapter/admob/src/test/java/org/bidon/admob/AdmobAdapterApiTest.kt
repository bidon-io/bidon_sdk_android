package org.bidon.admob

import com.google.common.truth.Truth.assertThat
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.Initializable
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.stats.StatisticsCollector
import org.junit.Test
import kotlin.test.assertNotNull

class AdmobAdapterApiTest {

    private val adapterClass = AdmobAdapter::class.java

    @Test
    fun `adapter implements Adapter_Network interface`() {
        assertThat(Adapter.Network::class.java.isAssignableFrom(adapterClass)).isTrue()
    }

    @Test
    fun `adapter implements Initializable interface`() {
        assertThat(Initializable::class.java.isAssignableFrom(adapterClass)).isTrue()
    }

    @Test
    fun `adapter implements AdProvider_Banner interface`() {
        assertThat(AdProvider.Banner::class.java.isAssignableFrom(adapterClass)).isTrue()
    }

    @Test
    fun `adapter implements AdProvider_Rewarded interface`() {
        assertThat(AdProvider.Rewarded::class.java.isAssignableFrom(adapterClass)).isTrue()
    }

    @Test
    fun `adapter implements AdProvider_Interstitial interface`() {
        assertThat(AdProvider.Interstitial::class.java.isAssignableFrom(adapterClass)).isTrue()
    }

    @Test
    fun `Adapter_Network has required properties`() {
        val demandIdField = adapterClass.getDeclaredField("demandId")
        assertNotNull(demandIdField)

        val adapterInfoField = adapterClass.getDeclaredField("adapterInfo")
        assertNotNull(adapterInfoField)
    }

    @Test
    fun `Initializable has required methods`() {
        val parseConfigParamMethod = adapterClass.getMethod("parseConfigParam", String::class.java)
        assertNotNull(parseConfigParamMethod)
    }

    @Test
    fun `AdProvider_Banner creates valid AdSource_Banner`() {
        val bannerMethod = adapterClass.getMethod("banner")
        assertNotNull(bannerMethod)
        assertThat(AdSource.Banner::class.java.isAssignableFrom(bannerMethod.returnType)).isTrue()
    }

    @Test
    fun `AdProvider_Rewarded creates valid AdSource_Rewarded`() {
        val rewardedMethod = adapterClass.getMethod("rewarded")
        assertNotNull(rewardedMethod)
        assertThat(AdSource.Rewarded::class.java.isAssignableFrom(rewardedMethod.returnType)).isTrue()
    }

    @Test
    fun `AdProvider_Interstitial creates valid AdSource_Interstitial`() {
        val interstitialMethod = adapterClass.getMethod("interstitial")
        assertNotNull(interstitialMethod)
        assertThat(AdSource.Interstitial::class.java.isAssignableFrom(interstitialMethod.returnType)).isTrue()
    }

    @Test
    fun `AdSource_Banner implements required interfaces`() {
        val bannerImplClass = Class.forName("org.bidon.admob.impl.AdmobBannerImpl")
        assertThat(AdEventFlow::class.java.isAssignableFrom(bannerImplClass)).isTrue()
        assertThat(StatisticsCollector::class.java.isAssignableFrom(bannerImplClass)).isTrue()
        assertThat(AdSource.Banner::class.java.isAssignableFrom(bannerImplClass)).isTrue()
    }

    @Test
    fun `AdSource_Rewarded implements required interfaces`() {
        val rewardedImplClass = Class.forName("org.bidon.admob.impl.AdmobRewardedImpl")
        assertThat(AdEventFlow::class.java.isAssignableFrom(rewardedImplClass)).isTrue()
        assertThat(StatisticsCollector::class.java.isAssignableFrom(rewardedImplClass)).isTrue()
        assertThat(AdSource.Rewarded::class.java.isAssignableFrom(rewardedImplClass)).isTrue()
    }

    @Test
    fun `AdSource_Interstitial implements required interfaces`() {
        val interstitialImplClass = Class.forName("org.bidon.admob.impl.AdmobInterstitialImpl")
        assertThat(AdEventFlow::class.java.isAssignableFrom(interstitialImplClass)).isTrue()
        assertThat(StatisticsCollector::class.java.isAssignableFrom(interstitialImplClass)).isTrue()
        assertThat(AdSource.Interstitial::class.java.isAssignableFrom(interstitialImplClass)).isTrue()
    }

    @Test
    fun `AdSource classes have basic structure`() {
        val bannerImplClass = Class.forName("org.bidon.admob.impl.AdmobBannerImpl")
        val rewardedImplClass = Class.forName("org.bidon.admob.impl.AdmobRewardedImpl")
        val interstitialImplClass = Class.forName("org.bidon.admob.impl.AdmobInterstitialImpl")

        assertNotNull(bannerImplClass.constructors)
        assertNotNull(rewardedImplClass.constructors)
        assertNotNull(interstitialImplClass.constructors)

        assertThat(bannerImplClass.constructors.isNotEmpty()).isTrue()
        assertThat(rewardedImplClass.constructors.isNotEmpty()).isTrue()
        assertThat(interstitialImplClass.constructors.isNotEmpty()).isTrue()
    }
}
