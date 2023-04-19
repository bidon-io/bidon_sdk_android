package org.bidon.dtexchange.impl

import android.app.Activity
import android.widget.FrameLayout
import com.fyber.inneractive.sdk.external.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.bidon.dtexchange.ext.asBidonError
import org.bidon.sdk.adapter.*
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.impl.dpToPx
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.minByPricefloorOrNull
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.stats.models.asRoundStatus
import org.bidon.sdk.utils.SdkDispatchers

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
    private val dispatcher: CoroutineDispatcher = SdkDispatchers.Main
    private var adSpot: InneractiveAdSpot? = null

    override val ad: Ad?
        get() = adSpot?.asAd()
    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE)
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
            pricefloor = pricefloor,
            bannerFormat = bannerFormat,
            context = activity.applicationContext,
            containerWidth = containerWidth
        )
    }

    override suspend fun bid(adParams: DTExchangeBannerAuctionParams): AuctionResult {
        logInfo(Tag, "Starting with $adParams")
        return withContext(dispatcher) {
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
                    adEvent.tryEmit(AdEvent.LoadFailed(inneractiveErrorCode.asBidonError()))
                }
            })
            adSpot.requestAd(adRequest)

            // WAIT FOR RESULT
            val state = adEvent.first {
                it is AdEvent.Bid || it is AdEvent.LoadFailed
            }
            when (state) {
                is AdEvent.LoadFailed -> {
                    AuctionResult(
                        ecpm = adParams.lineItem.pricefloor,
                        adSource = this@DTExchangeBanner,
                        roundStatus = state.cause.asRoundStatus()
                    )
                }
                is AdEvent.Bid -> state.result
                else -> error("unexpected: $state")
            }
        }
    }

    override suspend fun fill(): Result<Ad> = runCatching {
        logInfo(Tag, "Starting fill: $this")
        /**
         * DataExchange fills the bid automatically. It's not needed to fill it manually.
         */
        val event = AdEvent.Fill(requireNotNull(adSpot?.asAd()))
        adEvent.tryEmit(event)
        event.ad
    }

    override fun getAdView(): AdViewHolder {
        // Getting the spot's controller
        val controller = adSpot?.selectedUnitController as InneractiveAdViewUnitController
        // set to new container, because DTExchange does not expose its bannerView
        val container = FrameLayout(requireNotNull(param?.context))
        controller.eventsListener = object : InneractiveAdViewEventsListener {
            override fun onAdImpression(adSpot: InneractiveAdSpot?) {
                logInfo(Tag, "onAdImpression: $adSpot")
                val adValue = AdValue(
                    adRevenue = param?.pricefloor ?: 0.0,
                    precision = Precision.Estimated,
                    currency = AdValue.USD
                )
                val ad = adSpot?.asAd() ?: return
                adEvent.tryEmit(AdEvent.PaidRevenue(ad, adValue))
                adEvent.tryEmit(AdEvent.Shown(ad))
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
            widthPx = FrameLayout.LayoutParams.MATCH_PARENT,
            heightPx = when (param?.bannerFormat) {
                BannerFormat.Adaptive,
                BannerFormat.Banner -> 50.dpToPx
                BannerFormat.LeaderBoard -> 90.dpToPx
                BannerFormat.MRec -> 250.dpToPx
                null -> FrameLayout.LayoutParams.WRAP_CONTENT
            }
        )
    }

    override fun show(activity: Activity) {}

    override fun destroy() {
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