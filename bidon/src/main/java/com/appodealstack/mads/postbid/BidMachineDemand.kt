package com.appodealstack.mads.postbid

import android.content.Context
import android.os.Bundle
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.auctions.AuctionData
import com.appodealstack.mads.auctions.AuctionRequest
import com.appodealstack.mads.auctions.ObjRequest
import com.appodealstack.mads.base.AdType
import com.appodealstack.mads.base.ContextProvider
import com.appodealstack.mads.demands.Demand
import com.appodealstack.mads.demands.DemandError
import com.appodealstack.mads.demands.DemandId
import com.appodealstack.mads.postbid.bidmachine.asBidonError
import io.bidmachine.BidMachine
import io.bidmachine.PriceFloorParams
import io.bidmachine.interstitial.InterstitialRequest
import io.bidmachine.utils.BMError
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

val BidMachineDemandId = DemandId("bidmachine")

internal class BidMachineDemand : Demand.PostBid {
    private val contextProvider = ContextProvider

    override val demandId = BidMachineDemandId

    override suspend fun init(context: Context, configParams: Bundle): Unit = suspendCoroutine { continuation ->
        val sourceId = configParams.getString(SourceIdKey) ?: "1" // TODO remove 1
        BidMachine.initialize(context, sourceId) {
            // TODO seems like init callback does not work
        }
        continuation.resume(Unit)
    }

    override fun createActionRequest(): AuctionRequest.PostBid {
        return object : AuctionRequest.PostBid {
            override suspend fun execute(additionalData: AuctionRequest.AdditionalData?): AuctionData {
                return executeRequest(additionalData)
            }
        }
    }

    private suspend fun executeRequest(additionalData: AuctionRequest.AdditionalData?): AuctionData =
        suspendCoroutine { continuation ->
            val isFinished = AtomicBoolean(false)
            val interstitialRequestBuilder = InterstitialRequest.Builder()
            additionalData?.let {
                interstitialRequestBuilder.setPriceFloorParams(
                    PriceFloorParams().addPriceFloor(it.priceFloor)
                )
            }
            interstitialRequestBuilder.setListener(
                object : InterstitialRequest.AdRequestListener {
                    override fun onRequestSuccess(
                        request: InterstitialRequest,
                        auctionResult: io.bidmachine.models.AuctionResult
                    ) {
                        if (!isFinished.getAndSet(true)) {
                            setCoreListener(interstitialRequestBuilder)
                            continuation.resume(
                                AuctionData.Success(
                                    demandId = demandId,
                                    price = auctionResult.price,
                                    objRequest = object : ObjRequest(interstitialRequestBuilder) {
                                        override fun showAd() {
                                            TODO()
                                        }
                                    },
                                    objResponse = auctionResult,
                                    adType = AdType.Interstitial,
                                )
                            )
                        }
                    }

                    override fun onRequestFailed(request: InterstitialRequest, bmError: BMError) {
                        if (!isFinished.getAndSet(true)) {
                            setCoreListener(interstitialRequestBuilder)
                            val failure = AuctionData.Failure(
                                demandId = demandId,
                                adType = AdType.Interstitial,
                                objRequest = interstitialRequestBuilder,
                                cause = bmError.asBidonError()
                            )
                            continuation.resume(failure)
                        }
                    }

                    override fun onRequestExpired(request: InterstitialRequest) {
                        if (!isFinished.getAndSet(true)) {
                            setCoreListener(interstitialRequestBuilder)
                            val failure = AuctionData.Failure(
                                demandId = demandId,
                                adType = AdType.Interstitial,
                                objRequest = interstitialRequestBuilder,
                                cause = DemandError.Expired
                            )
                            continuation.resume(failure)
                        }
                    }
                }
            )
            interstitialRequestBuilder
                .build()
                .request(contextProvider.requiredContext)
        }

    private fun setCoreListener(interstitialRequestBuilder: InterstitialRequest.Builder) {
        interstitialRequestBuilder.setListener(
            SdkCore.getListenerForDemand(AdType.Interstitial).wrapToBidMachineListener()
        )
    }
}

private const val SourceIdKey = "SourceId"