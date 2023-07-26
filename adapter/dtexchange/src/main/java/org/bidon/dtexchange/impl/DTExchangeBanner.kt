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
import org.bidon.sdk.adapter.AdLoadingType
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.impl.pxToDp
import org.bidon.sdk.auction.models.minByPricefloorOrNull
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
    AdLoadingType.Network<DTExchangeBannerAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var param: DTExchangeBannerAuctionParams? = null
    private var adSpot: InneractiveAdSpot? = null
    private var adViewHolder: AdViewHolder? = null

    override val isAdReadyToShow: Boolean get() = adSpot?.isReady == true

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            val lineItem = lineItems
                .minByPricefloorOrNull(demandId, pricefloor)
                ?.also(onLineItemConsumed) ?: error("BidonError.NoAppropriateAdUnitId")
            DTExchangeBannerAuctionParams(
                lineItem = lineItem,
                bannerFormat = bannerFormat,
                context = activity.applicationContext,
            )
        }
    }

    override fun fill(adParams: DTExchangeBannerAuctionParams) {
        logInfo(Tag, "Starting with $adParams")
        param = adParams
        // Spot integration for display square
        val adSpot = InneractiveAdSpotManager.get().createSpot()
        // Adding the adview controller
        val controller = InneractiveAdViewUnitController()
        adSpot.addUnitController(controller)
        val adRequest = InneractiveAdRequest(adParams.adUnitId)
        adSpot.setRequestListener(object : InneractiveAdSpot.RequestListener {
            override fun onInneractiveSuccessfulAdRequest(inneractiveAdSpot: InneractiveAdSpot?) {
                logInfo(Tag, "onInneractiveSuccessfulAdRequest: $inneractiveAdSpot")
                this@DTExchangeBanner.adSpot = inneractiveAdSpot
                emitEvent(AdEvent.Fill(requireNotNull(inneractiveAdSpot?.asAd())))
            }

            override fun onInneractiveFailedAdRequest(
                inneractiveAdSpot: InneractiveAdSpot?,
                inneractiveErrorCode: InneractiveErrorCode?
            ) {
                logInfo(Tag, "onInneractiveFailedAdRequest: $inneractiveErrorCode")
                emitEvent(
                    AdEvent.LoadFailed(inneractiveErrorCode.asBidonError())
                )
            }
        })
        adSpot.requestAd(adRequest)
    }

    override fun getAdView(): AdViewHolder? {
        return adViewHolder ?: synchronized(this) {
            adViewHolder ?: createViewHolder(adSpot)
        }
    }

    override fun destroy() {
        adSpot?.setRequestListener(null)
        adSpot?.destroy()
        adSpot = null
        adViewHolder = null
    }

    private fun createViewHolder(adSpot: InneractiveAdSpot?): AdViewHolder? {
        // Getting the spot's controller
        val controller = adSpot?.selectedUnitController as? InneractiveAdViewUnitController ?: return null
        // set to new container, because DTExchange does not expose its bannerView
        val context = param?.context ?: return null
        val container = FrameLayout(context)
        controller.eventsListener = object : InneractiveAdViewEventsListenerWithImpressionData {
            override fun onAdImpression(
                adSpot: InneractiveAdSpot?,
                impressionData: ImpressionData?
            ) {
                logInfo(Tag, "onAdImpression: $adSpot, $impressionData")
                val adValue = impressionData?.asAdValue() ?: return
                val ad = adSpot?.asAd() ?: return
                emitEvent(AdEvent.PaidRevenue(ad, adValue))
                // tracked impression/shown by [BannerView]
            }

            override fun onAdImpression(adSpot: InneractiveAdSpot?) {
            }

            override fun onAdClicked(adSpot: InneractiveAdSpot?) {
                logInfo(Tag, "onAdClicked: $adSpot")
                adSpot?.asAd()?.let {
                    emitEvent(AdEvent.Clicked(ad = it))
                }
            }

            override fun onAdEnteredErrorState(
                adSpot: InneractiveAdSpot?,
                adDisplayError: InneractiveUnitController.AdDisplayError?
            ) {
                val cause = adDisplayError.asBidonError()
                logError(Tag, "onAdEnteredErrorState: $adSpot, $adDisplayError", cause)
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
        adUnitId = param?.adUnitId,
        networkName = demandId.demandId,
        currencyCode = AdValue.USD,
        demandAd = demandAd,
        dsp = this.mediationNameString,
        roundId = roundId,
        demandAdObject = this
    )
}

private const val Tag = "DTExchangeBanner"