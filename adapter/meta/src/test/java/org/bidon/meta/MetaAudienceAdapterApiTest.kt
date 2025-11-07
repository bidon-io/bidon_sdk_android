package org.bidon.meta

import com.google.common.truth.Truth.assertThat
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.Initializable
import org.junit.Test
import kotlin.test.assertNotNull

class MetaAudienceAdapterApiTest {

    private val adapterClass = MetaAudienceAdapter::class.java

    @Test
    fun `adapter implements Adapter_Bidding interface`() {
        assertThat(Adapter.Bidding::class.java.isAssignableFrom(adapterClass)).isTrue()
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
    fun `Adapter has required properties`() {
        val hasDemandIdField = try {
            adapterClass.getDeclaredField("demandId")
            true
        } catch (e: NoSuchFieldException) {
            false
        }

        val hasDemandIdGetter = try {
            adapterClass.getMethod("getDemandId")
            true
        } catch (e: NoSuchMethodException) {
            false
        }

        assertThat(hasDemandIdField || hasDemandIdGetter).isTrue()

        val hasAdapterInfoField = try {
            adapterClass.getDeclaredField("adapterInfo")
            true
        } catch (e: NoSuchFieldException) {
            false
        }

        val hasAdapterInfoGetter = try {
            adapterClass.getMethod("getAdapterInfo")
            true
        } catch (e: NoSuchMethodException) {
            false
        }

        assertThat(hasAdapterInfoField || hasAdapterInfoGetter).isTrue()
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
    fun `Adapter_Bidding has getToken method`() {
        val getTokenMethod = adapterClass.getDeclaredMethods().find { it.name == "getToken" }
        assertNotNull(getTokenMethod)
    }
}
