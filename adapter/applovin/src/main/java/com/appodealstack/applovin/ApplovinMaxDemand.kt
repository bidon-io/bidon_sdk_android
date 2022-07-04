package com.appodealstack.applovin

import android.content.Context
import android.os.Bundle
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.sdk.AppLovinSdk
import com.appodealstack.applovin.base.asBidonError
import com.appodealstack.applovin.ext.wrapToMaxAdListener
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.auctions.AuctionData
import com.appodealstack.mads.auctions.AuctionRequest
import com.appodealstack.mads.auctions.ObjRequest
import com.appodealstack.mads.base.AdType
import com.appodealstack.mads.demands.Demand
import com.appodealstack.mads.demands.DemandAd
import com.appodealstack.mads.demands.DemandId
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

val ApplovinMaxDemandId = DemandId("applovin")

class ApplovinMaxDemand : Demand.Mediation {
    override val demandId: DemandId = ApplovinMaxDemandId

    override suspend fun init(context: Context, configParams: Bundle) {
        require(AppLovinSdk.getInstance(context).isInitialized)
    }

    override fun createAuctionRequest(demandAd: DemandAd): AuctionRequest.Mediation {
        return object : AuctionRequest.Mediation {
            override suspend fun execute(): AuctionData {
                return executeRequest(demandAd)
            }
        }
    }

    private suspend fun executeRequest(demandAd: DemandAd): AuctionData = suspendCoroutine { continuation ->
        val isFinished = AtomicBoolean(false)
        when (val sourceAd = demandAd.objRequest) {
            is MaxInterstitialAd -> {
                val objRequest = createObjRequest(sourceAd)
                sourceAd.setListener(
                    object : MaxAdListener {
                        override fun onAdLoaded(ad: MaxAd) {
                            if (!isFinished.getAndSet(true)) {
                                setCoreListener(sourceAd, objRequest)
                                continuation.resume(
                                    AuctionData.Success(
                                        demandId = demandId,
                                        price = ad.revenue,
                                        adType = demandAd.adType,
                                        objRequest = objRequest,
                                        objResponse = ad,
                                    )
                                )
                            }
                        }

                        override fun onAdLoadFailed(adUnitId: String?, error: MaxError) {
                            if (!isFinished.getAndSet(true)) {
                                setCoreListener(sourceAd, objRequest)
                                val failure = AuctionData.Failure(
                                    demandId = demandAd.demandId,
                                    adType = demandAd.adType,
                                    objRequest = demandAd.objRequest,
                                    cause = error.asBidonError()
                                )
                                continuation.resume(failure)
                            }
                        }

                        override fun onAdDisplayed(ad: MaxAd?) {}
                        override fun onAdHidden(ad: MaxAd?) {}
                        override fun onAdClicked(ad: MaxAd?) {}
                        override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {}
                    }
                )
                sourceAd.loadAd()
            }
            else -> {
                isFinished.set(true)
                val error = Exception("$demandAd. Not implemented executeRequest() for ${demandAd.objRequest::class.java}")
                val failure = AuctionData.Failure(
                    demandId = demandAd.demandId,
                    adType = demandAd.adType,
                    objRequest = demandAd.objRequest,
                    cause = error
                )
                continuation.resume(failure)
            }
        }
    }

    private fun createObjRequest(objRequest: MaxInterstitialAd): ObjRequest = object : ObjRequest {
        override fun canShowAd(): Boolean {
            return objRequest.isReady
        }

        override fun showAd(adParams: Bundle) {
            val placement = adParams.getString(placementKey)
            val customData = adParams.getString(customDataKey)
            objRequest.showAd(placement, customData)
        }
    }

    private fun setCoreListener(maxInterstitialAd: MaxInterstitialAd, objRequest: ObjRequest) {
        maxInterstitialAd.setListener(
            SdkCore.getListenerForDemand(AdType.Interstitial).wrapToMaxAdListener(objRequest)
        )
    }
}

internal const val adUnitIdKey = "adUnitId"
internal const val placementKey = "placement"
internal const val customDataKey = "customData"
internal const val keyKey = "key"
internal const val valueKey = "valueKey"