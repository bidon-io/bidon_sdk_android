package com.appodealstack.ironsource.rewarded

import android.app.Activity
import androidx.core.os.bundleOf
import com.appodealstack.ironsource.ISDecorator
import com.appodealstack.ironsource.IronSourceDecorator
import com.appodealstack.ironsource.PlacementKey
import com.appodealstack.bidon.SdkCore
import com.appodealstack.bidon.auctions.AuctionResolver
import com.appodealstack.bidon.demands.*

internal class RewardedImpl : ISDecorator.Rewarded {
    private var userListener: IronSourceRewardedListener? = null
    private var userLevelPlayListener: IronSourceLevelPlayRewardedListener? = null
    private val demandAd by lazy { DemandAd(AdType.Rewarded) }

    init {
        SdkCore.setListener(demandAd, object : AdListener {
            override fun onAdLoaded(ad: Ad) {
                userLevelPlayListener?.onAdReady(ad)
                userListener?.onRewardedVideoAvailabilityChanged(available = true)
            }

            override fun onAdLoadFailed(cause: Throwable) {
                userLevelPlayListener?.onAdLoadFailed(cause)
                userListener?.onRewardedVideoAvailabilityChanged(available = false)
            }

            override fun onAdDisplayed(ad: Ad) {
                userListener?.onRewardedVideoAdOpened()
                userLevelPlayListener?.onAdOpened(ad)
                userListener?.onRewardedVideoAvailabilityChanged(available = false)
            }

            override fun onAdDisplayFailed(cause: Throwable) {
                userListener?.onRewardedVideoAdShowFailed(cause)
                userLevelPlayListener?.onAdShowFailed(cause)
                userListener?.onRewardedVideoAvailabilityChanged(available = false)
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
        showRewardedVideo(IronSourceDecorator.activity, placementName)
    }

    override fun showRewardedVideo(activity: Activity?, placementName: String?) {
        SdkCore.showAd(
            activity = activity ?: IronSourceDecorator.activity,
            demandAd = demandAd,
            adParams = bundleOf(PlacementKey to placementName)
        )
    }

    override fun hasRewardedVideo(): Boolean = SdkCore.canShow(demandAd)

    override fun setRewardedVideoAuctionResolver(auctionResolver: AuctionResolver) {
        SdkCore.saveAuctionResolver(demandAd, auctionResolver)
    }
}