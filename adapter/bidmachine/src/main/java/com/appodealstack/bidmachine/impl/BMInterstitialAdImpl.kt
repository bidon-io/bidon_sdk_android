package com.appodealstack.bidmachine.impl

import android.app.Activity
import android.content.Context
import com.appodealstack.bidmachine.BMAuctionResult
import com.appodealstack.bidmachine.BMFullscreenParams
import com.appodealstack.bidmachine.asBidonError
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.adapters.AdSource.Interstitial.State
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.data.models.LineItem
import com.appodealstack.bidon.core.ext.asFailure
import com.appodealstack.bidon.core.ext.asSuccess
import com.appodealstack.bidon.core.ext.logError
import io.bidmachine.AdContentType
import io.bidmachine.AdRequest
import io.bidmachine.PriceFloorParams
import io.bidmachine.interstitial.InterstitialAd
import io.bidmachine.interstitial.InterstitialListener
import io.bidmachine.interstitial.InterstitialRequest
import io.bidmachine.utils.BMError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

internal class BMInterstitialAdImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String
) : AdSource.Interstitial<BMFullscreenParams> {

    override val state = MutableStateFlow<State>(State.Initialized)
    override val ad: Ad? get() = interstitialAd?.asAd()

    private var context: Context? = null
    private var adRequest: InterstitialRequest? = null
    private var interstitialAd: InterstitialAd? = null

    private val requestListener by lazy {
        object : AdRequest.AdRequestListener<InterstitialRequest> {
            override fun onRequestSuccess(
                request: InterstitialRequest,
                result: BMAuctionResult
            ) {
                adRequest = request
                state.value = State.Bid.Success(
                    AuctionResult(
                        priceFloor = result.price,
                        adSource = this@BMInterstitialAdImpl,
                    )
                )
            }

            override fun onRequestFailed(request: InterstitialRequest, bmError: BMError) {
                adRequest = request
                logError(Tag, "Error while bidding: $bmError")
                state.value = State.Bid.Failure(bmError.asBidonError(demandId))
            }

            override fun onRequestExpired(request: InterstitialRequest) {
                adRequest = request
                state.value = State.Bid.Failure(DemandError.Expired(demandId))
            }
        }
    }

    private val interstitialListener by lazy {
        object : InterstitialListener {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                state.value = State.Fill.Success(interstitialAd.asAd())
            }

            override fun onAdLoadFailed(interstitialAd: InterstitialAd, bmError: BMError) {
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                state.value = State.Fill.Failure(bmError.asBidonError(demandId))
            }

            @Deprecated("Source BidMachine deprecated callback")
            override fun onAdShown(interstitialAd: InterstitialAd) {
            }

            override fun onAdShowFailed(interstitialAd: InterstitialAd, bmError: BMError) {
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                state.value = State.Show.ShowFailed(bmError.asBidonError(demandId))
            }

            override fun onAdImpression(interstitialAd: InterstitialAd) {
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                state.value = State.Show.Impression(interstitialAd.asAd())
            }

            override fun onAdClicked(interstitialAd: InterstitialAd) {
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                state.value = State.Show.Clicked(interstitialAd.asAd())
            }

            override fun onAdExpired(interstitialAd: InterstitialAd) {
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                state.value = State.Expired(interstitialAd.asAd())
            }

            override fun onAdClosed(interstitialAd: InterstitialAd, boolean: Boolean) {
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                state.value = State.Show.Closed(interstitialAd.asAd())
            }
        }
    }

    override suspend fun bid(activity: Activity?, adParams: BMFullscreenParams): Result<State.Bid.Success> {
        context = activity?.applicationContext
        state.value = State.Bid.Requesting

        val context = activity?.applicationContext
        if (context == null) {
            state.value = State.Bid.Failure(DemandError.NoActivity(demandId))
        } else {
            InterstitialRequest.Builder()
                .setAdContentType(AdContentType.All)
                .setPriceFloorParams(PriceFloorParams().addPriceFloor(adParams.priceFloor))
                .setLoadingTimeOut(adParams.timeout.toInt())
                .setListener(requestListener)
                .apply {
                    demandAd.placement?.let { setPlacementId(it) }
                }.build()
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
            is State.Bid.Success -> state.asSuccess()
            State.Bid.Requesting -> error("unexpected: $state")
        }
    }

    override suspend fun fill(): Result<State.Fill.Success> {
        state.value = State.Fill.LoadingResources
        val context = context
        if (context == null) {
            state.value = State.Fill.Failure(DemandError.NoActivity(demandId))
        } else {
            val bmInterstitialAd = InterstitialAd(context).also {
                interstitialAd = it
            }
            bmInterstitialAd.setListener(interstitialListener)
            bmInterstitialAd.load(adRequest)
        }
        val state = state.first {
            it is State.Fill.Success || it is State.Fill.Failure || it is State.Expired
        }
        return when (state) {
            is State.Fill.Success -> state.asSuccess()
            is State.Fill.Failure -> state.cause.asFailure()
            is State.Expired -> BidonError.FillTimedOut(demandId).asFailure()
            else -> error("unexpected: $state")
        }
    }

    override fun show(activity: Activity) {
        if (interstitialAd?.canShow() == true) {
            interstitialAd?.show()
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
        interstitialAd?.destroy()
        interstitialAd = null
    }

    private fun InterstitialAd.asAd(): Ad {
        return Ad(
            demandId = demandId,
            demandAd = demandAd,
            price = this.auctionResult?.price ?: 0.0,
            sourceAd = this,
            currencyCode = null,
            roundId = roundId,
            dsp = this.auctionResult?.demandSource,
            monetizationNetwork = null
        )
    }
}

private const val Tag = "BidMachine Interstitial"