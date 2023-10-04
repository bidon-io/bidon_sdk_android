package org.bidon.dtexchange.impl

import android.app.Activity
import com.fyber.inneractive.sdk.external.ImpressionData
import com.fyber.inneractive.sdk.external.InneractiveAdRequest
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager
import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListenerWithImpressionData
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController
import com.fyber.inneractive.sdk.external.InneractiveFullscreenVideoContentController
import com.fyber.inneractive.sdk.external.InneractiveUnitController
import org.bidon.dtexchange.ext.asAdValue
import org.bidon.dtexchange.ext.asBidonError
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

/**
 * Created by Aleksei Cherniaev on 28/02/2023.
 */
internal class DTExchangeInterstitial :
    AdSource.Interstitial<DTExchangeAdAuctionParams>,
    Mode.Network,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var lineItem: LineItem? = null
    private var inneractiveAdSpot: InneractiveAdSpot? = null
    private var demandSource: String? = null

    override val isAdReadyToShow: Boolean
        get() = inneractiveAdSpot?.isReady == true

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            val lineItem = popLineItem(demandId) ?: error(BidonError.NoAppropriateAdUnitId)
            DTExchangeAdAuctionParams(lineItem)
        }
    }

    override fun load(adParams: DTExchangeAdAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        lineItem = adParams.lineItem
        val spot = InneractiveAdSpotManager.get().createSpot()
        val controller = InneractiveFullscreenUnitController()
        val videoController = InneractiveFullscreenVideoContentController()
        controller.addContentController(videoController)
        controller.eventsListener = object : InneractiveFullscreenAdEventsListenerWithImpressionData {
            override fun onAdImpression(
                adSpot: InneractiveAdSpot?,
                impressionData: ImpressionData?
            ) {
                logInfo(TAG, "onAdImpression: $adSpot")
                val adValue = impressionData?.asAdValue() ?: return
                demandSource = impressionData.demandSource
                setDsp(demandSource)
                val ad = getAd() ?: return
                emitEvent(AdEvent.PaidRevenue(ad, adValue))
                emitEvent(AdEvent.Shown(ad))
            }

            override fun onAdImpression(adSpot: InneractiveAdSpot?) {}

            override fun onAdClicked(adSpot: InneractiveAdSpot?) {
                logInfo(TAG, "onAdClicked: $adSpot")
                getAd()?.let {
                    emitEvent(AdEvent.Clicked(ad = it))
                }
            }

            override fun onAdWillCloseInternalBrowser(adSpot: InneractiveAdSpot?) {}
            override fun onAdWillOpenExternalApp(adSpot: InneractiveAdSpot?) {}

            override fun onAdEnteredErrorState(
                adSpot: InneractiveAdSpot?,
                adDisplayError: InneractiveUnitController.AdDisplayError?
            ) {
                logInfo(TAG, "onAdEnteredErrorState: $adSpot, $adDisplayError")
                emitEvent(AdEvent.ShowFailed(adDisplayError.asBidonError()))
            }

            override fun onAdDismissed(adSpot: InneractiveAdSpot?) {
                logInfo(TAG, "onAdDismissed: $adSpot")
                getAd()?.let {
                    emitEvent(AdEvent.Closed(ad = it))
                }
            }
        }
        spot.addUnitController(controller)
        val adRequest = InneractiveAdRequest(adParams.spotId)
        spot.setRequestListener(
            object : InneractiveAdSpot.RequestListener {
                override fun onInneractiveSuccessfulAdRequest(inneractiveAdSpot: InneractiveAdSpot?) {
                    logInfo(TAG, "onInneractiveSuccessfulAdRequest: $inneractiveAdSpot")
                    this@DTExchangeInterstitial.inneractiveAdSpot = inneractiveAdSpot
                    setDsp(demandSource ?: inneractiveAdSpot?.mediationNameString)
                    getAd()?.let {
                        emitEvent(AdEvent.Fill(it))
                    }
                }

                override fun onInneractiveFailedAdRequest(
                    inneractiveAdSpot: InneractiveAdSpot?,
                    inneractiveErrorCode: InneractiveErrorCode?
                ) {
                    logInfo(TAG, "onInneractiveFailedAdRequest: $inneractiveErrorCode")
                    emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
                }
            }
        )
        spot.requestAd(adRequest)
    }

    override fun show(activity: Activity) {
        val controller =
            inneractiveAdSpot?.selectedUnitController as? InneractiveFullscreenUnitController
        if (inneractiveAdSpot?.isReady == true && controller != null) {
            controller.show(activity)
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }

    override fun destroy() {
        inneractiveAdSpot?.destroy()
        inneractiveAdSpot = null
    }
}

private const val TAG = "DTExchangeInterstitial"