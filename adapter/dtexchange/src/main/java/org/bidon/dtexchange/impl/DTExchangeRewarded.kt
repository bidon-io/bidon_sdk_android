package org.bidon.dtexchange.impl

import android.app.Activity
import com.fyber.inneractive.sdk.external.*
import kotlinx.coroutines.flow.MutableSharedFlow
import org.bidon.dtexchange.ext.asAdValue
import org.bidon.dtexchange.ext.asBidonError
import org.bidon.sdk.adapter.*
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.minByPricefloorOrNull
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus

/**
 * Created by Bidon Team on 28/02/2023.
 */
internal class DTExchangeRewarded(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val auctionId: String
) : AdSource.Rewarded<DTExchangeAdAuctionParams>,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId,
        demandAd = demandAd,
    ) {

    private var auctionParams: DTExchangeAdAuctionParams? = null
    private var inneractiveAdSpot: InneractiveAdSpot? = null

    private val adRequestListener by lazy {
        object : InneractiveAdSpot.RequestListener {
            override fun onInneractiveSuccessfulAdRequest(inneractiveAdSpot: InneractiveAdSpot?) {
                logInfo(Tag, "SuccessfulAdRequest: $inneractiveAdSpot")
                this@DTExchangeRewarded.inneractiveAdSpot = inneractiveAdSpot
                val ecpm = auctionParams?.lineItem?.pricefloor ?: 0.0
                adEvent.tryEmit(
                    AdEvent.Bid(
                        AuctionResult(
                            ecpm = ecpm,
                            adSource = this@DTExchangeRewarded,
                            roundStatus = RoundStatus.Successful
                        )
                    )
                )
            }

            override fun onInneractiveFailedAdRequest(
                inneractiveAdSpot: InneractiveAdSpot?,
                inneractiveErrorCode: InneractiveErrorCode?
            ) {
                logError(Tag, "Error while bidding: $inneractiveErrorCode", inneractiveErrorCode.asBidonError())
                adEvent.tryEmit(AdEvent.LoadFailed(inneractiveErrorCode.asBidonError()))
            }
        }
    }

    private val adRewardedListener by lazy {
        InneractiveFullScreenAdRewardedListener { inneractiveAdSpot ->
            adEvent.tryEmit(
                AdEvent.OnReward(
                    ad = inneractiveAdSpot.asAd(),
                    reward = null
                )
            )
            sendRewardImpression()
        }
    }

    private val impressionListener by lazy {
        object : InneractiveFullscreenAdEventsListenerWithImpressionData {
            override fun onAdImpression(adSpot: InneractiveAdSpot?, impressionData: ImpressionData?) {
                val adValue = impressionData?.asAdValue() ?: return
                val ad = adSpot?.asAd(impressionData.demandSource) ?: return
                adEvent.tryEmit(AdEvent.PaidRevenue(ad, adValue))
                adEvent.tryEmit(AdEvent.Shown(ad))
            }

            override fun onAdImpression(adSpot: InneractiveAdSpot?) {
            }

            override fun onAdClicked(adSpot: InneractiveAdSpot?) {
                adSpot?.asAd()?.let {
                    adEvent.tryEmit(AdEvent.Clicked(ad = it))
                }
            }

            override fun onAdWillCloseInternalBrowser(adSpot: InneractiveAdSpot?) {
            }

            override fun onAdWillOpenExternalApp(adSpot: InneractiveAdSpot?) {
            }

            override fun onAdEnteredErrorState(
                adSpot: InneractiveAdSpot?,
                adDisplayError: InneractiveUnitController.AdDisplayError?
            ) {
                adEvent.tryEmit(AdEvent.ShowFailed(adDisplayError.asBidonError()))
            }

            override fun onAdDismissed(adSpot: InneractiveAdSpot?) {
                adSpot?.asAd()?.let {
                    adEvent.tryEmit(AdEvent.Closed(ad = it))
                }
            }
        }
    }

    override val ad: Ad?
        get() = inneractiveAdSpot?.asAd()
    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE, replay = 1)
    override val isAdReadyToShow: Boolean
        get() = inneractiveAdSpot?.isReady == true

    override fun getAuctionParams(
        activity: Activity,
        pricefloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        onLineItemConsumed: (LineItem) -> Unit
    ): Result<AdAuctionParams> = runCatching {
        val lineItem = lineItems
            .minByPricefloorOrNull(demandId, pricefloor)
            ?.also(onLineItemConsumed)
        lineItem?.adUnitId ?: error(BidonError.NoAppropriateAdUnitId)
        DTExchangeAdAuctionParams(lineItem)
    }

    override fun bid(adParams: DTExchangeAdAuctionParams) {
        logInfo(Tag, "Starting with $adParams: $this")
        auctionParams = adParams
        val spot = InneractiveAdSpotManager.get().createSpot()
        val controller = InneractiveFullscreenUnitController()
        val videoController = InneractiveFullscreenVideoContentController()
        controller.addContentController(videoController)
        controller.eventsListener = impressionListener
        controller.rewardedListener = adRewardedListener
        spot.addUnitController(controller)

        val adRequest = InneractiveAdRequest(adParams.spotId)
        spot.setRequestListener(adRequestListener)
        spot.requestAd(adRequest)
    }

    override fun fill() {
        logInfo(Tag, "Starting fill: $this")
        /**
         * DataExchange fills the bid automatically. It's not needed to fill it manually.
         */
        adEvent.tryEmit(AdEvent.Fill(requireNotNull(inneractiveAdSpot?.asAd())))
    }

    override fun show(activity: Activity) {
        val controller = inneractiveAdSpot?.selectedUnitController as? InneractiveFullscreenUnitController
        if (inneractiveAdSpot?.isReady == true && controller != null) {
            controller.show(activity)
        } else {
            adEvent.tryEmit(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        }
    }

    override fun destroy() {
        inneractiveAdSpot?.destroy()
        inneractiveAdSpot = null
    }

    private fun InneractiveAdSpot.asAd(demandSource: String? = null) = Ad(
        ecpm = auctionParams?.lineItem?.pricefloor ?: 0.0,
        auctionId = auctionId,
        adUnitId = auctionParams?.lineItem?.adUnitId,
        networkName = demandId.demandId,
        currencyCode = AdValue.USD,
        demandAd = demandAd,
        dsp = demandSource ?: this.mediationNameString,
        roundId = roundId,
        demandAdObject = this
    )
}

private const val Tag = "DTExchangeRewarded"