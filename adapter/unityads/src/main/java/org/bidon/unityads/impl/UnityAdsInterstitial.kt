package org.bidon.unityads.impl

import android.app.Activity
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdLoadingType
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.unityads.ext.asBidonError

/**
 * Created by Aleksei Cherniaev on 02/03/2023.
 */
internal class UnityAdsInterstitial :
    AdSource.Interstitial<UnityAdsFullscreenAuctionParams>,
    AdLoadingType.Network<UnityAdsFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var lineItem: LineItem? = null

    override var isAdReadyToShow: Boolean = false

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            UnityAdsFullscreenAuctionParams(
                lineItem = popLineItem(demandId) ?: error(BidonError.NoAppropriateAdUnitId)
            )
        }
    }

    override fun fill(adParams: UnityAdsFullscreenAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        lineItem = adParams.lineItem

        val loadListener = object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                logInfo(TAG, "onUnityAdsAdLoaded: $this")
                isAdReadyToShow = true
                getAd(this@UnityAdsInterstitial)?.let {
                    emitEvent(AdEvent.Fill(it))
                }
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String?,
                error: UnityAds.UnityAdsLoadError?,
                message: String?
            ) {
                logError(
                    tag = TAG,
                    message = "onUnityAdsFailedToLoad: placementId=$placementId, error=$error, message=$message",
                    error = error?.asBidonError()
                )
                emitEvent(AdEvent.LoadFailed(error.asBidonError()))
            }
        }
        UnityAds.load(adParams.lineItem.adUnitId, loadListener)
    }

    override fun show(activity: Activity) {
        val showListener = object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(
                placementId: String?,
                error: UnityAds.UnityAdsShowError?,
                message: String?
            ) {
                logError(
                    tag = TAG,
                    message = "onUnityAdsShowFailure: placementId=$placementId, error=$error, message=$message",
                    error = error.asBidonError()
                )
                emitEvent(AdEvent.ShowFailed(error.asBidonError()))
            }

            override fun onUnityAdsShowStart(placementId: String?) {
                logInfo(TAG, "onUnityAdsShowStart: placementId=$placementId")
                getAd(this@UnityAdsInterstitial)?.let {
                    emitEvent(AdEvent.Shown(it))
                    emitEvent(
                        AdEvent.PaidRevenue(
                            ad = it,
                            adValue = AdValue(
                                adRevenue = (lineItem?.pricefloor ?: 0.0) / 1000.0,
                                currency = AdValue.USD,
                                precision = Precision.Estimated
                            )
                        )
                    )
                }
            }

            override fun onUnityAdsShowClick(placementId: String?) {
                logInfo(TAG, "onUnityAdsShowClick. placementId: $placementId")
                getAd(this@UnityAdsInterstitial)?.let { emitEvent(AdEvent.Clicked(it)) }
            }

            override fun onUnityAdsShowComplete(
                placementId: String?,
                state: UnityAds.UnityAdsShowCompletionState?
            ) {
                logInfo(TAG, "onUnityAdsShowComplete: placementId=$placementId, state=$state")
                getAd(this@UnityAdsInterstitial)?.let { emitEvent(AdEvent.Closed(it)) }
            }
        }
        UnityAds.show(activity, lineItem?.adUnitId, UnityAdsShowOptions(), showListener)
        isAdReadyToShow = false
    }

    override fun destroy() {
        // do nothing
    }
}

private const val TAG = "UnityAdsInterstitial"