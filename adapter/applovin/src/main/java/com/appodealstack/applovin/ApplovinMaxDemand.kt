package com.appodealstack.applovin

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.sdk.AppLovinSdk
import com.appodealstack.applovin.ext.wrapToMaxAdListener
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.base.AdType
import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.Demand
import com.appodealstack.mads.demands.DemandId

class ApplovinMaxDemand : Demand {
    private val interstitials = mutableMapOf<String, MaxInterstitialAd>()

    override val demandId: DemandId = DemandId("Applovin Max")

    override suspend fun init(context: Context, configParams: Bundle) {
        require(AppLovinSdk.getInstance(context).isInitialized) {
            "Finish ApplovinSdk's initialization before BidOnInitializer.build()"
        }
    }

    override fun loadAd(activity: Activity, adType: AdType, adParams: Bundle) {
        val adUnitId = adParams.getString(adUnitIdKey) ?: ""
        when (adType) {
            AdType.Banner -> TODO()
            AdType.Interstitial -> {
                val maxInterstitialAd = interstitials[adUnitId]
                    ?: MaxInterstitialAd(adUnitId, activity).apply {
                        this.setListener(getCoreListener(adType).wrapToMaxAdListener())
                    }.also {
                        interstitials[adUnitId] = it
                    }
                maxInterstitialAd.loadAd()
            }
            AdType.Rewarded -> TODO()
            AdType.Native -> TODO()
        }
    }

    override fun destroyAd(adType: AdType, bundle: Bundle): Boolean {
        val adUnitId = bundle.getString(adUnitIdKey) ?: return false
        return when (adType) {
            AdType.Banner -> TODO()
            AdType.Interstitial -> {
                interstitials.remove(adUnitId)?.destroy() != null
            }
            AdType.Rewarded -> TODO()
            AdType.Native -> TODO()
        }
    }

    override fun showAd(adType: AdType, bundle: Bundle): Boolean {
        val adUnitId = bundle.getString(adUnitIdKey) ?: ""
        return when (adType) {
            AdType.Banner -> TODO()
            AdType.Interstitial -> {
                interstitials[adUnitId]?.let { interstitialAd ->
                    val placement = bundle.getString(placementKey)
                    val customData = bundle.getString(customDataKey)
                    interstitialAd.showAd(placement, customData)
                } != null
            }
            AdType.Rewarded -> TODO()
            AdType.Native -> TODO()
        }
    }

    override fun canShow(adType: AdType, adParams: Bundle): Boolean {
        val adUnitId = adParams.getString(adUnitIdKey) ?: ""
        return when (adType) {
            AdType.Banner -> TODO()
            AdType.Interstitial -> {
                interstitials[adUnitId]?.isReady ?: false
            }
            AdType.Rewarded -> TODO()
            AdType.Native -> TODO()
        }
    }

    override fun setExtras(bundle: Bundle) {
        val adUnitId = bundle.getString(adUnitIdKey) ?: return
        val key = bundle.getString(keyKey) ?: return
        val value = bundle.getString(valueKey)
        interstitials[adUnitId]?.setExtraParameter(key, value)
    }

    private fun getCoreListener(adType: AdType): AdListener {
        return SdkCore.getListenerForDemand(adType)
    }
}

internal const val adUnitIdKey = "adUnitId"
internal const val placementKey = "placement"
internal const val customDataKey = "customData"
internal const val keyKey = "key"
internal const val valueKey = "valueKey"