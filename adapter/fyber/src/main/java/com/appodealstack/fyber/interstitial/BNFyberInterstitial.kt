package com.appodealstack.fyber.interstitial

import android.app.Activity
import androidx.core.os.bundleOf
import com.appodealstack.fyber.PlacementKey
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.core.DefaultAutoRefreshTimeoutMs
import com.appodealstack.mads.demands.Ad
import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.AdType
import com.appodealstack.mads.demands.DemandAd
import com.appodealstack.mads.demands.banners.AutoRefresh
import com.fyber.fairbid.ads.ImpressionData
import com.fyber.fairbid.ads.Interstitial
import com.fyber.fairbid.ads.LossNotificationReason
import com.fyber.fairbid.ads.ShowOptions

object BNFyberInterstitial : FyberInterstitial by FyberInterstitialImpl()

interface FyberInterstitial {
    fun request(placementId: String)
    fun request(placementId: String, activity: Activity?)
    fun setInterstitialListener(fyberInterstitialListener: FyberInterstitialListener)
    fun show(placementId: String, activity: Activity)
    fun show(placementId: String, showOptions: ShowOptions, activity: Activity)
    fun isAvailable(placementId: String): Boolean
    fun getImpressionData(placementId: String): ImpressionData?
    fun enableAutoRequesting(placementId: String)
    fun disableAutoRequesting(placementId: String)
    fun getImpressionDepth(): Int
    fun notifyLoss(placementId: String, reason: LossNotificationReason)
}

class FyberInterstitialImpl : FyberInterstitial {
    private val ads = mutableMapOf<String, DemandAd>()
    private var fyberInterstitialListener: FyberInterstitialListener? = null

    override fun request(placementId: String) {
        request(placementId, null)
    }

    override fun request(placementId: String, activity: Activity?) {
        val demandAd = getDemandAd(placementId)
        fyberInterstitialListener?.let {
            setInterstitialListener(
                demandAd = demandAd,
                fyberInterstitialListener = it,
                placementId = placementId
            )
        }
        SdkCore.loadAd(activity, demandAd, bundleOf(PlacementKey to placementId))
    }

    override fun setInterstitialListener(fyberInterstitialListener: FyberInterstitialListener) {
        this.fyberInterstitialListener = fyberInterstitialListener
        ads.forEach { (placementId, demandAd) ->
            setInterstitialListener(demandAd, fyberInterstitialListener, placementId)
        }
    }

    override fun show(placementId: String, activity: Activity) {
        SdkCore.showAd(activity, demandAd = getDemandAd(placementId), bundleOf(PlacementKey to placementId))
    }

    override fun show(placementId: String, showOptions: ShowOptions, activity: Activity) {
        val bundle = bundleOf(PlacementKey to placementId).apply {
            showOptions.customParameters.forEach { (key, value) ->
                this.putString(key, value)
            }
        }
        SdkCore.showAd(
            activity = activity,
            demandAd = getDemandAd(placementId),
            adParams = bundle
        )
    }

    override fun isAvailable(placementId: String): Boolean {
        return Interstitial.isAvailable(placementId)
    }

    override fun getImpressionData(placementId: String): ImpressionData? {
        return Interstitial.getImpressionData(placementId)
    }

    override fun enableAutoRequesting(placementId: String) {
        SdkCore.setAutoRefresh(getDemandAd(placementId), AutoRefresh.On(DefaultAutoRefreshTimeoutMs))
    }

    override fun disableAutoRequesting(placementId: String) {
        SdkCore.setAutoRefresh(getDemandAd(placementId), AutoRefresh.Off)
    }

    override fun getImpressionDepth(): Int {
        return Interstitial.getImpressionDepth()
    }

    override fun notifyLoss(placementId: String, reason: LossNotificationReason) {
        Interstitial.notifyLoss(placementId, reason)
    }

    private fun getDemandAd(placementId: String) = ads.getOrPut(placementId) {
        DemandAd(AdType.Interstitial)
    }

    private fun setInterstitialListener(
        demandAd: DemandAd,
        fyberInterstitialListener: FyberInterstitialListener,
        placementId: String
    ) {
        SdkCore.setListener(demandAd, object : AdListener {
            override fun onAdLoaded(ad: Ad) {
                fyberInterstitialListener.onAvailable(placementId, ad)
            }

            override fun onAdLoadFailed(cause: Throwable) {
                fyberInterstitialListener.onUnavailable(placementId, cause)
            }

            override fun onAdDisplayed(ad: Ad) {
                fyberInterstitialListener.onShow(placementId, ad)
            }

            override fun onAdDisplayFailed(cause: Throwable) {
                fyberInterstitialListener.onShowFailure(placementId, cause)
            }

            override fun onAdClicked(ad: Ad) {
                fyberInterstitialListener.onClick(placementId, ad)
            }

            override fun onAdHidden(ad: Ad) {
                fyberInterstitialListener.onHide(placementId, ad)
            }

            override fun onDemandAdLoaded(ad: Ad) {
                fyberInterstitialListener.onDemandAdLoaded(placementId, ad)
            }

            override fun onDemandAdLoadFailed(cause: Throwable) {
                fyberInterstitialListener.onDemandAdLoadFailed(placementId, cause)
            }

            override fun onAuctionFinished(ads: List<Ad>) {
                fyberInterstitialListener.onAuctionFinished(placementId, ads)
            }
        })
    }
}
