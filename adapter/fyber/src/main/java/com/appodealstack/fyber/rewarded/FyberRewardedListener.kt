package com.appodealstack.fyber.rewarded

import com.appodealstack.bidon.ads.Ad

interface FyberRewardedListener {
    fun onAvailable(placementId: String, ad: Ad)
    fun onUnavailable(placementId: String, cause: Throwable)
    fun onShow(placementId: String, ad: Ad)
    fun onShowFailure(placementId: String, cause: Throwable)
    fun onClick(placementId: String, ad: Ad)
    fun onHide(placementId: String, ad: Ad)

    fun onDemandAdLoaded(placementId: String, ad: Ad)
    fun onDemandAdLoadFailed(placementId: String, cause: Throwable)
    fun onAuctionFinished(placementId: String, ads: List<Ad>)

    fun onCompletion(placementId: String, userRewarded: Boolean)
}
