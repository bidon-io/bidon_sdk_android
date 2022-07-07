package com.appodealstack.applovin.impl

import com.applovin.mediation.*
import com.applovin.mediation.ads.MaxAdView
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.mediation.ads.MaxRewardedAd
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.auctions.AuctionResult
import com.appodealstack.mads.demands.Ad
import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.RewardedAdListener

internal fun MaxRewardedAd.setCoreListener(auctionResult: AuctionResult) {
    val core = SdkCore.getListenerForDemand(auctionResult.ad.demandAd)
    this.setListener(
        object : MaxRewardedAdListener {
            override fun onAdLoaded(ad: MaxAd?) {
                core.onAdClicked(auctionResult.ad)
            }

            override fun onAdDisplayed(ad: MaxAd?) {
                core.onAdDisplayed(auctionResult.ad)
            }

            override fun onAdHidden(ad: MaxAd?) {
                core.onAdHidden(auctionResult.ad)
            }

            override fun onAdClicked(ad: MaxAd?) {
                core.onAdClicked(auctionResult.ad)
            }

            override fun onAdLoadFailed(adUnitId: String?, error: MaxError) {
                core.onAdLoadFailed(error.asBidonError())
            }

            override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError) {
                core.onAdDisplayFailed(error.asBidonError())
            }

            override fun onRewardedVideoStarted(ad: MaxAd?) {
                core.onRewardedStarted(auctionResult.ad)
            }

            override fun onRewardedVideoCompleted(ad: MaxAd?) {
                core.onRewardedCompleted(auctionResult.ad)
            }

            override fun onUserRewarded(ad: MaxAd?, reward: MaxReward?) {
                core.onUserRewarded(
                    auctionResult.ad, reward?.let {
                        RewardedAdListener.Reward(
                            label = reward.label ?: "",
                            amount = reward.amount
                        )
                    }
                )
            }
        }
    )
}

internal fun MaxAdView.setCoreListener(auctionResult: AuctionResult) {
    val core = SdkCore.getListenerForDemand(auctionResult.ad.demandAd)
    this.setListener(
        object : MaxAdViewAdListener {
            override fun onAdLoaded(ad: MaxAd?) {
                core.onAdLoaded(auctionResult.ad)
            }

            override fun onAdDisplayed(ad: MaxAd?) {
                core.onAdDisplayed(auctionResult.ad)
            }

            override fun onAdHidden(ad: MaxAd?) {
                core.onAdHidden(auctionResult.ad)
            }

            override fun onAdClicked(ad: MaxAd?) {
                core.onAdClicked(auctionResult.ad)
            }

            override fun onAdLoadFailed(adUnitId: String?, error: MaxError) {
                core.onAdLoadFailed(error.asBidonError())
            }

            override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError) {
                core.onAdDisplayFailed(error.asBidonError())
            }

            override fun onAdExpanded(ad: MaxAd?) {
            }

            override fun onAdCollapsed(ad: MaxAd?) {
            }
        }
    )
}

internal fun MaxInterstitialAd.setCoreListener(auctionResult: AuctionResult) {
    val core = SdkCore.getListenerForDemand(auctionResult.ad.demandAd)
    this.setListener(
        object : MaxAdListener {
            override fun onAdLoaded(ad: MaxAd?) {
                core.onAdClicked(auctionResult.ad)
            }

            override fun onAdDisplayed(ad: MaxAd?) {
                core.onAdDisplayed(auctionResult.ad)
            }

            override fun onAdHidden(ad: MaxAd?) {
                core.onAdHidden(auctionResult.ad)
            }

            override fun onAdClicked(ad: MaxAd?) {
                core.onAdClicked(auctionResult.ad)
            }

            override fun onAdLoadFailed(adUnitId: String?, error: MaxError) {
                core.onAdLoadFailed(error.asBidonError())
            }

            override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError) {
                core.onAdDisplayFailed(error.asBidonError())
            }
        }
    )
}