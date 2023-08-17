package org.bidon.admob.impl

import android.app.Activity
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import org.bidon.admob.AdmobFullscreenAdAuctionParams
import org.bidon.admob.asBidonError
import org.bidon.admob.ext.asBidonAdValue
import org.bidon.admob.ext.asBundle
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdLoadingType
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.rewarded.Reward
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

internal class AdmobRewardedImpl :
    AdSource.Rewarded<AdmobFullscreenAdAuctionParams>,
    AdLoadingType.Network<AdmobFullscreenAdAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var param: AdmobFullscreenAdAuctionParams? = null
    private var rewardedAd: RewardedAd? = null

    override val isAdReadyToShow: Boolean
        get() = rewardedAd != null

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            val lineItem = popLineItem(demandId) ?: error(BidonError.NoAppropriateAdUnitId)
            AdmobFullscreenAdAuctionParams(
                lineItem = lineItem,
                context = activity.applicationContext,
                adUnitId = requireNotNull(lineItem.adUnitId)
            )
        }
    }

    override fun fill(adParams: AdmobFullscreenAdAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        param = adParams
        val adRequest = AdRequest.Builder()
            .addNetworkExtrasBundle(AdMobAdapter::class.java, BidonSdk.regulation.asBundle())
            .build()
        val adUnitId = adParams.adUnitId
        val requestListener = object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                logError(
                    TAG,
                    "Error while loading ad. LoadAdError=$loadAdError.\n$this",
                    loadAdError.asBidonError()
                )
                emitEvent(AdEvent.LoadFailed(loadAdError.asBidonError()))
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                logInfo(TAG, "onAdLoaded. RewardedAd=$rewardedAd, $this")
                this@AdmobRewardedImpl.rewardedAd = rewardedAd
                rewardedAd.onPaidEventListener = OnPaidEventListener { adValue ->
                    emitEvent(
                        AdEvent.PaidRevenue(
                            ad = Ad(
                                demandAd = demandAd,
                                ecpm = param?.lineItem?.pricefloor ?: 0.0,
                                demandAdObject = rewardedAd,
                                networkName = demandId.demandId,
                                dsp = null,
                                roundId = roundId,
                                currencyCode = "USD",
                                auctionId = auctionId,
                                adUnitId = param?.lineItem?.adUnitId
                            ),
                            adValue = adValue.asBidonAdValue()
                        )
                    )
                }
                rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdClicked() {
                        logInfo(TAG, "onAdClicked: $this")
                        emitEvent(AdEvent.Clicked(rewardedAd.asAd()))
                    }

                    override fun onAdDismissedFullScreenContent() {
                        logInfo(TAG, "onAdDismissedFullScreenContent: $this")
                        emitEvent(AdEvent.Closed(rewardedAd.asAd()))
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        logError(TAG, "onAdFailedToShowFullScreenContent: $this", error.asBidonError())
                        emitEvent(AdEvent.ShowFailed(error.asBidonError()))
                    }

                    override fun onAdImpression() {
                        logInfo(TAG, "onAdShown: $this")
                        emitEvent(AdEvent.Shown(rewardedAd.asAd()))
                    }

                    override fun onAdShowedFullScreenContent() {}
                }
                emitEvent(AdEvent.Fill(rewardedAd.asAd()))
            }
        }
        RewardedAd.load(adParams.context, adUnitId, adRequest, requestListener)
    }

    override fun show(activity: Activity) {
        logInfo(TAG, "Starting show: $this")
        val rewardedAd = rewardedAd
        if (rewardedAd == null) {
            emitEvent(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        } else {
            rewardedAd.show(activity) { rewardItem ->
                logInfo(TAG, "onUserEarnedReward $rewardItem: $this")
                emitEvent(
                    AdEvent.OnReward(
                        ad = rewardedAd.asAd(),
                        reward = Reward(rewardItem.type, rewardItem.amount)
                    )
                )
                sendRewardImpression()
            }
        }
    }

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        rewardedAd?.onPaidEventListener = null
        rewardedAd?.fullScreenContentCallback = null
        rewardedAd = null
        param = null
    }

    private fun RewardedAd.asAd(): Ad {
        return Ad(
            demandAd = demandAd,
            ecpm = param?.lineItem?.pricefloor ?: 0.0,
            demandAdObject = this,
            networkName = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = "USD",
            auctionId = auctionId,
            adUnitId = param?.lineItem?.adUnitId
        )
    }
}

private const val TAG = "Admob Rewarded"
