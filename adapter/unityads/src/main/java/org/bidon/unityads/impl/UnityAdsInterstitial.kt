package org.bidon.unityads.impl

import android.app.Activity
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import kotlinx.coroutines.flow.MutableSharedFlow
import org.bidon.sdk.adapter.*
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.minByPricefloorOrNull
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.unityads.ext.asBidonError

/**
 * Created by Bidon Team on 02/03/2023.
 */
internal class UnityAdsInterstitial(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val auctionId: String,
) : AdSource.Interstitial<UnityAdsFullscreenAuctionParams>,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId,
        demandAd = demandAd
    ) {

    private var lineItem: LineItem? = null

    override val ad: Ad?
        get() = lineItem?.let {
            Ad(
                demandAd = demandAd,
                adUnitId = it.adUnitId,
                dsp = null,
                currencyCode = AdValue.USD,
                networkName = demandId.demandId,
                auctionId = auctionId,
                ecpm = it.pricefloor,
                demandAdObject = Unit,
                roundId = roundId
            )
        }

    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE, replay = 1)

    override var isAdReadyToShow: Boolean = false

    override fun getAuctionParams(
        activity: Activity,
        pricefloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        onLineItemConsumed: (LineItem) -> Unit
    ): Result<AdAuctionParams> = runCatching {
        val lineItem = lineItems
            .minByPricefloorOrNull(demandId, pricefloor)
            ?.also(onLineItemConsumed) ?: error(org.bidon.sdk.config.BidonError.NoAppropriateAdUnitId)
        UnityAdsFullscreenAuctionParams(lineItem)
    }

    override fun bid(adParams: UnityAdsFullscreenAuctionParams) {
        logInfo(Tag, "Starting with $adParams: $this")
        lineItem = adParams.lineItem

        val loadListener = object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                logInfo(Tag, "onUnityAdsAdLoaded: $this")
                isAdReadyToShow = true
                adEvent.tryEmit(
                    AdEvent.Bid(
                        AuctionResult(
                            ecpm = requireNotNull(lineItem?.pricefloor),
                            adSource = this@UnityAdsInterstitial,
                            roundStatus = RoundStatus.Successful
                        )
                    )
                )
            }

            override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                logError(
                    tag = Tag,
                    message = "onUnityAdsFailedToLoad: placementId=$placementId, error=$error, message=$message",
                    error = error?.asBidonError()
                )
                adEvent.tryEmit(AdEvent.LoadFailed(error.asBidonError()))
            }
        }
        UnityAds.load(adParams.lineItem.adUnitId, loadListener)
    }

    override fun fill() {
        logInfo(Tag, "Starting fill: $this")
        /**
         * UnityAds fills the bid automatically. It's not needed to fill it manually.
         */
        adEvent.tryEmit(AdEvent.Fill(requireNotNull(ad)))
    }

    override fun show(activity: Activity) {
        val showListener = object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                logError(
                    tag = Tag,
                    message = "onUnityAdsShowFailure: placementId=$placementId, error=$error, message=$message",
                    error = error.asBidonError()
                )
                adEvent.tryEmit(AdEvent.ShowFailed(error.asBidonError()))
            }

            override fun onUnityAdsShowStart(placementId: String?) {
                logInfo(Tag, "onUnityAdsShowStart: placementId=$placementId")
                ad?.let {
                    adEvent.tryEmit(AdEvent.Shown(it))
                    adEvent.tryEmit(
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
                logInfo(Tag, "onUnityAdsShowClick. placementId: $placementId")
                ad?.let { adEvent.tryEmit(AdEvent.Clicked(it)) }
            }

            override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                logInfo(Tag, "onUnityAdsShowComplete: placementId=$placementId, state=$state")
                ad?.let { adEvent.tryEmit(AdEvent.Closed(it)) }
            }
        }
        UnityAds.show(activity, lineItem?.adUnitId, UnityAdsShowOptions(), showListener)
        isAdReadyToShow = false
    }

    override fun destroy() {
        // do nothing
    }
}

private const val Tag = "UnityAdsInterstitial"