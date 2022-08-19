package com.appodealstack.bidmachine.impl

import android.app.Activity
import android.content.Context
import com.appodealstack.bidmachine.BMAuctionResult
import com.appodealstack.bidmachine.BMFullscreenParams
import com.appodealstack.bidmachine.asBidonError
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.adapters.AdSource.Rewarded.OnReward
import com.appodealstack.bidon.adapters.AdSource.State
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
) : AdSource.Rewarded<BMFullscreenParams>, WinLossNotifiable {

    override val state = MutableStateFlow<State>(State.Initialized)
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
                state.value = State.Bid.Success(
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
                state.value = State.Bid.Failure(bmError.asBidonError(demandId))
            }

            override fun onRequestExpired(request: RewardedRequest) {
                adRequest = request
                state.value = State.Bid.Failure(DemandError.Expired(demandId))
            }
        }
    }

    private val interstitialListener by lazy {
        object : RewardedListener {
            override fun onAdRewarded(rewardedAd: RewardedAd) {
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                state.value = OnReward.Success(
                    ad = rewardedAd.asAd(),
                    reward = Reward(
                        label = "",
                        amount = 0
                    )
                )
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                state.value = State.Fill.Success(rewardedAd.asAd())
            }

            override fun onAdLoadFailed(rewardedAd: RewardedAd, bmError: BMError) {
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                state.value = State.Fill.Failure(bmError.asBidonError(demandId))
            }

            @Deprecated("Source BidMachine deprecated callback")
            override fun onAdShown(rewardedAd: RewardedAd) {
            }

            override fun onAdShowFailed(rewardedAd: RewardedAd, bmError: BMError) {
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                state.value = State.Show.ShowFailed(bmError.asBidonError(demandId))
            }

            override fun onAdImpression(rewardedAd: RewardedAd) {
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                state.value = State.Show.Impression(rewardedAd.asAd())
            }

            override fun onAdClicked(rewardedAd: RewardedAd) {
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                state.value = State.Show.Clicked(rewardedAd.asAd())
            }

            override fun onAdExpired(rewardedAd: RewardedAd) {
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                state.value = State.Expired(rewardedAd.asAd())
            }

            override fun onAdClosed(rewardedAd: RewardedAd, boolean: Boolean) {
                this@BMRewardedAdImpl.rewardedAd = rewardedAd
                state.value = State.Show.Closed(rewardedAd.asAd())
            }
        }
    }

    override suspend fun bid(activity: Activity?, adParams: BMFullscreenParams): Result<AuctionResult> {
        context = activity?.applicationContext
        state.value = State.Bid.Requesting
        logInternal(Tag, "Starting with $adParams")

        val context = activity?.applicationContext
        if (context == null) {
            state.value = State.Bid.Failure(DemandError.NoActivity(demandId))
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
            it is State.Bid.Success || it is State.Bid.Failure
        } as State.Bid
        return when (state) {
            is State.Bid.Failure -> state.cause.asFailure()
            is State.Bid.Success -> state.result.asSuccess()
            State.Bid.Requesting -> error("unexpected: $state")
        }
    }

    override suspend fun fill(): Result<Ad> {
        state.value = State.Fill.LoadingResources
        val context = context
        if (context == null) {
            state.value = State.Fill.Failure(DemandError.NoActivity(demandId))
        } else {
            val bmInterstitialAd = RewardedAd(context).also {
                rewardedAd = it
            }
            bmInterstitialAd.setListener(interstitialListener)
            bmInterstitialAd.load(adRequest)
        }
        val state = state.first {
            it is State.Fill.Success || it is State.Fill.Failure || it is State.Expired
        }
        return when (state) {
            is State.Fill.Success -> state.ad.asSuccess()
            is State.Fill.Failure -> state.cause.asFailure()
            is State.Expired -> BidonError.FillTimedOut(demandId).asFailure()
            else -> error("unexpected: $state")
        }
    }

    override fun show(activity: Activity) {
        if (rewardedAd?.canShow() == true) {
            rewardedAd?.show()
        } else {
            state.value = State.Show.ShowFailed(BidonError.FullscreenAdNotReady)
        }
    }

    override fun notifyLoss() {
        adRequest?.notifyMediationLoss()
    }

    override fun notifyWin() {
        adRequest?.notifyMediationWin()
    }

    override fun getParams(priceFloor: Double, timeout: Long, lineItems: List<LineItem>): AdSource.AdParams {
        return BMFullscreenParams(priceFloor = priceFloor, timeout = timeout)
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

private const val Tag = "BidMachine Interstitial"