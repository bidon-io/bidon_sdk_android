package com.applovin.mediation.adapters.banner

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.View.OnAttachStateChangeListener
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.adapters.keeper.AdInstance
import com.applovin.mediation.adapters.keeper.DEFAULT_BID_TYPE
import com.applovin.mediation.adapters.keeper.DEFAULT_DEMAND_ID
import com.applovin.mediation.adapters.keeper.DEFAULT_ECPM
import com.applovin.mediation.adapters.keeper.DEFAULT_UID
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.BannerListener
import org.bidon.sdk.ads.banner.BannerView

internal class BannerAdInstance(
    context: Context,
    format: MaxAdFormat,
    auctionKey: String? = null,
) : AdInstance {

    internal val bannerAd = BannerView(context = context, auctionKey = auctionKey)

    private var viewAttachListener: OnAttachStateChangeListener? = null
    private var bannerListener: BannerListener? = null
    private var bannerAdInfo: Ad? = null

    init {
        bannerAd.setBannerFormat(
            when (format) {
                MaxAdFormat.BANNER -> BannerFormat.Banner
                MaxAdFormat.MREC -> BannerFormat.MRec
                MaxAdFormat.LEADER -> BannerFormat.LeaderBoard
                else -> BannerFormat.Banner
            }
        )
        bannerAd.addExtra("mediator", "max")
    }

    override val ecpm: Double get() = bannerAdInfo?.price ?: DEFAULT_ECPM
    override val demandId: String get() = bannerAdInfo?.networkName ?: DEFAULT_DEMAND_ID
    override val isReady: Boolean get() = bannerAd.isReady()
    override val uid: String get() = bannerAdInfo?.adUnit?.uid ?: DEFAULT_UID
    override val bidType: String get() = bannerAdInfo?.bidType?.code ?: DEFAULT_BID_TYPE

    fun setListener(listener: BannerListener) {
        this.bannerListener = listener
        bannerAd.setBannerListener(listener)
    }

    fun addExtra(key: String, value: Any?) {
        bannerAd.addExtra(key, value)
    }

    fun load(activity: Activity) {
        bannerAd.loadAd(activity = activity, pricefloor = BidonSdk.DefaultPricefloor)
    }

    fun show() {
        viewAttachListener = object : OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                bannerAd.showAd()
                v.removeOnAttachStateChangeListener(this)
            }

            override fun onViewDetachedFromWindow(v: View) {
                // No-op
            }
        }
        bannerAd.addOnAttachStateChangeListener(viewAttachListener)
    }

    override fun applyAdInfo(ad: Ad): BannerAdInstance = this.apply { bannerAdInfo = ad }

    override fun notifyWin() {
        bannerAd.notifyWin()
    }

    override fun notifyLoss(winnerDemandId: String, winnerPrice: Double) {
        bannerAd.notifyLoss(
            winnerDemandId = "maxca_$winnerDemandId",
            winnerPrice = winnerPrice,
        )
    }

    override fun destroy() {
        bannerAd.removeOnAttachStateChangeListener(viewAttachListener)
        bannerAd.destroyAd()
        bannerListener = null
        bannerAdInfo = null
    }
}
