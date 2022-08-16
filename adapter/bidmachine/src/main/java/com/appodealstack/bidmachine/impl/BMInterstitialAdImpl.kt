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
    private val state = MutableStateFlow<State>(State.Initialized)
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
                state.value = State.Bid(
                    AuctionResult(
                        priceFloor = result.price,
                        adSource = this@BMInterstitialAdImpl
                    )
                )
            }

            override fun onRequestFailed(request: InterstitialRequest, bmError: BMError) {
                adRequest = request
                state.value = State.Failed(bmError.asBidonError(demandId))
            }

            override fun onRequestExpired(request: InterstitialRequest) {
                adRequest = request
                state.value = State.Failed(DemandError.Expired(demandId))
            }
        }
    }

    private val interstitialListener by lazy {
        object : InterstitialListener {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                state.value = State.Fill(interstitialAd.asAd())
            }

            override fun onAdLoadFailed(interstitialAd: InterstitialAd, bmError: BMError) {
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                state.value = State.Failed(bmError.asBidonError(demandId))
            }

            @Deprecated("Source BidMachine deprecated callback")
            override fun onAdShown(interstitialAd: InterstitialAd) {
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                state.value = State.Shown(interstitialAd.asAd())
            }

            override fun onAdImpression(interstitialAd: InterstitialAd) {
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                state.value = State.Impression(interstitialAd.asAd())
            }

            override fun onAdClicked(interstitialAd: InterstitialAd) {
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                state.value = State.Clicked(interstitialAd.asAd())
            }

            override fun onAdExpired(interstitialAd: InterstitialAd) {
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                state.value = State.Failed(DemandError.Expired(demandId))
            }

            override fun onAdShowFailed(interstitialAd: InterstitialAd, bmError: BMError) {
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                state.value = State.Failed(bmError.asBidonError(demandId))
            }

            override fun onAdClosed(interstitialAd: InterstitialAd, boolean: Boolean) {
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                state.value = State.Closed(interstitialAd.asAd())
            }
        }
    }

    override suspend fun bid(activity: Activity?, adParams: BMFullscreenParams): Result<State.Bid> {
        context = activity?.applicationContext
        state.value = State.Requesting

        val context = activity?.applicationContext
        if (context == null) {
            state.value = State.Failed(DemandError.NoActivity(demandId))
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
        val bidResult = state.first {
            it is State.Bid || it is State.Failed
        }
        return when (bidResult) {
            is State.Bid -> bidResult.asSuccess()
            is State.Failed -> bidResult.cause.asFailure()
            else -> error("Unexpected")
        }
    }

    override suspend fun fill(): Result<State.Fill> {
        state.value = State.LoadingResources

        val context = context ?: return DemandError.NoActivity(demandId).asFailure()
        val bmInterstitialAd = InterstitialAd(context).also {
            interstitialAd = it
        }
        bmInterstitialAd.setListener(interstitialListener)
        bmInterstitialAd.load(adRequest)

        val bidResult = state.first {
            it is State.Fill || it is State.Failed
        }
        return when (bidResult) {
            is State.Fill -> bidResult.asSuccess()
            is State.Failed -> bidResult.cause.asFailure()
            else -> error("Unexpected")
        }
    }

    override fun show(activity: Activity) {
        val bmInterstitialAd = ((state.value as? State.Fill)?.ad?.sourceAd as? InterstitialAd)
        if (bmInterstitialAd?.canShow() == true) {
            bmInterstitialAd.show()
        } else {
            state.value = State.Failed(DemandError.FullscreenAdNotReady(demandId))
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