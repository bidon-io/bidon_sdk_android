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
import org.bidon.sdk.ads.rewarded.Reward
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.config.BidonError
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

    private var rewardedAd: RewardedAd? = null
    private var isBiddingMode: Boolean = false
    private var price: Double? = null

    override val isAdReadyToShow: Boolean
        get() = rewardedAd != null

    override suspend fun getToken(context: Context, adTypeParam: AdTypeParam): String? {
        isBiddingMode = true
        return obtainToken(context, demandAd.adType)
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return obtainAdAuctionParams(auctionParamsScope, demandAd.adType, isBiddingMode)
    }

    override fun load(adParams: AdmobFullscreenAdAuctionParams) {
        logInfo(TAG, "Starting with $adParams")
        val adRequest = getAdRequest(adParams)
        price = adParams.price
        val requestListener = object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                logError(TAG, "onAdFailedToLoad: $loadAdError. $this", loadAdError.asBidonError())
                emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                logInfo(TAG, "onAdLoaded. RewardedAd=$rewardedAd, $this")
                this@AdmobRewardedImpl.rewardedAd = rewardedAd
                adParams.activity.runOnUiThread {
                    rewardedAd.onPaidEventListener = OnPaidEventListener { adValue ->
                        getAd()?.let {
                            emitEvent(
                                AdEvent.PaidRevenue(
                                    ad = it,
                                    adValue = adValue.asBidonAdValue()
                                )
                            )
                        }
                    }
                    rewardedAd.fullScreenContentCallback = getFullScreenContentCallback.createCallback(
                        adEventFlow = this@AdmobRewardedImpl,
                        getAd = {
                            getAd()
                        },
                    )
                    getAd()?.let { emitEvent(AdEvent.Fill(it)) }
                }
            }
        }
        val adUnitId = when (adParams) {
            is AdmobFullscreenAdAuctionParams.Bidding -> adParams.adUnitId
            is AdmobFullscreenAdAuctionParams.Network -> adParams.adUnitId
        }
        RewardedAd.load(adParams.activity, adUnitId, adRequest, requestListener)
    }

    override fun show(activity: Activity) {
        logInfo(TAG, "Starting show: $this")
        val rewardedAd = rewardedAd
        if (rewardedAd == null) {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        } else {
            rewardedAd.show(activity) { rewardItem ->
                logInfo(TAG, "onUserEarnedReward $rewardItem: $this")
                getAd()?.let {
                    emitEvent(AdEvent.OnReward(it, Reward(rewardItem.type, rewardItem.amount)))
                }
            }
        }
    }

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        rewardedAd?.onPaidEventListener = null
        rewardedAd?.fullScreenContentCallback = null
        rewardedAd = null
    }
}

private const val TAG = "AdmobRewarded"
