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
) : AdSource.Interstitial<BMFullscreenAuctionParams>, WinLossNotifiable {

    override val state = MutableStateFlow<AdState>(AdState.Initialized)
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
                logInternal(Tag, "RequestSuccess: $result")
                state.value = AdState.Bid(
                    AuctionResult(
                        priceFloor = result.price,
                        adSource = this@BMInterstitialAdImpl,
                    )
                )
            }

            override fun onRequestFailed(request: InterstitialRequest, bmError: BMError) {
                adRequest = request
                logError(Tag, "Error while bidding: $bmError")
                bmError.code
                state.value = AdState.LoadFailed(bmError.asBidonError(demandId))
            }

            override fun onRequestExpired(request: InterstitialRequest) {
                adRequest = request
                state.value = AdState.LoadFailed(DemandError.Expired(demandId))
            }
        }
    }

    private val interstitialListener by lazy {
        object : InterstitialListener {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                state.value = AdState.Fill(interstitialAd.asAd())
            }

            override fun onAdLoadFailed(interstitialAd: InterstitialAd, bmError: BMError) {
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                state.value = AdState.LoadFailed(bmError.asBidonError(demandId))
            }

            @Deprecated("Source BidMachine deprecated callback")
            override fun onAdShown(interstitialAd: InterstitialAd) {
            }

            override fun onAdShowFailed(interstitialAd: InterstitialAd, bmError: BMError) {
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                state.value = AdState.ShowFailed(bmError.asBidonError(demandId))
            }

            override fun onAdImpression(interstitialAd: InterstitialAd) {
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                state.value = AdState.Impression(interstitialAd.asAd())
            }

            override fun onAdClicked(interstitialAd: InterstitialAd) {
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                state.value = AdState.Clicked(interstitialAd.asAd())
            }

            override fun onAdExpired(interstitialAd: InterstitialAd) {
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                state.value = AdState.Expired(interstitialAd.asAd())
            }

            override fun onAdClosed(interstitialAd: InterstitialAd, boolean: Boolean) {
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                state.value = AdState.Closed(interstitialAd.asAd())
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
            InterstitialRequest.Builder()
                .setAdContentType(AdContentType.All)
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
            val bmInterstitialAd = InterstitialAd(context).also {
                interstitialAd = it
            }
            bmInterstitialAd.setListener(interstitialListener)
            bmInterstitialAd.load(adRequest)
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
        if (interstitialAd?.canShow() == true) {
            interstitialAd?.show()
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
        interstitialAd?.destroy()
        interstitialAd = null
    }

    private fun InterstitialAd.asAd(): Ad {
        return Ad(
            demandId = demandId,
            demandAd = demandAd,
            price = this.auctionResult?.price ?: 0.0,
            sourceAd = this,
            currencyCode = "USD",
            roundId = roundId,
            dsp = this.auctionResult?.demandSource,
            monetizationNetwork = this.auctionResult?.demandSource
        )
    }
}

private const val Tag = "BidMachine Interstitial"
