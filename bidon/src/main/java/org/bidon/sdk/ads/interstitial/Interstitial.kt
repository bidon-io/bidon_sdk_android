package org.bidon.sdk.ads.interstitial

import android.app.Activity
import org.bidon.sdk.BidOnSdk.DefaultPlacement
import org.bidon.sdk.BidOnSdk.DefaultPricefloor

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
class Interstitial @JvmOverloads constructor(
    override val placementId: String = DefaultPlacement,
) : InterstitialAd by InterstitialAdImpl(placementId)

interface InterstitialAd {
    val placementId: String

    fun loadAd(activity: Activity, pricefloor: Double = DefaultPricefloor)
    fun destroyAd()
    fun isReady(): Boolean
    fun showAd(activity: Activity)
    fun setInterstitialListener(listener: InterstitialListener)
}
