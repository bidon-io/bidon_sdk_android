package com.appodealstack.mads.postbid

import com.appodealstack.mads.auctions.AuctionData
import com.appodealstack.mads.auctions.ObjRequest
import com.appodealstack.mads.base.AdType
import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.DemandError
import com.appodealstack.mads.postbid.bidmachine.asBidonError
import io.bidmachine.interstitial.InterstitialRequest
import io.bidmachine.models.AuctionResult
import io.bidmachine.utils.BMError

internal fun AdListener.wrapToBidMachineListener() = object : InterstitialRequest.AdRequestListener {
    override fun onRequestSuccess(request: InterstitialRequest, auctionResult: AuctionResult) {
        this@wrapToBidMachineListener.onAdLoaded(auctionResult.asAuctionSuccess(request))
    }

    override fun onRequestFailed(request: InterstitialRequest, bmError: BMError) {
        this@wrapToBidMachineListener.onDemandAdLoadFailed(
            AuctionData.Failure(
                demandId = BidMachineDemandId,
                adType = AdType.Interstitial,
                objRequest = request,
                cause = bmError.asBidonError(),
            )
        )
    }

    override fun onRequestExpired(request: InterstitialRequest) {
        this@wrapToBidMachineListener.onDemandAdLoadFailed(
            AuctionData.Failure(
                demandId = BidMachineDemandId,
                adType = AdType.Interstitial,
                objRequest = request,
                cause = DemandError.Expired,
            )
        )
    }

    private fun AuctionResult.asAuctionSuccess(request: InterstitialRequest) = AuctionData.Success(
        demandId = BidMachineDemandId,
        adType = AdType.Interstitial,
        price = this.price,
        objRequest = object : ObjRequest(request) {
            override fun showAd() {
                TODO()
            }
        },
        objResponse = this,
    )
}