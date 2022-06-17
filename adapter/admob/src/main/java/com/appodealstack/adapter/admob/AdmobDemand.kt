package com.appodealstack.adapter.admob

import android.content.Context
import android.os.Bundle
import android.view.View
import com.appodealstack.mads.base.AdType
import com.appodealstack.mads.base.AdUnit
import com.appodealstack.mads.demands.*
import com.appodealstack.mads.demands.AdObject.AdView.*
import com.appodealstack.mads.demands.AdObject.Fullscreen.*

class AdmobDemand : Demand, BannerProvider, InterstitialProvider {
    override val demandId = DemandId("admob")

    override val adTypes: Set<AdType> = setOf(AdType.Banner, AdType.Interstitial)

    override fun getAdUnit(adType: AdType): AdUnit {
        TODO("Not yet implemented")
    }

    override suspend fun init(context: Context, configParams: Bundle) {
        TODO("Not yet implemented")
    }

    override fun createInterstitial(): InterstitialAdObject {
        TODO("Not yet implemented")
    }

    override fun createBanner(): BannerAdObject {
        return object : BannerAdObject {
            override val listener: BannerAdObject.BannerAdObjectListener get() = TODO("Not yet implemented")
            override val isLoaded: Boolean get() = TODO("Not yet implemented")
            override val canShow: Boolean get() = TODO("Not yet implemented")
            override val isDestroyed: Boolean get() = TODO("Not yet implemented")

            override fun load(context: Context, listener: AdObjectListener) {
                TODO("Not yet implemented")
            }

            override fun getView(): View? {
                TODO("Not yet implemented")
            }
        }
    }
}