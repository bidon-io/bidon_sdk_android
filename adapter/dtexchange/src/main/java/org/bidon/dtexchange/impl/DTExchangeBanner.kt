package org.bidon.dtexchange.impl

import android.widget.FrameLayout
import com.fyber.inneractive.sdk.external.ImpressionData
import com.fyber.inneractive.sdk.external.InneractiveAdRequest
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager
import com.fyber.inneractive.sdk.external.InneractiveAdViewEventsListenerWithImpressionData
import com.fyber.inneractive.sdk.external.InneractiveAdViewUnitController
import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.fyber.inneractive.sdk.external.InneractiveUnitController
import org.bidon.dtexchange.ext.asAdValue
import org.bidon.dtexchange.ext.asBidonError
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.impl.pxToDp
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

/**
 * Created by Aleksei Cherniaev on 17/04/2023.
 */
internal class DTExchangeBanner :
    AdSource.Banner<DTExchangeBannerAuctionParams>,
    Mode.Network,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var param: DTExchangeBannerAuctionParams? = null
    private var adSpot: InneractiveAdSpot? = null
    private var adViewHolder: AdViewHolder? = null

    override val isAdReadyToShow: Boolean get() = adSpot?.isReady == true

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            val lineItem = popLineItem(demandId) ?: error(BidonError.NoAppropriateAdUnitId)
            DTExchangeBannerAuctionParams(
                lineItem = lineItem,
                bannerFormat = bannerFormat,
                context = activity.applicationContext,
            )
        }
    }

    override fun load(adParams: DTExchangeBannerAuctionParams) {
        logInfo(TAG, "Starting with $adParams")
        param = adParams
        val adSpot = InneractiveAdSpotManager.get().createSpot()
        val controller = InneractiveAdViewUnitController()
        adSpot.addUnitController(controller)
        val adRequest = InneractiveAdRequest(adParams.spotId)
        adSpot.setRequestListener(object : InneractiveAdSpot.RequestListener {
            override fun onInneractiveSuccessfulAdRequest(inneractiveAdSpot: InneractiveAdSpot?) {
                logInfo(TAG, "onInneractiveSuccessfulAdRequest: $inneractiveAdSpot")
                this@DTExchangeBanner.adSpot = inneractiveAdSpot
                createViewHolder(inneractiveAdSpot)
                inneractiveAdSpot?.let {
                    emitEvent(AdEvent.Fill(it.asAd()))
                }
            }

            override fun onInneractiveFailedAdRequest(
                inneractiveAdSpot: InneractiveAdSpot?,
                inneractiveErrorCode: InneractiveErrorCode?
            ) {
                logInfo(TAG, "onInneractiveFailedAdRequest: $inneractiveErrorCode")
                emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
            }
        })
        adSpot.requestAd(adRequest)
    }

    override fun getAdView(): AdViewHolder? {
        return adViewHolder
    }

    override fun destroy() {
        adSpot?.setRequestListener(null)
        adSpot?.destroy()
        adSpot = null
        adViewHolder = null
    }

    private fun createViewHolder(adSpot: InneractiveAdSpot?): AdViewHolder? {
        val controller = adSpot?.selectedUnitController as? InneractiveAdViewUnitController ?: return null
        val context = param?.context ?: return null
        val container = FrameLayout(context)
        controller.eventsListener = object : InneractiveAdViewEventsListenerWithImpressionData {
            override fun onAdImpression(
                adSpot: InneractiveAdSpot?,
                impressionData: ImpressionData?
            ) {
                logInfo(TAG, "onAdImpression: $adSpot, $impressionData")
                val adValue = impressionData?.asAdValue() ?: return
                val ad = adSpot?.asAd() ?: return
                emitEvent(AdEvent.PaidRevenue(ad, adValue))
                // tracked impression/shown by [BannerView]
            }

            override fun onAdImpression(adSpot: InneractiveAdSpot?) {
            }

            override fun onAdClicked(adSpot: InneractiveAdSpot?) {
                logInfo(TAG, "onAdClicked: $adSpot")
                adSpot?.asAd()?.let {
                    emitEvent(AdEvent.Clicked(ad = it))
                }
            }

            override fun onAdEnteredErrorState(
                adSpot: InneractiveAdSpot?,
                adDisplayError: InneractiveUnitController.AdDisplayError?
            ) {
                val cause = adDisplayError.asBidonError()
                logError(TAG, "onAdEnteredErrorState: $adSpot, $adDisplayError", cause)
                emitEvent(AdEvent.ShowFailed(cause))
            }

            override fun onAdExpanded(adSpot: InneractiveAdSpot?) {}
            override fun onAdResized(adSpot: InneractiveAdSpot?) {}
            override fun onAdWillCloseInternalBrowser(adSpot: InneractiveAdSpot?) {}
            override fun onAdWillOpenExternalApp(adSpot: InneractiveAdSpot?) {}
            override fun onAdCollapsed(adSpot: InneractiveAdSpot?) {}
        }
        controller.bindView(container)
        return AdViewHolder(
            networkAdview = container,
            widthDp = when (param?.bannerFormat) {
                BannerFormat.Banner -> 320
                BannerFormat.LeaderBoard -> 728
                BannerFormat.MRec -> 300
                BannerFormat.Adaptive,
                null -> controller.adContentWidth.pxToDp
            },
            heightDp = when (param?.bannerFormat) {
                BannerFormat.Banner -> 50
                BannerFormat.LeaderBoard -> 90
                BannerFormat.MRec -> 250
                BannerFormat.Adaptive,
                null -> controller.adContentHeight.pxToDp
            }
        ).also {
            this.adViewHolder = it
        }
    }

    private fun InneractiveAdSpot.asAd() = Ad(
        ecpm = param?.lineItem?.pricefloor ?: 0.0,
        auctionId = auctionId,
        adUnitId = param?.spotId,
        networkName = demandId.demandId,
        currencyCode = AdValue.USD,
        demandAd = demandAd,
        dsp = this.mediationNameString,
        roundId = roundId,
        demandAdObject = this
    )
}

private const val TAG = "DTExchangeBanner"