package org.bidon.sdk.ads.interstitial

import android.app.Activity
import org.bidon.sdk.BidonSdk.DefaultPlacement
import org.bidon.sdk.BidonSdk.DefaultPricefloor

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
class InterstitialAd @JvmOverloads constructor(
    override val placementId: String = DefaultPlacement,
) : Interstitial by InterstitialImpl(placementId)

interface Interstitial {
    val placementId: String

    fun loadAd(activity: Activity, pricefloor: Double = DefaultPricefloor)
    fun destroyAd()
    fun isReady(): Boolean
    fun showAd(activity: Activity)
    fun setInterstitialListener(listener: InterstitialListener)
}
