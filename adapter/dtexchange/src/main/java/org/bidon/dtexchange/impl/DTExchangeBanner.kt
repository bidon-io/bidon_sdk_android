package org.bidon.dtexchange.impl

import android.app.Activity
import android.widget.FrameLayout
import com.fyber.inneractive.sdk.external.ImpressionData
import com.fyber.inneractive.sdk.external.InneractiveAdRequest
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager
import com.fyber.inneractive.sdk.external.InneractiveAdViewEventsListenerWithImpressionData
import com.fyber.inneractive.sdk.external.InneractiveAdViewUnitController
import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.fyber.inneractive.sdk.external.InneractiveUnitController
import kotlinx.coroutines.flow.MutableSharedFlow
import org.bidon.dtexchange.ext.asAdValue
import org.bidon.dtexchange.ext.asBidonError
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.impl.pxToDp
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.minByPricefloorOrNull
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus

/**
 * Created by Aleksei Cherniaev on 17/04/2023.
 */
internal class DTExchangeBanner(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val auctionId: String,
) : AdSource.Banner<DTExchangeBannerAuctionParams>,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId,
        demandAd = demandAd
    ) {

    private var param: DTExchangeBannerAuctionParams? = null
    private var adSpot: InneractiveAdSpot? = null

    override val ad: Ad?
        get() = adSpot?.asAd()
    override val adEvent =
        MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE, replay = 1)
    override val isAdReadyToShow: Boolean get() = adSpot?.isReady == true

    override fun getAuctionParams(
        activity: Activity,
        pricefloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        bannerFormat: BannerFormat,
        onLineItemConsumed: (LineItem) -> Unit,
        containerWidth: Float,
    ): Result<AdAuctionParams> = runCatching {
        val lineItem = lineItems
            .minByPricefloorOrNull(demandId, pricefloor)
            ?.also(onLineItemConsumed) ?: error(BidonError.NoAppropriateAdUnitId)
        DTExchangeBannerAuctionParams(
            lineItem = lineItem,
            bannerFormat = bannerFormat,
            context = activity.applicationContext,
        )
    }

    override fun bid(adParams: DTExchangeBannerAuctionParams) {
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
                adEvent.tryEmit(
                    AdEvent.Bid(
                        AuctionResult(
                            ecpm = adParams.lineItem.pricefloor,
                            adSource = this@DTExchangeBanner,
                            roundStatus = RoundStatus.Successful
                        )
                    )
                )
            }

            override fun onInneractiveFailedAdRequest(
                inneractiveAdSpot: InneractiveAdSpot?,
                inneractiveErrorCode: InneractiveErrorCode?
            ) {
                logInfo(Tag, "onInneractiveFailedAdRequest: $inneractiveErrorCode")
                adEvent.tryEmit(
                    AdEvent.LoadFailed(inneractiveErrorCode.asBidonError())
                )
            }
        })
        adSpot.requestAd(adRequest)
    }

    override fun fill() {
        logInfo(Tag, "Starting fill: $this")
        /**
         * DataExchange fills the bid automatically. It's not needed to fill it manually.
         */
        adEvent.tryEmit(AdEvent.Fill(requireNotNull(adSpot?.asAd())))
    }

    override fun getAdView(): AdViewHolder {
        // Getting the spot's controller
        val controller = adSpot?.selectedUnitController as InneractiveAdViewUnitController
        // set to new container, because DTExchange does not expose its bannerView
        val container = FrameLayout(requireNotNull(param?.context))
        controller.eventsListener = object : InneractiveAdViewEventsListenerWithImpressionData {
            override fun onAdImpression(
                adSpot: InneractiveAdSpot?,
                impressionData: ImpressionData?
            ) {
                logInfo(Tag, "onAdImpression: $adSpot, $impressionData")
                val adValue = impressionData?.asAdValue() ?: return
                val ad = adSpot?.asAd() ?: return
                adEvent.tryEmit(AdEvent.PaidRevenue(ad, adValue))
                // tracked impression/shown by [BannerView]
            }

            override fun onAdImpression(adSpot: InneractiveAdSpot?) {
            }

            override fun onAdClicked(adSpot: InneractiveAdSpot?) {
                logInfo(Tag, "onAdClicked: $adSpot")
                adSpot?.asAd()?.let {
                    adEvent.tryEmit(AdEvent.Clicked(ad = it))
                }
            }

            override fun onAdEnteredErrorState(
                adSpot: InneractiveAdSpot?,
                adDisplayError: InneractiveUnitController.AdDisplayError?
            ) {
                logInfo(Tag, "onAdEnteredErrorState: $adSpot, $adDisplayError")
                adEvent.tryEmit(AdEvent.ShowFailed(adDisplayError.asBidonError()))
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
        )
    }

    override fun show(activity: Activity) {}

    override fun destroy() {
        adSpot?.setRequestListener(null)
        adSpot?.destroy()
        adSpot = null
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