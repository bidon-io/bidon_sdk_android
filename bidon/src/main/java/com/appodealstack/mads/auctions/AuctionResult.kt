package com.appodealstack.mads.auctions

import com.appodealstack.mads.demands.AdType
import com.appodealstack.mads.demands.DemandId
import com.appodealstack.mads.demands.ObjRequest

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