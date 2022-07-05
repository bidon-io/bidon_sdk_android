package com.appodealstack.applovin.ext

import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.appodealstack.applovin.ApplovinMaxDemandId
import com.appodealstack.mads.auctions.AuctionData
import com.appodealstack.mads.demands.ObjRequest
import com.appodealstack.mads.demands.AdType
import com.appodealstack.mads.demands.AdListener

fun AdListener.wrapToMaxAdListener(objRequest: ObjRequest): MaxAdListener {
    val commonListener = this
    return object : MaxAdListener {
        override fun onAdLoaded(ad: MaxAd) {
            commonListener.onAdLoaded(ad.asAuctionDataSuccess())
        }

        override fun onAdDisplayed(ad: MaxAd) {
            commonListener.onAdDisplayed(ad.asAuctionDataSuccess())
        }

        override fun onAdHidden(ad: MaxAd) {
            commonListener.onAdHidden(ad.asAuctionDataSuccess())
        }

        override fun onAdClicked(ad: MaxAd) {
            commonListener.onAdClicked(ad.asAuctionDataSuccess())
        }

        override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
            commonListener.onDemandAdLoadFailed(error.asAuctionDataFailure())
        }

        override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
            commonListener.onAdDisplayFailed(error.asAuctionDataFailure())
        }

        private fun MaxAd.asAuctionDataSuccess() = AuctionData.Success(
            demandId = ApplovinMaxDemandId,
            adType = AdType.Interstitial,
            price = this.revenue,
            objRequest = objRequest,
            objResponse = this,
        )

        private fun MaxError?.asAuctionDataFailure() = AuctionData.Failure(
            demandId = ApplovinMaxDemandId,
            adType = AdType.Interstitial,
            objRequest = objRequest,
            cause = this?.asBidonError()
        )
    }
}