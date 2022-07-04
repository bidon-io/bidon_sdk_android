package com.appodealstack.mads.auctions

import com.appodealstack.mads.base.AdType
import com.appodealstack.mads.demands.DemandId

sealed interface AuctionData {
    class Success(
        val demandId: DemandId,
        val price: Double,
        val adType: AdType,
        val objRequest: ObjRequest, // Demand's request Ad object – e.x. MaxInterstitialAd
        val objResponse: Any // Demand's response Ad object - e.x. MaxAd
    ) : AuctionData {
        override fun toString(): String {
            return "AuctionData.Success($demandId, price=$price, adType=$adType, objRequest: ${objRequest::class.java}, objResponse: ${objResponse::class.java})"
        }
    }

    class Failure(
        val demandId: DemandId,
        val adType: AdType,
        val objRequest: Any, // Demand's request Ad object – e.x. MaxInterstitialAd
        val cause: Throwable?
    ) : AuctionData {
        override fun toString(): String {
            return "AuctionData.Failure($demandId, adType=$adType, objRequest: ${objRequest::class.java}, cause: ${cause})"
        }
    }
}


open class ObjRequest(
    val objRequest: Any
) {
    open fun showAd() {
        error("Not implemented for $objRequest")
    }
}