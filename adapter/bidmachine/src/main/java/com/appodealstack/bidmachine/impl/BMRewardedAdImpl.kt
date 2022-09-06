package com.appodealstack.bidmachine.impl

import android.app.Activity
import android.content.Context
import com.appodealstack.bidmachine.BMAuctionResult
import com.appodealstack.bidmachine.BMFullscreenAuctionParams
import com.appodealstack.bidmachine.asBidonError
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.analytics.BidStatsProvider
import com.appodealstack.bidon.analytics.data.models.RoundStatus
import com.appodealstack.bidon.analytics.data.models.asRoundStatus
import com.appodealstack.bidon.analytics.domain.BidStatsProviderImpl
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.data.models.LineItem
import com.appodealstack.bidon.core.ext.asFailure
import com.appodealstack.bidon.core.ext.asSuccess
import com.appodealstack.bidon.core.ext.logInternal
import io.bidmachine.AdRequest
import io.bidmachine.PriceFloorParams
import io.bidmachine.rewarded.RewardedAd
import io.bidmachine.rewarded.RewardedListener
import io.bidmachine.rewarded.RewardedRequest
import io.bidmachine.utils.BMError
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

internal class BMRewardedAdImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val auctionId: String
) : AdSource.Rewarded<BMFullscreenAuctionParams>,
    WinLossNotifiable,
    BidStatsProvider by BidStatsProviderImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId
    ) {

    override val adState = MutableSharedFlow<AdState>(extraBufferCapacity = Int.MAX_VALUE)
    override val ad: Ad? get() = rewardedAd?.asAd()

    private var context: Context? = null
    private var adRequest: RewardedRequest? = null
    private var rewardedAd: RewardedAd? = null

    private val requestListener by lazy {
        object : AdRequest.AdRequestListener<RewardedRequest> {
            override fun onRequestSuccess(
                request: RewardedRequest,
                result: BMAuctionResult
            ) {
                logInternal(Tag, "onRequestSuccess $result: $this")
                adRequest = request
                onBidFinished(
                    ecpm = result.price,
                    roundStatus = RoundStatus.SuccessfulBid,
                )
                adState.tryEmit(
                    AdState.Bid(
                        AuctionResult(
                            ecpm = result.price,
                            adSource = this@BMRewardedAdImpl,
                        )
                    )
                )
            }

            override fun onRequestFailed(request: RewardedRequest, bmError: BMError) {
                logInternal(Tag, "onRequestFailed $bmError. $this", bmError.asBidonError(demandId))
                adRequest = request
                onBidFinished(
                    ecpm = null,
                    roundStatus = bmError.asBidonError(demandId).asRoundStatus(),
                )
                adState.tryEmit(AdState.LoadFailed(bmError.asBidonError(demandId)))
            }

            override fun onRequestExpired(request: RewardedRequest) {
                logInternal(Tag, "onRequestExpired: $this")
                adRequest = request
                onBidFinished(
                    ecpm = null,
                    roundStatus = RoundStatus.NoBid,
                )
                adState.tryEmit(AdState.LoadFailed(DemandError.Expired(demandId)))
            }
        }
    }

    private val rewardedListener by lazy {
        object : RewardedListener {
            override fun onAdRewarded(rewardedAd: RewardedAd) {
                logInternal(Tag, "onAdRewarded $rewardedAd: $this")
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                adState.tryEmit(
                    AdState.OnReward(
                        ad = rewardedAd.asAd(),
                        reward = null
                    )
                )
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                logInternal(Tag, "onAdLoaded: $this")
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                adState.tryEmit(AdState.Fill(rewardedAd.asAd()))
            }

            override fun onAdLoadFailed(rewardedAd: RewardedAd, bmError: BMError) {
                logInternal(Tag, "onAdLoadFailed: $this", bmError.asBidonError(demandId))
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                adState.tryEmit(AdState.LoadFailed(bmError.asBidonError(demandId)))
            }

            @Deprecated("Source BidMachine deprecated callback")
            override fun onAdShown(rewardedAd: RewardedAd) {
            }

            override fun onAdShowFailed(rewardedAd: RewardedAd, bmError: BMError) {
                logInternal(Tag, "onAdShowFailed: $this", bmError.asBidonError(demandId))
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                adState.tryEmit(AdState.ShowFailed(bmError.asBidonError(demandId)))
            }

            override fun onAdImpression(rewardedAd: RewardedAd) {
                logInternal(Tag, "onAdImpression: $this")
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                adState.tryEmit(AdState.Impression(rewardedAd.asAd()))
            }

            override fun onAdClicked(rewardedAd: RewardedAd) {
                logInternal(Tag, "onAdClicked: $this")
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                adState.tryEmit(AdState.Clicked(rewardedAd.asAd()))
            }

            override fun onAdExpired(rewardedAd: RewardedAd) {
                logInternal(Tag, "onAdExpired: $this")
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                adState.tryEmit(AdState.Expired(rewardedAd.asAd()))
            }

            override fun onAdClosed(rewardedAd: RewardedAd, boolean: Boolean) {
                logInternal(Tag, "onAdClosed: $this")
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                adState.tryEmit(AdState.Closed(rewardedAd.asAd()))
            }
        }
    }

    override suspend fun bid(adParams: BMFullscreenAuctionParams): AuctionResult {
        logInternal(Tag, "Starting with $adParams: $this")
        onBidStarted()
        this.context = adParams.context
        RewardedRequest.Builder()
            .setPriceFloorParams(PriceFloorParams().addPriceFloor(adParams.priceFloor))
            .setLoadingTimeOut(adParams.timeout.toInt())
            .setListener(requestListener)
            .setPlacementId(demandAd.placement)
            .build()
            .also {
                adRequest = it
            }
            .request(adParams.context)
        val state = adState.first {
            it is AdState.Bid || it is AdState.LoadFailed
        }
        return when (state) {
            is AdState.LoadFailed -> {
                AuctionResult(
                    ecpm = 0.0,
                    adSource = this
                )
            }
            is AdState.Bid -> state.result
            else -> error("unexpected: $state")
        }
    }

    override suspend fun fill(): Result<Ad> {
        logInternal(Tag, "Starting fill: $this")
        val context = context
        if (context == null) {
            adState.tryEmit(AdState.LoadFailed(DemandError.NoActivity(demandId)))
        } else {
            val bmRewardedAd = RewardedAd(context).also {
                rewardedAd = it
            }
            bmRewardedAd.setListener(rewardedListener)
            bmRewardedAd.load(adRequest)
        }
        val state = adState.first {
            it is AdState.Fill || it is AdState.LoadFailed || it is AdState.Expired
        }
        return when (state) {
            is AdState.Fill -> state.ad.asSuccess()
            is AdState.LoadFailed -> state.cause.asFailure()
            is AdState.Expired -> BidonError.FillTimedOut(demandId).asFailure()
            else -> error("unexpected: $state")
        }
    }

    override fun show(activity: Activity) {
        logInternal(Tag, "Starting show: $this")
        if (rewardedAd?.canShow() == true) {
            rewardedAd?.show()
        } else {
            adState.tryEmit(AdState.ShowFailed(BidonError.FullscreenAdNotReady))
        }
    }

    override fun notifyLoss() {
        adRequest?.notifyMediationLoss()
    }

    override fun notifyWin() {
        adRequest?.notifyMediationWin()
    }

    override fun getAuctionParams(
        activity: Activity,
        priceFloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        onLineItemConsumed: (LineItem) -> Unit,
    ): Result<AdAuctionParams> = runCatching {
        BMFullscreenAuctionParams(priceFloor = priceFloor, timeout = timeout, context = activity.applicationContext)
    }

    override fun destroy() {
        logInternal(Tag, "destroy $this")
        adRequest?.destroy()
        adRequest = null
        rewardedAd?.destroy()
        rewardedAd = null
    }

    private fun RewardedAd.asAd(): Ad {
        return Ad(
            demandId = demandId,
            demandAd = demandAd,
            price = this.auctionResult?.price ?: 0.0,
            sourceAd = this,
            currencyCode = "USD",
            roundId = roundId,
            dsp = this.auctionResult?.demandSource,
            monetizationNetwork = demandId.demandId,
            auctionId = auctionId,
        )
    }
}

private const val Tag = "BidMachine Rewarded"
