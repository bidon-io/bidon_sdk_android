package com.ironsource.adapters.custom.bidon.interstitial

import android.app.Activity
import com.ironsource.adapters.custom.bidon.keeper.AdInstance
import com.ironsource.adapters.custom.bidon.keeper.DEFAULT_DEMAND_ID
import com.ironsource.adapters.custom.bidon.keeper.DEFAULT_ECPM
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.interstitial.InterstitialAd
import org.bidon.sdk.ads.interstitial.InterstitialListener

internal class InterstitialAdInstance(auctionKey: String? = null) : AdInstance {

    private val interstitialAd = InterstitialAd(auctionKey = auctionKey)

    private var interstitialListener: InterstitialListener? = null
    private var interstitialAdInfo: Ad? = null

    init {
        interstitialAd.addExtra("mediator", "level_play")
    }

    override val ecpm: Double get() = interstitialAdInfo?.price ?: DEFAULT_ECPM
    override val demandId: String get() = interstitialAdInfo?.networkName ?: DEFAULT_DEMAND_ID
    override val isReady: Boolean get() = interstitialAd.isReady()

    fun setListener(listener: InterstitialListener) {
        this.interstitialListener = listener
        interstitialAd.setInterstitialListener(listener)
    }

    fun addExtra(key: String, value: Any?) {
        interstitialAd.addExtra(key, value)
    }

    fun load(activity: Activity) {
        interstitialAd.loadAd(activity = activity, pricefloor = BidonSdk.DefaultPricefloor)
    }

    fun show(activity: Activity) {
        interstitialAd.showAd(activity)
    }

    override fun applyAdInfo(ad: Ad): InterstitialAdInstance = this.apply { interstitialAdInfo = ad }

    override fun notifyLoss(winnerDemandId: String, winnerPrice: Double) {
        interstitialAd.notifyLoss(
            winnerDemandId = "lpca_$winnerDemandId",
            winnerPrice = winnerPrice,
        )
    }

    override fun destroy() {
        interstitialAd.destroyAd()
        interstitialListener = null
        interstitialAdInfo = null
    }
}
