package org.bidon.sdk.ads.banner.refresh

import android.app.Activity
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.BannerListener
import org.bidon.sdk.ads.banner.BannerView
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.ext.TAG
import java.util.SortedMap

/**
 * Created by Aleksei Cherniaev on 05/09/2023.
 */
internal interface BannersCache {
    /**
     * Get and automatically load next banner
     */
    fun get(
        activity: Activity,
        format: BannerFormat,
        pricefloor: Double,
        extras: Extras,
        onLoaded: (Ad, BannerView) -> Unit,
        onFailed: (BidonError) -> Unit,
    )

    fun clear()
}

internal class BannersCacheImpl : BannersCache {
    private val Tag get() = TAG
    private val cache = sortedMapOf<Ad, BannerView>({ ad1, ad2 ->
        ((ad2.ecpm - ad1.ecpm) * 1000000).toInt()
    })

    override fun get(
        activity: Activity,
        format: BannerFormat,
        pricefloor: Double,
        extras: Extras,
        onLoaded: (Ad, BannerView) -> Unit,
        onFailed: (BidonError) -> Unit,
    ) {
        if (cache.isNotEmpty()) {
            val (ad, banner) = cache.pop() ?: return
            onLoaded(ad, banner)
            return
        }
        activity.runOnUiThread {
            val banner = BannerView(activity.applicationContext)
            banner.setExtras(extras)
            banner.setBannerFormat(format)
            banner.setBannerListener(object : BannerListener {
                override fun onAdLoaded(ad: Ad) {
                    logInfo(Tag, "Banner loaded: $ad")
                    onLoaded(ad, banner)
                }

                override fun onAdLoadFailed(cause: BidonError) {
                    logInfo(Tag, "Banner load failed: $cause")
                    onFailed(cause)
                }

                override fun onAdShown(ad: Ad) {}

                override fun onAdExpired(ad: Ad) {
                    cache.removeBannerView(banner)
                }
            })
            banner.loadAd(activity, pricefloor)
        }
    }

    override fun clear() {
        cache.clear()
    }

    private fun SortedMap<Ad, BannerView>.pop(): Pair<Ad, BannerView>? {
        if (isEmpty()) return null
        val ad = firstKey()
        val banner = this[ad] ?: return null
        remove(ad)
        logInfo(Tag, "Banner popped from cache: $banner, $ad")
        return ad to banner
    }

    private fun SortedMap<Ad, BannerView>.removeBannerView(banner: BannerView) {
        if (this.containsValue(banner)) {
            logInfo(Tag, "Banner expired and will be removed from cache: $banner")
            val (key, _) = this.filter { (_, bannerView) ->
                banner == bannerView
            }.entries.first()
            this.remove(key)
        }
    }

    private fun BannerView.setExtras(extras: Extras) {
        extras.getExtras().forEach { (key, value) ->
            this.addExtra(key, value)
        }
    }
}