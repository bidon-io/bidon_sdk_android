package org.bidon.sdk.ads.banner

import org.bidon.sdk.BidonSdk

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface BannerAd {
    fun setBannerFormat(bannerFormat: BannerFormat)
    fun loadAd(pricefloor: Double = BidonSdk.DefaultPricefloor)

    /**
     * Shows if banner is ready to show
     */
    fun isReady(): Boolean
    fun showAd()
    fun destroyAd()
    fun setBannerListener(listener: BannerListener)
}