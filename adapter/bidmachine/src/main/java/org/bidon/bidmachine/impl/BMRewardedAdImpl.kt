package org.bidon.bidmachine.impl

import android.app.Activity
import android.content.Context
import io.bidmachine.AdRequest
import io.bidmachine.BidMachine
import io.bidmachine.CustomParams
import io.bidmachine.PriceFloorParams
import io.bidmachine.rewarded.RewardedAd
import io.bidmachine.rewarded.RewardedListener
import io.bidmachine.rewarded.RewardedRequest
import io.bidmachine.utils.BMError
import org.bidon.bidmachine.BMAuctionResult
import org.bidon.bidmachine.BMFullscreenAuctionParams
import org.bidon.bidmachine.asBidonErrorOnFill
import org.bidon.bidmachine.ext.asBidonAdValue
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.adapter.WinLossNotifiable
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

internal class BMRewardedAdImpl :
    AdSource.Rewarded<BMFullscreenAuctionParams>,
    Mode.Bidding,
    Mode.Network,
    AdEventFlow by AdEventFlowImpl(),
    WinLossNotifiable,
    StatisticsCollector by StatisticsCollectorImpl() {

    private var context: Context? = null
    private var adRequest: RewardedRequest? = null
    private var rewardedAd: RewardedAd? = null
    private var isBidding = false

    override val isAdReadyToShow: Boolean
        get() = rewardedAd?.canShow() == true

    override suspend fun getToken(context: Context, adTypeParam: AdTypeParam): String {
        isBidding = true
        return BidMachine.getBidToken(context)
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            BMFullscreenAuctionParams(
                price = pricefloor,
                timeout = timeout,
                context = activity.applicationContext,
                payload = json?.optString("payload")
            )
        }
    }

    override fun load(adParams: BMFullscreenAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        context = adParams.context
        val requestBuilder = RewardedRequest.Builder()
            .setPriceFloorParams(PriceFloorParams().addPriceFloor(adParams.price))
            .setCustomParams(CustomParams().addParam("mediation_mode", "bidon"))
            .setLoadingTimeOut(adParams.timeout.toInt())
            .setListener(
                object : AdRequest.AdRequestListener<RewardedRequest> {
                    override fun onRequestSuccess(
                        request: RewardedRequest,
                        result: BMAuctionResult
                    ) {
                        logInfo(TAG, "onRequestSuccess $result: $this")
                        fillAd(request)
                    }

                    override fun onRequestFailed(request: RewardedRequest, bmError: BMError) {
                        logInfo(TAG, "onRequestFailed $bmError. $this")
                        emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
                    }

                    override fun onRequestExpired(request: RewardedRequest) {
                        logInfo(TAG, "onRequestExpired: $this")
                        emitEvent(AdEvent.LoadFailed(BidonError.Expired(demandId)))
                    }
                }
            )
        adParams.payload?.let {
            requestBuilder.setBidPayload(it)
        }
        requestBuilder.build()
            .also {
                adRequest = it
            }
            .request(adParams.context)
    }

    override fun show(activity: Activity) {
        logInfo(TAG, "Starting show: $this")
        if (rewardedAd?.canShow() == true) {
            rewardedAd?.show()
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }

    override fun notifyLoss(winnerNetworkName: String, winnerNetworkPrice: Double) {
        adRequest?.notifyMediationLoss(winnerNetworkName, winnerNetworkPrice)
    }

    override fun notifyWin() {
        adRequest?.notifyMediationWin()
    }

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        adRequest?.destroy()
        adRequest = null
        rewardedAd?.destroy()
        rewardedAd = null
    }

    private fun fillAd(adRequest: RewardedRequest?) {
        logInfo(TAG, "Starting fill: $this")
        val context = context
        if (context == null) {
            emitEvent(AdEvent.LoadFailed(BidonError.NoContextFound))
        } else {
            rewardedAd = RewardedAd(context)
            val rewardedListener = object : RewardedListener {
                override fun onAdRewarded(rewardedAd: RewardedAd) {
                    logInfo(TAG, "onAdRewarded $rewardedAd: $this")
                    getAd()?.let { ad ->
                        emitEvent(
                            AdEvent.OnReward(
                                ad = ad,
                                reward = null
                            )
                        )
                    }
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    logInfo(TAG, "onAdLoaded: $this")
                    setDsp(rewardedAd.auctionResult?.demandSource)
                    if (!isBidding) {
                        setPrice(rewardedAd.auctionResult?.price ?: 0.0)
                    }
                    getAd()?.let {
                        emitEvent(AdEvent.Fill(it))
                    }
                }

                override fun onAdLoadFailed(rewardedAd: RewardedAd, bmError: BMError) {
                    val error = bmError.asBidonErrorOnFill(demandId)
                    logError(TAG, "onAdLoadFailed: $this", error)
                    getAd()?.let {
                        emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
                    }
                }

                override fun onAdShowFailed(rewardedAd: RewardedAd, bmError: BMError) {
                    val error = bmError.asBidonErrorOnFill(demandId)
                    logError(TAG, "onAdShowFailed: $this", error)
                    emitEvent(AdEvent.ShowFailed(error))
                }

                override fun onAdImpression(rewardedAd: RewardedAd) {
                    logInfo(TAG, "onAdShown: $this")
                    getAd()?.let {
                        emitEvent(AdEvent.Shown(it))
                        emitEvent(AdEvent.PaidRevenue(it, rewardedAd.auctionResult.asBidonAdValue()))
                    }
                }

                override fun onAdClicked(rewardedAd: RewardedAd) {
                    logInfo(TAG, "onAdClicked: $this")
                    getAd()?.let {
                        emitEvent(AdEvent.Clicked(it))
                    }
                }

                override fun onAdExpired(rewardedAd: RewardedAd) {
                    logInfo(TAG, "onAdExpired: $this")
                    getAd()?.let {
                        emitEvent(AdEvent.Expired(it))
                    }
                }

                override fun onAdClosed(rewardedAd: RewardedAd, boolean: Boolean) {
                    logInfo(TAG, "onAdClosed: $this")
                    getAd()?.let {
                        emitEvent(AdEvent.Closed(it))
                    }
                    this@BMRewardedAdImpl.rewardedAd = null
                    this@BMRewardedAdImpl.adRequest = null
                }
            }
            rewardedAd
                ?.setListener(rewardedListener)
                ?.load(adRequest)
        }
    }
}

private const val TAG = "BidMachineRewarded"
