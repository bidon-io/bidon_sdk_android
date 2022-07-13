package com.appodealstack.fyber.banner

import com.appodealstack.mads.demands.Ad

interface FyberBannerListener {
     fun onError(placementId: String, cause: Throwable)
     fun onLoad(placementId: String, ad: Ad)

     /**
      * Ad revenue appears here at [ad.price]
      */
     fun onShow(placementId: String, ad: Ad)
     fun onClick(placementId: String, ad: Ad)
     fun onRequestStart(placementId: String, ad: Ad)

     fun onDemandAdLoaded(placementId: String, ad: Ad)
     fun onDemandAdLoadFailed(placementId: String, cause: Throwable)
     fun onAuctionFinished(placementId: String, ads: List<Ad>)
}