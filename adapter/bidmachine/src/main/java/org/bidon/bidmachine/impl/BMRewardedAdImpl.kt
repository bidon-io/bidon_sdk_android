package org.bidon.bidmachine.impl

import android.app.Activity
import android.content.Context
import io.bidmachine.AdRequest
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
import org.bidon.sdk.adapter.WinLossNotifiable
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.BidType

internal class BMRewardedAdImpl(
    private val obtainAdAuctionParams: GetAdAuctionParamUseCase = GetAdAuctionParamUseCase()
) :
    AdSource.Rewarded<BMFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    WinLossNotifiable,
    StatisticsCollector by StatisticsCollectorImpl() {

    private var context: Context? = null
    private var adRequest: RewardedRequest? = null
    private var rewardedAd: RewardedAd? = null

    override val isAdReadyToShow: Boolean
        get() = rewardedAd?.canShow() == true

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return obtainAdAuctionParams.getBMFullscreenAuctionParams(auctionParamsScope)
    }

    override fun load(adParams: BMFullscreenAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        context = adParams.context
        val bidType = adParams.adUnit.bidType
        val requestBuilder = RewardedRequest.Builder()
            .apply {
                when (bidType) {
                    BidType.CPM -> {
                        setNetworks("")
                        setPlacementId(adParams.placement)
                    }

                    BidType.RTB -> {
                        val payload = adParams.payload
                        if (payload == null) {
                            val error = BidonError.IncorrectAdUnit(
                                demandId = demandId,
                                message = "Payload is null for RTB"
                            )
                            emitEvent(AdEvent.LoadFailed(error))
                            return
                        }
                        setBidPayload(payload)
                    }
                }
            }
            .setPriceFloorParams(PriceFloorParams().addPriceFloor(adParams.price))
            .setCustomParams(adParams.customParameters)
            .setLoadingTimeOut(adParams.timeout.toInt())
            .setListener(
                object : AdRequest.AdRequestListener<RewardedRequest> {
                    override fun onRequestSuccess(
                        request: RewardedRequest,
                        result: BMAuctionResult
                    ) {
                        logInfo(TAG, "onRequestSuccess $result: $this")
                        fillAd(request, bidType)
                    }

                    override fun onRequestFailed(request: RewardedRequest, bmError: BMError) {
                        val error = if (bidType == BidType.RTB) {
                            bmError.asBidonErrorOnBid(demandId)
                        } else {
                            bmError.asBidonErrorOnFill(demandId)
                        }
                        logError(TAG, "onRequestFailed $bmError. $this", error)
                        emitEvent(AdEvent.LoadFailed(error))
                    }

                    override fun onRequestExpired(request: RewardedRequest) {
                        logInfo(TAG, "onRequestExpired: $this")
                        emitEvent(AdEvent.LoadFailed(BidonError.Expired(demandId)))
                    }
                }
            )
        requestBuilder.build()
            .also { adRequest = it }
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
        logInfo(TAG, "notifyLoss: $this")
        adRequest?.notifyMediationLoss(winnerNetworkName, winnerNetworkPrice)
    }

    override fun notifyWin() {
        logInfo(TAG, "notifyWin: $this")
        adRequest?.notifyMediationWin()
    }

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        adRequest?.destroy()
        adRequest = null
        rewardedAd?.destroy()
        rewardedAd = null
    }

    private fun fillAd(adRequest: RewardedRequest?, bidType: BidType) {
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
                    if (bidType == BidType.CPM) {
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
                        emitEvent(AdEvent.LoadFailed(error))
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
