package com.appodealstack.fyber.interstitial

import com.appodealstack.bidon.demands.Ad

interface FyberInterstitialListener {
     fun onAvailable(placementId: String, ad: Ad)
     fun onUnavailable(placementId: String, cause: Throwable)
     fun onShow(placementId: String, ad: Ad)
     fun onShowFailure(placementId: String, cause: Throwable)
     fun onClick(placementId: String, ad: Ad)
     fun onHide(placementId: String, ad: Ad)

     fun onDemandAdLoaded(placementId: String, ad: Ad)
     fun onDemandAdLoadFailed(placementId: String, cause: Throwable)
     fun onAuctionFinished(placementId: String, ads: List<Ad>)
}