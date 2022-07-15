package com.appodealstack.fyber.banner

import android.app.Activity
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.appodealstack.fyber.PlacementKey
import com.appodealstack.fyber.banner.BNFyberBannerOption.Position
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.demands.Ad
import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.AdType
import com.appodealstack.mads.demands.DemandAd
import com.appodealstack.mads.demands.banners.AutoRefresh
import com.fyber.fairbid.ads.Banner

object BNFyberBanner : FyberBanner by FyberBannerImpl()

interface FyberBanner {
    fun setBannerListener(fyberBannerListener: FyberBannerListener)

    // fun show(placementId: String, activity: Activity) - todo handle position.
    fun show(placementId: String, showOptions: BNFyberBannerOption, activity: Activity)
    fun destroy(placementId: String)
    fun getImpressionDepth(): Int
}

class FyberBannerImpl : FyberBanner {
    private val ads = mutableMapOf<String, DemandAd>()
    private val bannerViews = mutableMapOf<String, ViewGroup>()
    private val positions = mutableMapOf<String, Position>()
    private var fyberBannerListener: FyberBannerListener? = null

    override fun setBannerListener(fyberBannerListener: FyberBannerListener) {
        this.fyberBannerListener = fyberBannerListener
        ads.forEach { (placementId, demandAd) ->
            setBannerListener(demandAd, fyberBannerListener, placementId)
        }
    }

    override fun show(placementId: String, showOptions: BNFyberBannerOption, activity: Activity) {
        val position = showOptions.getPosition()
        require(position is Position.InViewGroup) {
            "Top/Bottom position is not implemented at the moment"
        }
        position.viewGroup.isVisible = false
        bannerViews[placementId] = position.viewGroup

        val adContainer = position.viewGroup

        val demandAd = getDemandAd(placementId)
        positions[placementId] = showOptions.getPosition()
        fyberBannerListener?.let {
            setBannerListener(
                demandAd = demandAd,
                fyberBannerListener = it,
                placementId = placementId
            )
        }
        SdkCore.loadAdView(
            context = activity,
            adContainer = adContainer,
            demandAd = demandAd,
            adParams = bundleOf(PlacementKey to placementId),
            autoRefresh = AutoRefresh.Off,
            onViewReady = { adView ->
                if (adView != bannerViews[placementId]) {
                    // Winner is not Fyber, so we have to cancel Fyber FairBid banner.
                    Banner.destroy(placementId)
                    bannerViews[placementId]?.let { adContainer ->
                        adContainer.removeAllViews()
                        adContainer.addView(adView)
                        adContainer.isVisible = true
                    }
                } else {
                    // Winner is Fyber FairBid banner.
                    adView.isVisible = true
                }
            }
        )
    }

    override fun destroy(placementId: String) {
        bannerViews[placementId]?.removeAllViews()
        bannerViews.remove(placementId)
        SdkCore.destroyAd(getDemandAd(placementId), bundleOf())
    }

    override fun getImpressionDepth(): Int {
        return Banner.getImpressionDepth()
    }

    private fun getDemandAd(placementId: String) = ads.getOrPut(placementId) {
        DemandAd(AdType.Banner)
    }

    private fun setBannerListener(
        demandAd: DemandAd,
        fyberBannerListener: FyberBannerListener,
        placementId: String
    ) {
        SdkCore.setListener(demandAd, object : AdListener {
            override fun onAdLoaded(ad: Ad) {
                fyberBannerListener.onLoad(placementId, ad)
            }

            override fun onAdLoadFailed(cause: Throwable) {
                fyberBannerListener.onError(placementId, cause)
            }

            override fun onAdDisplayed(ad: Ad) {
                fyberBannerListener.onShow(placementId, ad)
            }

            override fun onAdDisplayFailed(cause: Throwable) {
            }

            override fun onAdImpression(ad: Ad) {
            }

            override fun onAdClicked(ad: Ad) {
                fyberBannerListener.onClick(placementId, ad)
            }

            override fun onAdHidden(ad: Ad) {
            }

            override fun onDemandAdLoaded(ad: Ad) {
                fyberBannerListener.onDemandAdLoaded(placementId, ad)
            }

            override fun onDemandAdLoadFailed(cause: Throwable) {
                fyberBannerListener.onDemandAdLoadFailed(placementId, cause)
            }

            override fun onAuctionFinished(ads: List<Ad>) {
                fyberBannerListener.onAuctionFinished(placementId, ads)
            }
        })
    }
}
