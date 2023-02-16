package com.appodealstack.bidon.ads.interstitial

import android.app.Activity
import com.appodealstack.bidon.BidOnSdk.Companion.DefaultMinPrice
import com.appodealstack.bidon.BidOnSdk.Companion.DefaultPlacement

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
class Interstitial(
    override val placementId: String = DefaultPlacement,
) : InterstitialAd by InterstitialAdImpl(placementId)

interface InterstitialAd {
    val placementId: String

    fun load(activity: Activity, minPrice: Double = DefaultMinPrice)
    fun destroy()
    fun show(activity: Activity)
    fun setInterstitialListener(listener: InterstitialListener)
}
