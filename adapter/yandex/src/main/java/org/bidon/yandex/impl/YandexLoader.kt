package org.bidon.yandex.impl

import android.content.Context
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader

internal val singleLoader: YandexLoader by lazy { YandexLoaderImpl() }

internal class YandexLoaderImpl : YandexLoader {

    private var interstitialAdLoader: InterstitialAdLoader? = null
    private var rewardedAdLoader: RewardedAdLoader? = null

    override fun requestInterstitialAd(
        context: Context,
        adRequestConfiguration: AdRequestConfiguration,
        adLoadListener: InterstitialAdLoadListener
    ) {
        val interstitialAdLoader = interstitialAdLoader ?: createInterstitialAdLoader(context)
        interstitialAdLoader.setAdLoadListener(object : InterstitialAdLoadListener {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                adLoadListener.onAdLoaded(interstitialAd)
                interstitialAdLoader.setAdLoadListener(null)
            }

            override fun onAdFailedToLoad(error: AdRequestError) {
                adLoadListener.onAdFailedToLoad(error)
                interstitialAdLoader.setAdLoadListener(null)
            }
        })
        interstitialAdLoader.loadAd(adRequestConfiguration)
    }

    override fun requestRewardedAd(
        context: Context,
        adRequestConfiguration: AdRequestConfiguration,
        adLoadListener: RewardedAdLoadListener
    ) {
        val rewardedAdLoader = rewardedAdLoader ?: createRewardedAdLoader(context)
        rewardedAdLoader.setAdLoadListener(object : RewardedAdLoadListener {
            override fun onAdLoaded(rewarded: RewardedAd) {
                adLoadListener.onAdLoaded(rewarded)
                rewardedAdLoader.setAdLoadListener(null)
            }

            override fun onAdFailedToLoad(error: AdRequestError) {
                adLoadListener.onAdFailedToLoad(error)
                rewardedAdLoader.setAdLoadListener(null)
            }
        })
        rewardedAdLoader.loadAd(adRequestConfiguration)
    }

    private fun createInterstitialAdLoader(context: Context): InterstitialAdLoader {
        return InterstitialAdLoader(context).also {
            this.interstitialAdLoader = it
        }
    }

    private fun createRewardedAdLoader(context: Context): RewardedAdLoader {
        return RewardedAdLoader(context).also {
            this.rewardedAdLoader = it
        }
    }
}

internal interface YandexLoader {
    fun requestInterstitialAd(
        context: Context,
        adRequestConfiguration: AdRequestConfiguration,
        adLoadListener: InterstitialAdLoadListener
    )

    fun requestRewardedAd(
        context: Context,
        adRequestConfiguration: AdRequestConfiguration,
        adLoadListener: RewardedAdLoadListener
    )
}
