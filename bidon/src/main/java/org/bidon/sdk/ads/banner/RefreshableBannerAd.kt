package org.bidon.sdk.ads.banner

/**
 * Created by Aleksei Cherniaev on 02/03/2023.
 */
interface RefreshableBannerAd : BannerAd {
    /**
     * By default AutoRefresh is on with [DefaultAutoRefreshTimeoutMs]
     */
    fun startAutoRefresh(timeoutMs: Long = DefaultAutoRefreshTimeoutMs)
    fun stopAutoRefresh()
}

abstract class RefreshableBannerView : RefreshableBannerAd