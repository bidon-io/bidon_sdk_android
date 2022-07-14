package com.appodealstack.ironsource.rewarded

import androidx.core.os.bundleOf
import com.appodealstack.ironsource.ISDecorator
import com.appodealstack.ironsource.PlacementKey
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.demands.*

internal class RewardedImpl : ISDecorator.Rewarded {
    private var userListener: IronSourceRewardedListener? = null
    private var userLevelPlayListener: IronSourceLevelPlayRewardedListener? = null
    private val demandAd by lazy { DemandAd(AdType.Rewarded) }

    init {
        SdkCore.setListener(demandAd, object : AdListener {
            override fun onAdLoaded(ad: Ad) {
                userLevelPlayListener?.onAdReady(ad)
            }

            override fun onAdLoadFailed(cause: Throwable) {
                userLevelPlayListener?.onAdLoadFailed(cause)
            }

            override fun onAdDisplayed(ad: Ad) {
                userListener?.onRewardedVideoAdOpened()
                userLevelPlayListener?.onAdOpened(ad)
            }

            override fun onAdDisplayFailed(cause: Throwable) {
                userListener?.onRewardedVideoAdShowFailed(cause)
                userLevelPlayListener?.onAdShowFailed(cause)
            }

            override fun onAdImpression(ad: Ad) {
            }

            override fun onAdClicked(ad: Ad) {
                userListener?.onRewardedVideoAdClicked()
                userLevelPlayListener?.onAdClicked(ad)
            }

            override fun onAdHidden(ad: Ad) {
                userListener?.onRewardedVideoAdClosed()
                userLevelPlayListener?.onAdClosed(ad)
            }

            override fun onDemandAdLoaded(ad: Ad) {
                userListener?.onDemandAdLoaded(ad)
                userLevelPlayListener?.onDemandAdLoaded(ad)
            }

            override fun onDemandAdLoadFailed(cause: Throwable) {
                userListener?.onDemandAdLoadFailed(cause)
                userLevelPlayListener?.onDemandAdLoadFailed(cause)
            }

            override fun onAuctionFinished(ads: List<Ad>) {
                userListener?.onAuctionFinished(ads)
                userLevelPlayListener?.onAuctionFinished(ads)
            }

            override fun onRewardedStarted(ad: Ad) {
                userListener?.onRewardedVideoAdStarted()
            }

            override fun onRewardedCompleted(ad: Ad) {
                userListener?.onRewardedVideoAdEnded()
            }

            override fun onUserRewarded(ad: Ad, reward: RewardedAdListener.Reward?) {
                userListener?.onRewardedVideoAdRewarded(reward)
                userLevelPlayListener?.onAdRewarded(ad, reward)
            }
        })
    }

    override fun setRewardedVideoListener(rewardedVideoListener: IronSourceRewardedListener) {
        this.userListener = rewardedVideoListener
    }

    override fun setLevelPlayRewardedVideoListener(rewardedVideoListener: IronSourceLevelPlayRewardedListener) {
        this.userLevelPlayListener = rewardedVideoListener
    }

    override fun removeRewardedVideoListener() {
        this.userListener = null
        this.userLevelPlayListener = null
    }

    override fun loadRewardedVideo() {
        SdkCore.loadAd(activity = null, demandAd = demandAd, adParams = bundleOf())
    }

    override fun showRewardedVideo(placementName: String?) {
        SdkCore.showAd(
            activity = null,
            demandAd = demandAd,
            adParams = bundleOf(PlacementKey to placementName)
        )
    }
}