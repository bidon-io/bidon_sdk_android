package com.appodealstack.fyber.rewarded

import android.app.Activity
import androidx.core.os.bundleOf
import com.appodealstack.fyber.PlacementKey
import com.appodealstack.bidon.SdkCore
import com.appodealstack.bidon.core.DefaultAutoRefreshTimeoutMs
import com.appodealstack.bidon.demands.*
import com.appodealstack.bidon.demands.banners.AutoRefresh
import com.fyber.fairbid.ads.ImpressionData
import com.fyber.fairbid.ads.Interstitial
import com.fyber.fairbid.ads.LossNotificationReason
import com.fyber.fairbid.ads.ShowOptions

object BNFyberRewarded : FyberRewarded by FyberRewardedImpl()

interface FyberRewarded {
    fun request(placementId: String)
    fun request(placementId: String, activity: Activity?)
    fun setRewardedListener(fyberRewardedListener: FyberRewardedListener)
    fun show(placementId: String, activity: Activity)
    fun show(placementId: String, showOptions: ShowOptions, activity: Activity)
    fun isAvailable(placementId: String): Boolean
    fun getImpressionData(placementId: String): ImpressionData?
    fun enableAutoRequesting(placementId: String)
    fun disableAutoRequesting(placementId: String)
    fun getImpressionDepth(): Int
    fun notifyLoss(placementId: String, reason: LossNotificationReason)
}

class FyberRewardedImpl : FyberRewarded {
    private val ads = mutableMapOf<String, DemandAd>()
    private var fyberRewardedListener: FyberRewardedListener? = null

    override fun request(placementId: String) {
        request(placementId, null)
    }

    override fun request(placementId: String, activity: Activity?) {
        val demandAd = getDemandAd(placementId)
        fyberRewardedListener?.let {
            setInterstitialListener(
                demandAd = demandAd,
                fyberRewardedListener = it,
                placementId = placementId
            )
        }
        SdkCore.loadAd(activity, demandAd, bundleOf(PlacementKey to placementId))
    }

    override fun setRewardedListener(fyberRewardedListener: FyberRewardedListener) {
        this.fyberRewardedListener = fyberRewardedListener
        ads.forEach { (placementId, demandAd) ->
            setInterstitialListener(demandAd, fyberRewardedListener, placementId)
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
        DemandAd(AdType.Rewarded)
    }

    private fun setInterstitialListener(
        demandAd: DemandAd,
        fyberRewardedListener: FyberRewardedListener,
        placementId: String
    ) {
        SdkCore.setListener(demandAd, object : AdListener {
            override fun onAdLoaded(ad: Ad) {
                fyberRewardedListener.onAvailable(placementId, ad)
            }

            override fun onAdLoadFailed(cause: Throwable) {
                fyberRewardedListener.onUnavailable(placementId, cause)
            }

            override fun onAdDisplayed(ad: Ad) {
                fyberRewardedListener.onShow(placementId, ad)
            }

            override fun onAdDisplayFailed(cause: Throwable) {
                fyberRewardedListener.onShowFailure(placementId, cause)
            }

            override fun onAdImpression(ad: Ad) {
            }

            override fun onAdClicked(ad: Ad) {
                fyberRewardedListener.onClick(placementId, ad)
            }

            override fun onAdHidden(ad: Ad) {
                fyberRewardedListener.onHide(placementId, ad)
            }

            override fun onDemandAdLoaded(ad: Ad) {
                fyberRewardedListener.onDemandAdLoaded(placementId, ad)
            }

            override fun onDemandAdLoadFailed(cause: Throwable) {
                fyberRewardedListener.onDemandAdLoadFailed(placementId, cause)
            }

            override fun onAuctionFinished(ads: List<Ad>) {
                fyberRewardedListener.onAuctionFinished(placementId, ads)
            }

            override fun onUserRewarded(ad: Ad, reward: RewardedAdListener.Reward?) {
                fyberRewardedListener.onCompletion(placementId, userRewarded = reward != null)
            }
        })
    }
}
