package com.appodealstack.bidmachine.impl

import android.app.Activity
import android.content.Context
import com.appodealstack.bidmachine.BMAuctionResult
import com.appodealstack.bidmachine.BMFullscreenAuctionParams
import com.appodealstack.bidmachine.asBidonError
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.data.models.LineItem
import com.appodealstack.bidon.core.ext.asFailure
import com.appodealstack.bidon.core.ext.asSuccess
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInternal
import io.bidmachine.AdRequest
import io.bidmachine.PriceFloorParams
import io.bidmachine.rewarded.RewardedAd
import io.bidmachine.rewarded.RewardedListener
import io.bidmachine.rewarded.RewardedRequest
import io.bidmachine.utils.BMError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

@Suppress("unused")
internal class BMRewardedAdImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String
) : AdSource.Rewarded<BMFullscreenAuctionParams>, WinLossNotifiable {

    override val state = MutableStateFlow<AdState>(AdState.Initialized)
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
                adRequest = request
                logInternal(Tag, "RequestSuccess: $result")
                state.value = AdState.Bid(
                    AuctionResult(
                        priceFloor = result.price,
                        adSource = this@BMRewardedAdImpl,
                    )
                )
            }

            override fun onRequestFailed(request: RewardedRequest, bmError: BMError) {
                adRequest = request
                logError(Tag, "Error while bidding: $bmError")
                bmError.code
                state.value = AdState.LoadFailed(bmError.asBidonError(demandId))
            }

            override fun onRequestExpired(request: RewardedRequest) {
                adRequest = request
                state.value = AdState.LoadFailed(DemandError.Expired(demandId))
            }
        }
    }

    private val rewardedListener by lazy {
        object : RewardedListener {
            override fun onAdRewarded(rewardedAd: RewardedAd) {
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                state.value = AdState.OnReward(
                    ad = rewardedAd.asAd(),
                    reward = Reward(
                        label = "",
                        amount = 0
                    )
                )
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                state.value = AdState.Fill(rewardedAd.asAd())
            }

            override fun onAdLoadFailed(rewardedAd: RewardedAd, bmError: BMError) {
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                state.value = AdState.LoadFailed(bmError.asBidonError(demandId))
            }

            @Deprecated("Source BidMachine deprecated callback")
            override fun onAdShown(rewardedAd: RewardedAd) {
            }

            override fun onAdShowFailed(rewardedAd: RewardedAd, bmError: BMError) {
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                state.value = AdState.ShowFailed(bmError.asBidonError(demandId))
            }

            override fun onAdImpression(rewardedAd: RewardedAd) {
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                state.value = AdState.Impression(rewardedAd.asAd())
            }

            override fun onAdClicked(rewardedAd: RewardedAd) {
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                state.value = AdState.Clicked(rewardedAd.asAd())
            }

            override fun onAdExpired(rewardedAd: RewardedAd) {
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                state.value = AdState.Expired(rewardedAd.asAd())
            }

            override fun onAdClosed(rewardedAd: RewardedAd, boolean: Boolean) {
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                state.value = AdState.Closed(rewardedAd.asAd())
            }
        }
    }

    override suspend fun bid(activity: Activity?, adParams: BMFullscreenAuctionParams): Result<AuctionResult> {
        context = activity?.applicationContext
        logInternal(Tag, "Starting with $adParams")

        val context = activity?.applicationContext
        if (context == null) {
            state.value = AdState.LoadFailed(DemandError.NoActivity(demandId))
        } else {
            RewardedRequest.Builder()
                .setPriceFloorParams(PriceFloorParams().addPriceFloor(adParams.priceFloor))
                .setLoadingTimeOut(adParams.timeout.toInt())
                .setListener(requestListener)
                .setPlacementId(demandAd.placement)
                .build()
                .also {
                    adRequest = it
                }
                .request(context)
        }
        val state = state.first {
            it is AdState.Bid || it is AdState.LoadFailed
        }
        return when (state) {
            is AdState.LoadFailed -> state.cause.asFailure()
            is AdState.Bid -> state.result.asSuccess()
            else -> error("unexpected: $state")
        }
    }

    override suspend fun fill(): Result<Ad> {
        val context = context
        if (context == null) {
            state.value = AdState.LoadFailed(DemandError.NoActivity(demandId))
        } else {
            val bmRewardedAd = RewardedAd(context).also {
                rewardedAd = it
            }
            bmRewardedAd.setListener(rewardedListener)
            bmRewardedAd.load(adRequest)
        }
        val state = state.first {
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
        if (rewardedAd?.canShow() == true) {
            rewardedAd?.show()
        } else {
            state.value = AdState.ShowFailed(BidonError.FullscreenAdNotReady)
        }
    }

    override fun notifyLoss() {
        adRequest?.notifyMediationLoss()
    }

    override fun notifyWin() {
        adRequest?.notifyMediationWin()
    }

    override fun getAuctionParams(priceFloor: Double, timeout: Long, lineItems: List<LineItem>): AdAuctionParams {
        return BMFullscreenAuctionParams(priceFloor = priceFloor, timeout = timeout)
    }

    override fun destroy() {
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
            monetizationNetwork = this.auctionResult?.demandSource,
        )
    }
}

private const val Tag = "BidMachine Rewarded"
