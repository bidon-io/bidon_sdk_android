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
import org.bidon.bidmachine.asBidonErrorOnBid
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
import org.bidon.sdk.ads.Ad
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

    override val isAdReadyToShow: Boolean
        get() = rewardedAd?.canShow() == true

    override suspend fun getToken(context: Context): String {
        return BidMachine.getBidToken(context)
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
                        val error = bmError.asBidonErrorOnBid(demandId)
                        logError(TAG, "onRequestFailed $bmError. $this", error)
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

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        adRequest?.destroy()
        adRequest = null
        rewardedAd?.destroy()
        rewardedAd = null
    }

    override fun notifyLoss(winnerNetworkName: String, winnerNetworkPrice: Double) {
        adRequest?.notifyMediationLoss(winnerNetworkName, winnerNetworkPrice)
    }

    override fun notifyWin() {
        adRequest?.notifyMediationWin()
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
                    emitEvent(
                        AdEvent.OnReward(
                            ad = rewardedAd.asAd(),
                            reward = null
                        )
                    )
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    logInfo(TAG, "onAdLoaded: $this")
                    emitEvent(AdEvent.Fill(rewardedAd.asAd()))
                }

                override fun onAdLoadFailed(rewardedAd: RewardedAd, bmError: BMError) {
                    val error = bmError.asBidonErrorOnFill(demandId)
                    logError(TAG, "onAdLoadFailed: $this", error)
                    emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
                }

                override fun onAdShowFailed(rewardedAd: RewardedAd, bmError: BMError) {
                    val error = bmError.asBidonErrorOnFill(demandId)
                    logError(TAG, "onAdShowFailed: $this", error)
                    emitEvent(AdEvent.ShowFailed(error))
                }

                override fun onAdImpression(rewardedAd: RewardedAd) {
                    logInfo(TAG, "onAdShown: $this")
                    emitEvent(AdEvent.Shown(rewardedAd.asAd()))
                    emitEvent(
                        AdEvent.PaidRevenue(
                            ad = rewardedAd.asAd(),
                            adValue = rewardedAd.auctionResult.asBidonAdValue()
                        )
                    )
                }

                override fun onAdClicked(rewardedAd: RewardedAd) {
                    logInfo(TAG, "onAdClicked: $this")
                    emitEvent(AdEvent.Clicked(rewardedAd.asAd()))
                }

                override fun onAdExpired(rewardedAd: RewardedAd) {
                    logInfo(TAG, "onAdExpired: $this")
                    emitEvent(AdEvent.Expired(rewardedAd.asAd()))
                }

                override fun onAdClosed(rewardedAd: RewardedAd, boolean: Boolean) {
                    logInfo(TAG, "onAdClosed: $this")
                    emitEvent(AdEvent.Closed(rewardedAd.asAd()))
                }
            }
            rewardedAd
                ?.setListener(rewardedListener)
                ?.load(adRequest)
        }
    }

    private fun RewardedAd.asAd(): Ad {
        return Ad(
            demandAd = demandAd,
            ecpm = this.auctionResult?.price ?: 0.0,
            demandAdObject = this,
            currencyCode = "USD",
            roundId = roundId,
            dsp = this.auctionResult?.demandSource,
            networkName = demandId.demandId,
            auctionId = auctionId,
            adUnitId = null
        )
    }
}

private const val TAG = "BidMachineRewarded"
