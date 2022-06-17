package com.appodealstack.mads.demands

import android.content.Context
import android.view.View

interface AdObjectListener {
    fun onAdLoaded(adObject: AdObject, price: Double?)
    fun onAdFailToLoad(adObject: AdObject, cause: Throwable)
    fun onAdShown(adObject: AdObject)
    fun onAdFailToShow(adObject: AdObject, cause: Throwable)
    fun onAdClicked(adObject: AdObject)
    fun onAdExpired(adObject: AdObject)
}

sealed interface AdObject {
    val listener: AdObjectListener
    val isLoaded: Boolean
    val canShow: Boolean
    val isDestroyed: Boolean

    fun load(context: Context, listener: AdObjectListener)

    sealed interface AdView : AdObject {
        fun getView(): View?

        /**
         * Banner
         */
        interface BannerAdObject : AdView {
            override val listener: BannerAdObjectListener

            interface BannerAdObjectListener : AdObjectListener {
                fun onAdExpanded(adObject: BannerAdObject)
                fun onAdCollapsed(adObject: BannerAdObject)
            }
        }

        /**
         * Native ad
         */
        interface NativeAdObject : AdView {
            override val listener: AdObjectListener
        }
    }

    sealed interface Fullscreen : AdObject {
        fun show(context: Context, listener: AdObjectListener)

        /**
         * Rewarded ad
         */
        interface RewardedAdObject : Fullscreen {
            override val listener: RewardedAdObjectListener

            interface RewardedAdObjectListener : AdObjectListener {
                fun onRewarded(adObject: RewardedAdObject, rewardedInfo: RewardedInfo)
            }
        }

        /**
         * Interstitial ad
         */
        interface InterstitialAdObject : Fullscreen {
            override val listener: AdObjectListener
            fun onAdClose(adObject: InterstitialAdObject)
        }
    }
}