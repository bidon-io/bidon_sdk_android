package org.bidon.moloco.impl

import android.app.Activity
import com.moloco.sdk.publisher.AdLoad
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.MolocoAd
import com.moloco.sdk.publisher.MolocoAdError
import com.moloco.sdk.publisher.RewardedInterstitialAd
import com.moloco.sdk.publisher.RewardedInterstitialAdShowListener
import org.bidon.moloco.MolocoDemandId
import org.bidon.moloco.ext.toBidonLoadError
import org.bidon.moloco.ext.toBidonShowError
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

internal class MolocoRewardedImpl :
    AdSource.Rewarded<MolocoFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var rewardedAd: RewardedInterstitialAd? = null
    override val isAdReadyToShow
        get() = rewardedAd?.isLoaded == true

    private val loadListener = object : AdLoad.Listener {
        override fun onAdLoadSuccess(molocoAd: MolocoAd) {
            logInfo(TAG, "onAdLoadSuccess")
            getAd()?.let { emitEvent(AdEvent.Fill(it)) }
        }

        override fun onAdLoadFailed(molocoAdError: MolocoAdError) {
            val cause = molocoAdError.toBidonLoadError()
            logError(TAG, "onAdLoadFailed", cause)
            emitEvent(AdEvent.LoadFailed(cause))
        }
    }

    private val showListener: RewardedInterstitialAdShowListener by lazy {
        object : RewardedInterstitialAdShowListener {
            override fun onAdShowSuccess(molocoAd: MolocoAd) {
                logInfo(TAG, "onAdRendered")
                getAd()?.let {
                    emitEvent(AdEvent.Shown(it))
                    emitEvent(
                        AdEvent.PaidRevenue(
                            ad = it,
                            adValue = AdValue(
                                adRevenue = molocoAd.revenue?.toDouble() ?: 0.0,
                                currency = AdValue.USD,
                                precision = Precision.Precise
                            )
                        )
                    )
                }
            }

            override fun onAdShowFailed(molocoAdError: MolocoAdError) {
                logInfo(TAG, "onAdShowFailed: ${molocoAdError.description}")
                emitEvent(
                    AdEvent.ShowFailed(
                        (BidonError.Unspecified(
                            demandId,
                            molocoAdError.toBidonShowError()
                        ))
                    )
                )
            }

            override fun onAdHidden(molocoAd: MolocoAd) {
                logInfo(TAG, "onAdHidden: $this")
                getAd()?.let { emitEvent(AdEvent.Closed(it)) }
            }

            override fun onAdClicked(molocoAd: MolocoAd) {
                logInfo(TAG, "onAdClicked")
                getAd()?.let { emitEvent(AdEvent.Clicked(it)) }
            }

            override fun onUserRewarded(molocoAd: MolocoAd) {
                logInfo(TAG, "onUserEarnedReward: $this")
                getAd()?.let {
                    emitEvent(AdEvent.OnReward(ad = it, reward = null))
                }
            }

            override fun onRewardedVideoStarted(molocoAd: MolocoAd) {
                logInfo(TAG, "onRewardedVideoStarted: $this")
            }

            override fun onRewardedVideoCompleted(molocoAd: MolocoAd) {
                logInfo(TAG, "onRewardedVideoCompleted: $this")
            }
        }
    }

    override fun show(activity: Activity) {
        if (isAdReadyToShow) {
            rewardedAd?.show(showListener)
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }

    override fun load(adParams: MolocoFullscreenAuctionParams) {
        adParams.adUnitId ?: run {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(demandId = demandId, message = "adUnitId")
                )
            )
            return
        }
        adParams.payload ?: run {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(demandId = demandId, message = "payload")
                )
            )
            return
        }
        Moloco.createRewardedInterstitial(
            adUnitId = adParams.adUnitId
        ) { rewarded: RewardedInterstitialAd?, adCreateError: MolocoAdError.AdCreateError? ->
            if (rewarded != null) {
                rewardedAd = rewarded
                rewarded.load(adParams.payload, listener = loadListener)
            } else {
                emitEvent(
                    AdEvent.LoadFailed(
                        BidonError.Unspecified(
                            MolocoDemandId,
                            message = adCreateError?.description ?: "Created rewarded is null."
                        )
                    )
                )
            }
        }
    }

    override fun destroy() {
        rewardedAd?.destroy()
        rewardedAd = null
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return ObtainAuctionParamUseCase().getFullscreenParam(auctionParamsScope)
    }
}

private const val TAG = "MolocoRewardedImpl"
