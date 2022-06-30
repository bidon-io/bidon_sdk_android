package com.appodealstack.applovin.ext

import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.appodealstack.applovin.base.asBidOnAd
import com.appodealstack.applovin.base.asBidOnError
import com.appodealstack.mads.demands.AdListener

fun AdListener.wrapToMaxAdListener(): MaxAdListener {
    val commonListener = this
    return object : MaxAdListener {
        override fun onAdLoaded(ad: MaxAd?) {
            commonListener.onAdLoaded(ad?.asBidOnAd())
        }

        override fun onAdDisplayed(ad: MaxAd?) {
            commonListener.onAdLoaded(ad?.asBidOnAd())
        }

        override fun onAdHidden(ad: MaxAd?) {
            commonListener.onAdHidden(ad?.asBidOnAd())
        }

        override fun onAdClicked(ad: MaxAd?) {
            commonListener.onAdClicked(ad?.asBidOnAd())
        }

        override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
            commonListener.onAdLoadFailed(adUnitId, error?.asBidOnError())
        }

        override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
            commonListener.onAdDisplayFailed(ad?.asBidOnAd(), error?.asBidOnError())
        }


    }
}