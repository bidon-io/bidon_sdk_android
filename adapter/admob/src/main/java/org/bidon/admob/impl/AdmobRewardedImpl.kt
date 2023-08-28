package org.bidon.admob.impl

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import org.bidon.admob.AdmobFullscreenAdAuctionParams
import org.bidon.admob.AdmobInitParameters
import org.bidon.admob.asBidonError
import org.bidon.admob.ext.asBidonAdValue
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.rewarded.Reward
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

internal class AdmobRewardedImpl(
    configParams: AdmobInitParameters?,
    private val getAdRequest: GetAdRequestUseCase = GetAdRequestUseCase(configParams),
    private val getFullScreenContentCallback: GetFullScreenContentCallbackUseCase = GetFullScreenContentCallbackUseCase(),
    private val obtainToken: GetTokenUseCase = GetTokenUseCase(configParams),
    private val obtainAdAuctionParams: GetAdAuctionParamsUseCase = GetAdAuctionParamsUseCase(),
) : AdSource.Rewarded<AdmobFullscreenAdAuctionParams>,
    Mode.Bidding,
    Mode.Network,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var param: AdmobFullscreenAdAuctionParams? = null
    private var rewardedAd: RewardedAd? = null
    private var isBiddingMode: Boolean = false

    override val isAdReadyToShow: Boolean
        get() = rewardedAd != null

    override suspend fun getToken(context: Context): String? {
        isBiddingMode = true
        return obtainToken(context, demandAd.adType)
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return obtainAdAuctionParams(auctionParamsScope, demandAd.adType, isBiddingMode)
    }

    override fun load(adParams: AdmobFullscreenAdAuctionParams) {
        logInfo(TAG, "Starting with $adParams")
        val adRequest = getAdRequest(adParams)
        param = adParams
        val requestListener = object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                logError(TAG, "onAdFailedToLoad: $loadAdError. $this", loadAdError.asBidonError())
                emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                logInfo(TAG, "onAdLoaded. RewardedAd=$rewardedAd, $this")
                this@AdmobRewardedImpl.rewardedAd = rewardedAd
                rewardedAd.onPaidEventListener = OnPaidEventListener { adValue ->
                    emitEvent(
                        AdEvent.PaidRevenue(
                            ad = rewardedAd.asAd(),
                            adValue = adValue.asBidonAdValue()
                        )
                    )
                }
                rewardedAd.fullScreenContentCallback = getFullScreenContentCallback.createCallback(
                    adEventFlow = this@AdmobRewardedImpl,
                    getAd = {
                        rewardedAd.asAd()
                    },
                )
                emitEvent(AdEvent.Fill(rewardedAd.asAd()))
            }
        }
        val adUnitId = when (adParams) {
            is AdmobFullscreenAdAuctionParams.Bidding -> adParams.adUnitId
            is AdmobFullscreenAdAuctionParams.Network -> adParams.adUnitId
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
            ecpm = param?.price ?: 0.0,
            demandAdObject = this,
            networkName = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = AdValue.USD,
            auctionId = auctionId,
            adUnitId = param?.adUnitId
        )
    }
}

private const val TAG = "AdmobRewarded"
