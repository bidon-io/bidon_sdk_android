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
import kotlinx.coroutines.flow.MutableSharedFlow
import org.bidon.bidmachine.BMAuctionResult
import org.bidon.bidmachine.BMFullscreenAuctionParams
import org.bidon.bidmachine.asBidonErrorOnBid
import org.bidon.bidmachine.asBidonErrorOnFill
import org.bidon.bidmachine.ext.asBidonAdValue
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdLoadingType
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.WinLossNotifiable
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus

internal class BMRewardedAdImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val auctionId: String
) : AdSource.Rewarded<BMFullscreenAuctionParams>,
    AdLoadingType.Bidding<BMFullscreenAuctionParams>,
    AdLoadingType.Network<BMFullscreenAuctionParams>,
    WinLossNotifiable,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId,
        demandAd = demandAd,
    ) {

    override val adEvent =
        MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE, replay = 1)
    override val ad: Ad? get() = rewardedAd?.asAd()

    private var context: Context? = null
    private var adRequest: RewardedRequest? = null
    private var rewardedAd: RewardedAd? = null
    override val isAdReadyToShow: Boolean
        get() = rewardedAd?.canShow() == true

    private var isBiddingRequest = true
    private val requestListener by lazy {
        object : AdRequest.AdRequestListener<RewardedRequest> {
            override fun onRequestSuccess(
                request: RewardedRequest,
                result: BMAuctionResult
            ) {
                logInfo(Tag, "onRequestSuccess $result: $this")
                adRequest = request
                when (isBiddingRequest) {
                    false -> {
                        fillRequest(request, rewardedListener)
                    }

                    true -> {
                        adEvent.tryEmit(
                            AdEvent.Bid(
                                AuctionResult.Network.Success(
                                    adSource = this@BMRewardedAdImpl,
                                    roundStatus = RoundStatus.Successful
                                )
                            )
                        )
                    }
                }
            }

            override fun onRequestFailed(request: RewardedRequest, bmError: BMError) {
                val error = bmError.asBidonErrorOnBid(demandId)
                logError(Tag, "onRequestFailed $bmError. $this", error)
                adRequest = request
                adEvent.tryEmit(AdEvent.LoadFailed(error))
            }

            override fun onRequestExpired(request: RewardedRequest) {
                logInfo(Tag, "onRequestExpired: $this")
                adRequest = request
                adEvent.tryEmit(AdEvent.LoadFailed(BidonError.Expired(demandId)))
            }
        }
    }

    private val rewardedListener by lazy {
        object : RewardedListener {
            override fun onAdRewarded(rewardedAd: RewardedAd) {
                logInfo(Tag, "onAdRewarded $rewardedAd: $this")
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                adEvent.tryEmit(
                    AdEvent.OnReward(
                        ad = rewardedAd.asAd(),
                        reward = null
                    )
                )
                sendRewardImpression()
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                logInfo(Tag, "onAdLoaded: $this")
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                adEvent.tryEmit(AdEvent.Fill(rewardedAd.asAd()))
            }

            override fun onAdLoadFailed(rewardedAd: RewardedAd, bmError: BMError) {
                val error = bmError.asBidonErrorOnFill(demandId)
                logError(Tag, "onAdLoadFailed: $this", error)
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                adEvent.tryEmit(AdEvent.LoadFailed(error))
            }

            override fun onAdShowFailed(rewardedAd: RewardedAd, bmError: BMError) {
                val error = bmError.asBidonErrorOnFill(demandId)
                logError(Tag, "onAdShowFailed: $this", error)
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                adEvent.tryEmit(AdEvent.ShowFailed(error))
            }

            override fun onAdImpression(rewardedAd: RewardedAd) {
                logInfo(Tag, "onAdShown: $this")
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                adEvent.tryEmit(AdEvent.Shown(rewardedAd.asAd()))
                adEvent.tryEmit(
                    AdEvent.PaidRevenue(
                        ad = rewardedAd.asAd(),
                        adValue = rewardedAd.auctionResult.asBidonAdValue()
                    )
                )
            }

            override fun onAdClicked(rewardedAd: RewardedAd) {
                logInfo(Tag, "onAdClicked: $this")
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                adEvent.tryEmit(AdEvent.Clicked(rewardedAd.asAd()))
            }

            override fun onAdExpired(rewardedAd: RewardedAd) {
                logInfo(Tag, "onAdExpired: $this")
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                adEvent.tryEmit(AdEvent.Expired(rewardedAd.asAd()))
            }

            override fun onAdClosed(rewardedAd: RewardedAd, boolean: Boolean) {
                logInfo(Tag, "onAdClosed: $this")
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                adEvent.tryEmit(AdEvent.Closed(rewardedAd.asAd()))
            }
        }
    }

    override fun getToken(context: Context): String = BidMachine.getBidToken(context)

    override fun adRequest(adParams: BMFullscreenAuctionParams) {
        isBiddingRequest = true
        request(adParams, requestListener)
    }

    /**
     * As Bidding Network
     */
    override fun fill() {
        isBiddingRequest = true
        fillRequest(adRequest, rewardedListener)
    }

    /**
     * As AdNetwork
     */
    override fun fill(adParams: BMFullscreenAuctionParams) {
        isBiddingRequest = false
        request(adParams, requestListener)
    }

    override fun show(activity: Activity) {
        logInfo(Tag, "Starting show: $this")
        if (rewardedAd?.canShow() == true) {
            rewardedAd?.show()
        } else {
            adEvent.tryEmit(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        }
    }

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            BMFullscreenAuctionParams(
                pricefloor = pricefloor,
                timeout = timeout,
                context = activity.applicationContext,
                payload = payload
            )
        }
    }

    override fun destroy() {
        logInfo(Tag, "destroy $this")
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

    private fun request(adParams: BMFullscreenAuctionParams, requestListener: AdRequest.AdRequestListener<RewardedRequest>) {
        logInfo(Tag, "Starting with $adParams: $this")
        context = adParams.context
        val requestBuilder = RewardedRequest.Builder()
            .setPriceFloorParams(PriceFloorParams().addPriceFloor(adParams.pricefloor))
            .setCustomParams(CustomParams().addParam("mediation_mode", "bidon"))
            .setLoadingTimeOut(adParams.timeout.toInt())
            .setListener(requestListener)
        adParams.payload?.let {
            requestBuilder.setBidPayload(it)
        }
        requestBuilder.build()
            .also {
                adRequest = it
            }
            .request(adParams.context)
    }

    private fun fillRequest(adRequest: RewardedRequest?, listener: RewardedListener) {
        logInfo(Tag, "Starting fill: $this")
        val context = context
        if (context == null) {
            adEvent.tryEmit(AdEvent.LoadFailed(BidonError.NoContextFound))
        } else {
            RewardedAd(context)
                .also {
                    rewardedAd = it
                }
                .setListener(listener)
                .load(adRequest)
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

private const val Tag = "BidMachineRewarded"
