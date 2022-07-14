package com.appodealstack.ironsource

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.appodealstack.ironsource.impl.asBidonError
import com.appodealstack.ironsource.interstitial.addLevelPlayInterstitialListener
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.auctions.AuctionRequest
import com.appodealstack.mads.auctions.AuctionResult
import com.appodealstack.mads.demands.*
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.logger.IronSourceError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object IronSourceParameters : AdapterParameters

val IronSourceDemandId = DemandId("ironsource")

class IronSourceAdapter : Adapter.Mediation<IronSourceParameters>,
    AdSource.Interstitial {
    override val demandId: DemandId = IronSourceDemandId

    private val scope: CoroutineScope get() = CoroutineScope(Dispatchers.Default)
    private val interstitialFlow = MutableSharedFlow<InterstitialInterceptor>(Int.MAX_VALUE)
    private var interstitialDemandAd: DemandAd? = null

    init {
        scope.launch {
            interstitialFlow.collect { callback ->
                interstitialDemandAd?.let { demandAd->
                    val coreListener = SdkCore.getListenerForDemand(demandAd)
                    when (callback) {
                        is InterstitialInterceptor.AdClicked -> {
                            val ad = Ad(
                                demandId = demandId,
                                demandAd = demandAd,
                                price = callback.adInfo?.revenue ?: 0.0,
                                sourceAd = callback.adInfo ?: demandAd
                            )
                            coreListener.onAdClicked(ad)
                        }
                        is InterstitialInterceptor.AdClosed -> {
                            val ad = Ad(
                                demandId = demandId,
                                demandAd = demandAd,
                                price = callback.adInfo?.revenue ?: 0.0,
                                sourceAd = callback.adInfo ?: demandAd
                            )
                            coreListener.onAdHidden(ad)
                        }
                        is InterstitialInterceptor.AdOpened -> {
                            val ad = Ad(
                                demandId = demandId,
                                demandAd = demandAd,
                                price = callback.adInfo?.revenue ?: 0.0,
                                sourceAd = callback.adInfo ?: demandAd
                            )
                            coreListener.onAdImpression(ad)
                        }
                        is InterstitialInterceptor.AdShowFailed -> {
                            coreListener.onAdDisplayFailed(callback.ironSourceError.asBidonError())
                        }
                        is InterstitialInterceptor.AdShowSucceeded -> {
                            val ad = Ad(
                                demandId = demandId,
                                demandAd = demandAd,
                                price = callback.adInfo?.revenue ?: 0.0,
                                sourceAd = callback.adInfo ?: demandAd
                            )
                            coreListener.onAdDisplayed(ad)
                        }
                        is InterstitialInterceptor.AdReady,
                        is InterstitialInterceptor.AdLoadFailed -> {
                            // do nothing
                        }
                    }
                }
            }
        }
    }

    override suspend fun init(context: Context, configParams: IronSourceParameters) {
        interstitialFlow.addLevelPlayInterstitialListener()
    }

    override fun interstitial(activity: Activity?, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
        return AuctionRequest {
            interstitialDemandAd = demandAd
            IronSource.loadInterstitial()
            val loadingResult = interstitialFlow.first {
                it is InterstitialInterceptor.AdLoadFailed || it is InterstitialInterceptor.AdReady
            }
            when (loadingResult) {
                is InterstitialInterceptor.AdReady -> {
                    Result.success(
                        AuctionResult(
                            ad = Ad(
                                demandId = demandId,
                                demandAd = demandAd,
                                price = loadingResult.adInfo?.revenue ?: 0.0,
                                sourceAd = loadingResult.adInfo ?: demandAd
                            ),
                            adProvider = object : AdProvider {
                                override fun canShow() = IronSource.isInterstitialReady()
                                override fun destroy() {}

                                override fun showAd(activity: Activity?, adParams: Bundle) {
                                    val placementId = adParams.getString(PlacementKey)
                                    if (placementId.isNullOrBlank()) {
                                        IronSource.showInterstitial()
                                    } else {
                                        IronSource.showInterstitial(placementId)
                                    }
                                }
                            }
                        )
                    )
                }
                is InterstitialInterceptor.AdLoadFailed -> {
                    Result.failure(loadingResult.ironSourceError.asBidonError())
                }
                else -> error("Unexpected state")
            }
        }
    }
}

internal sealed interface InterstitialInterceptor {
    class AdReady(val adInfo: AdInfo?) : InterstitialInterceptor
    class AdLoadFailed(val ironSourceError: IronSourceError?) : InterstitialInterceptor
    class AdOpened(val adInfo: AdInfo?) : InterstitialInterceptor
    class AdShowSucceeded(val adInfo: AdInfo?) : InterstitialInterceptor
    class AdShowFailed(val adInfo: AdInfo?, val ironSourceError: IronSourceError?) : InterstitialInterceptor
    class AdClicked(val adInfo: AdInfo?) : InterstitialInterceptor
    class AdClosed(val adInfo: AdInfo?) : InterstitialInterceptor
}


const val PlacementKey = "placement"