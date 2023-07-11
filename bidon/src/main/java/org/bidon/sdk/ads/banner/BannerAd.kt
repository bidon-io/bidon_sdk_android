package org.bidon.sdk.ads.banner

import android.app.Activity
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.stats.LossNotifier

/**
 * Created by Bidon Team on 06/02/2023.
 */
interface BannerAd : LossNotifier, Extras {
    /**
     * Loaded Ad's size
     */
    val adSize: AdSize?

    fun setBannerFormat(bannerFormat: BannerFormat)
    fun loadAd(activity: Activity, pricefloor: Double = BidonSdk.DefaultPricefloor)

    /**
     * Shows if banner is ready to show
     */
    fun isReady(): Boolean
    fun showAd()
    fun destroyAd()
    fun setBannerListener(listener: BannerListener?)
}
