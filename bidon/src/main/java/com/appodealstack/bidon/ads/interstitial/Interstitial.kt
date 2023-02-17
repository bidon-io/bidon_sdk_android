package com.appodealstack.bidon.ads.interstitial

import android.app.Activity
import com.appodealstack.bidon.BidOnSdk.DefaultMinPrice
import com.appodealstack.bidon.BidOnSdk.DefaultPlacement

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
class Interstitial @JvmOverloads constructor(
    override val placementId: String = DefaultPlacement,
) : InterstitialAd by InterstitialAdImpl(placementId)

interface InterstitialAd {
    val placementId: String

    fun loadAd(activity: Activity, minPrice: Double = DefaultMinPrice)
    fun destroyAd()
    fun isReady(): Boolean
    fun showAd(activity: Activity)
    fun setInterstitialListener(listener: InterstitialListener)
}
