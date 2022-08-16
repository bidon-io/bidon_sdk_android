package com.appodealstack.applovin.impl

import com.applovin.mediation.*
import com.applovin.mediation.ads.MaxAdView
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.mediation.ads.MaxRewardedAd
import com.appodealstack.applovin.asAd
import com.appodealstack.bidon.SdkCore
import com.appodealstack.bidon.auctions.data.models.OldAuctionResult
import com.appodealstack.bidon.adapters.RewardedAdListener

internal fun MaxRewardedAd.setCoreListener(auctionResult: OldAuctionResult) {
    val core = SdkCore.getListenerForDemand(auctionResult.ad.demandAd)
    val demandAd = auctionResult.ad.demandAd
    this.setListener(
        object : MaxRewardedAdListener {
            override fun onAdLoaded(maxAd: MaxAd?) {
                core.onAdLoaded(maxAd.asAd(demandAd))
            }

            override fun onAdDisplayed(maxAd: MaxAd?) {
                core.onAdShown(maxAd.asAd(demandAd))
            }

            override fun onAdHidden(maxAd: MaxAd?) {
                core.onAdClosed(maxAd.asAd(demandAd))
            }

            override fun onAdClicked(maxAd: MaxAd?) {
                core.onAdClicked(maxAd.asAd(demandAd))
            }

            override fun onAdLoadFailed(adUnitId: String?, error: MaxError) {
                core.onAdLoadFailed(error.asBidonError())
            }

            override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError) {
                core.onAdShowFailed(error.asBidonError())
            }

            override fun onRewardedVideoStarted(maxAd: MaxAd?) {
                core.onRewardedStarted(maxAd.asAd(demandAd))
            }

            override fun onRewardedVideoCompleted(maxAd: MaxAd?) {
                core.onRewardedCompleted(maxAd.asAd(demandAd))
            }

            override fun onUserRewarded(ad: MaxAd?, reward: MaxReward?) {
                core.onUserRewarded(
                    ad = ad.asAd(demandAd),
                    reward = reward?.let {
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

internal fun MaxAdView.setCoreListener(auctionResult: OldAuctionResult) {
    val core = SdkCore.getListenerForDemand(auctionResult.ad.demandAd)
    val demandAd = auctionResult.ad.demandAd
    this.setListener(
        object : MaxAdViewAdListener {
            override fun onAdLoaded(maxAd: MaxAd?) {
                core.onAdLoaded(maxAd.asAd(demandAd))
            }

            override fun onAdDisplayed(maxAd: MaxAd?) {
                core.onAdShown(maxAd.asAd(demandAd))
            }

            override fun onAdHidden(maxAd: MaxAd?) {
                core.onAdClosed(maxAd.asAd(demandAd))
            }

            override fun onAdClicked(maxAd: MaxAd?) {
                core.onAdClicked(maxAd.asAd(demandAd))
            }

            override fun onAdLoadFailed(adUnitId: String?, error: MaxError) {
                core.onAdLoadFailed(error.asBidonError())
            }

            override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError) {
                core.onAdShowFailed(error.asBidonError())
            }

            override fun onAdExpanded(ad: MaxAd?) {
            }

            override fun onAdCollapsed(ad: MaxAd?) {
            }
        }
    )
}

internal fun MaxInterstitialAd.setCoreListener(auctionResult: OldAuctionResult) {
    val core = SdkCore.getListenerForDemand(auctionResult.ad.demandAd)
    this.setListener(
        object : MaxAdListener {
            override fun onAdLoaded(ad: MaxAd?) {
                core.onAdLoaded(auctionResult.ad)
            }

            override fun onAdDisplayed(ad: MaxAd?) {
                core.onAdShown(auctionResult.ad)
            }

            override fun onAdHidden(ad: MaxAd?) {
                core.onAdClosed(auctionResult.ad)
            }

            override fun onAdClicked(ad: MaxAd?) {
                core.onAdClicked(auctionResult.ad)
            }

            override fun onAdLoadFailed(adUnitId: String?, error: MaxError) {
                core.onAdLoadFailed(error.asBidonError())
            }

            override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError) {
                core.onAdShowFailed(error.asBidonError())
            }
        }
    )
}