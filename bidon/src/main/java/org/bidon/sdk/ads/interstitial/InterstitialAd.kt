package org.bidon.sdk.ads.interstitial

import android.app.Activity
import org.bidon.sdk.BidonSdk.DefaultPricefloor
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.stats.WinLossNotifier

/**
 * Created by Bidon Team on 06/02/2023.
 */
class InterstitialAd : Interstitial by InterstitialImpl()

interface Interstitial : Extras, WinLossNotifier {
    fun loadAd(activity: Activity, pricefloor: Double = DefaultPricefloor)
    fun destroyAd()
    fun isReady(): Boolean
    fun showAd(activity: Activity)
    fun setInterstitialListener(listener: InterstitialListener)
}
