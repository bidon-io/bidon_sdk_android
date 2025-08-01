package org.bidon.sdk.ads.banner.refresh

import android.app.Activity
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.AuctionInfo
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.BannerListener
import org.bidon.sdk.ads.banner.BannerView
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.ext.TAG
import java.util.SortedMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Aleksei Cherniaev on 05/09/2023.
 */
@Deprecated("")
internal interface BannersCache {
    /**
     * Get and automatically load next banner
     */
    fun get(
        activity: Activity,
        format: BannerFormat,
        pricefloor: Double,
        auctionKey: String?,
        extras: Extras,
        onLoaded: (Ad, AuctionInfo, BannerView) -> Unit,
        onFailed: (AuctionInfo?, BidonError) -> Unit,
    )

    fun clear()
}

@Deprecated("")
internal class BannersCacheImpl : BannersCache {
    private val Tag get() = TAG
    private val isLoading = AtomicBoolean(false)
    private val cache = sortedMapOf<Pair<Ad, AuctionInfo>, BannerView>({ ad1, ad2 ->
        ((ad2.first.price - ad1.first.price) * 1000000).toInt()
    })

    override fun get(
        activity: Activity,
        format: BannerFormat,
        pricefloor: Double,
        auctionKey: String?,
        extras: Extras,
        onLoaded: (Ad, AuctionInfo, BannerView) -> Unit,
        onFailed: (AuctionInfo?, BidonError) -> Unit,
    ) {
        if (cache.isNotEmpty()) {
            val (ad, banner) = cache.pop() ?: return
            activity.runOnUiThread {
                onLoaded(ad.first, ad.second, banner)
            }
            return
        }
        if (!isLoading.getAndSet(true)) {
            activity.runOnUiThread {
                val banner = BannerView(
                    context = activity.applicationContext,
                    auctionKey = auctionKey
                )
                banner.setExtras(extras)
                banner.setBannerFormat(format)
                banner.setBannerListener(object : BannerListener {
                    override fun onAdLoaded(ad: Ad, auctionInfo: AuctionInfo) {
                        logInfo(Tag, "Banner loaded: $ad")
                        onLoaded(ad, auctionInfo, banner)
                        isLoading.set(false)
                    }

                    override fun onAdLoadFailed(auctionInfo: AuctionInfo?, cause: BidonError) {
                        logInfo(Tag, "Banner load failed: $cause")
                        onFailed(auctionInfo, cause)
                        isLoading.set(false)
                    }

                    override fun onAdShown(ad: Ad) {}

                    override fun onAdExpired(ad: Ad) {
                        cache.removeBannerView(banner)
                    }
                })
                banner.loadAd(activity, pricefloor)
            }
        }
    }

    override fun clear() {
        cache.clear()
    }

    private fun SortedMap<Pair<Ad, AuctionInfo>, BannerView>.pop(): Pair<Pair<Ad, AuctionInfo>, BannerView>? {
        if (isEmpty()) return null
        val ad = firstKey()
        val banner = this[ad] ?: return null
        remove(ad)
        logInfo(Tag, "Banner popped from cache: $banner, $ad")
        return ad to banner
    }

    private fun SortedMap<Pair<Ad, AuctionInfo>, BannerView>.removeBannerView(banner: BannerView) {
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