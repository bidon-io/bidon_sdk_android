package com.appodealstack.applovin

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.sdk.AppLovinSdk
import com.appodealstack.applovin.ext.asBidonError
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.auctions.AuctionRequest
import com.appodealstack.mads.auctions.AuctionResult
import com.appodealstack.mads.demands.*
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

val ApplovinMaxDemandId = DemandId("applovin")

class ApplovinMaxAdapter : Adapter.Mediation, AdSource.Interstitial {
    override val demandId: DemandId = ApplovinMaxDemandId

    override suspend fun init(context: Context, configParams: Bundle) {
        require(AppLovinSdk.getInstance(context).isInitialized)
    }

    override fun interstitial(activity: Activity?, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
        if (activity == null) return AuctionRequest { Result.failure(DemandError.NoActivity) }
        val adUnitId = adParams.getString(adUnitIdKey)
        val maxInterstitialAd = MaxInterstitialAd(adUnitId, activity)

        return AuctionRequest {
            suspendCancellableCoroutine { continuation ->
                val isFinished = AtomicBoolean(false)
                maxInterstitialAd.setListener(
                    object : MaxAdListener {
                        override fun onAdLoaded(ad: MaxAd) {
                            if (!isFinished.getAndSet(true)) {
                                val auctionResult = AuctionResult(
                                    ad = Ad(
                                        demandId = ApplovinMaxDemandId,
                                        demandAd = demandAd,
                                        price = ad.revenue,
                                        sourceAd = ad
                                    ),
                                    adProvider = object : AdProvider {
                                        override fun canShow(): Boolean {
                                            return maxInterstitialAd.isReady
                                        }

                                        override fun showAd(activity: Activity?, adParams: Bundle) {
                                            val placement = adParams.getString(placementKey)
                                            val customData = adParams.getString(customDataKey)
                                            maxInterstitialAd.showAd(placement, customData)
                                        }

                                        override fun destroy() = maxInterstitialAd.destroy()

                                    }
                                )
                                maxInterstitialAd.setCoreListener(auctionResult)
                                continuation.resume(Result.success(auctionResult))
                            }
                        }

                        override fun onAdLoadFailed(adUnitId: String?, error: MaxError) {
                            if (!isFinished.getAndSet(true)) {
                                // remove listener
                                maxInterstitialAd.setListener(null)
                                continuation.resume(Result.failure(error.asBidonError()))
                            }
                        }

                        override fun onAdDisplayed(ad: MaxAd?) {
                            error("unexpected state. remove on release a28.")
                        }

                        override fun onAdHidden(ad: MaxAd?) {
                            error("unexpected state. remove on release a28.")
                        }

                        override fun onAdClicked(ad: MaxAd?) {
                            error("unexpected state. remove on release a28.")
                        }

                        override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
                            error("unexpected state. remove on release a28.")
                        }
                    }
                )
                maxInterstitialAd.loadAd()
            }
        }
    }

    private fun MaxInterstitialAd.setCoreListener(auctionResult: AuctionResult) {
        val core = SdkCore.getListenerForDemand(auctionResult.ad.demandAd)
        this.setListener(
            object : MaxAdListener {
                override fun onAdLoaded(ad: MaxAd?) {
                    core.onAdClicked(auctionResult.ad)
                }

                override fun onAdDisplayed(ad: MaxAd?) {
                    core.onAdDisplayed(auctionResult.ad)
                }

                override fun onAdHidden(ad: MaxAd?) {
                    core.onAdHidden(auctionResult.ad)
                }

                override fun onAdClicked(ad: MaxAd?) {
                    core.onAdClicked(auctionResult.ad)
                }

                override fun onAdLoadFailed(adUnitId: String?, error: MaxError) {
                    core.onAdLoadFailed(error.asBidonError())
                }

                override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError) {
                    core.onAdDisplayFailed(error.asBidonError())
                }
            }
        )
    }
}

internal const val adUnitIdKey = "adUnitId"
internal const val placementKey = "placement"
internal const val customDataKey = "customData"
internal const val keyKey = "key"
internal const val valueKey = "valueKey"