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
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.auction.ext.height
import org.bidon.sdk.auction.ext.width
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

/**
 * Created by Aleksei Cherniaev on 17/04/2023.
 */
internal class DTExchangeBanner :
    AdSource.Banner<DTExchangeBannerAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var adSpot: InneractiveAdSpot? = null
    private var adViewHolder: AdViewHolder? = null
    private var demandSource: String? = null

    override val isAdReadyToShow: Boolean get() = adSpot?.isReady == true

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            val adUnit = adUnit
            DTExchangeBannerAuctionParams(
                adUnit = adUnit,
                bannerFormat = bannerFormat,
                activity = activity,
            )
        }
    }

    override fun load(adParams: DTExchangeBannerAuctionParams) {
        logInfo(TAG, "Starting with $adParams")
        val spotId = adParams.spotId ?: run {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(demandId = demandId, "spotId")
                )
            )
            return
        }
        val adSpot = InneractiveAdSpotManager.get().createSpot()
        val controller = InneractiveAdViewUnitController()
        adSpot.addUnitController(controller)
        val adRequest = InneractiveAdRequest(spotId)
        adSpot.setRequestListener(object : InneractiveAdSpot.RequestListener {
            override fun onInneractiveSuccessfulAdRequest(inneractiveAdSpot: InneractiveAdSpot?) {
                logInfo(TAG, "onInneractiveSuccessfulAdRequest: $inneractiveAdSpot")
                this@DTExchangeBanner.adSpot = inneractiveAdSpot
                adParams.activity.runOnUiThread {
                    createViewHolder(inneractiveAdSpot, adParams)
                    getAd()?.let {
                        emitEvent(AdEvent.Fill(it))
                    }
                }
            }

            override fun onInneractiveFailedAdRequest(
                inneractiveAdSpot: InneractiveAdSpot?,
                inneractiveErrorCode: InneractiveErrorCode?
            ) {
                logInfo(TAG, "onInneractiveFailedAdRequest: $inneractiveErrorCode")
                emitEvent(AdEvent.LoadFailed(inneractiveErrorCode.asBidonError()))
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

    private fun createViewHolder(adSpot: InneractiveAdSpot?, adParams: DTExchangeBannerAuctionParams): AdViewHolder? {
        val controller = adSpot?.selectedUnitController as? InneractiveAdViewUnitController ?: return null
        val context = adParams.activity.applicationContext ?: return null
        val container = FrameLayout(context)
        controller.eventsListener = object : InneractiveAdViewEventsListenerWithImpressionData {
            override fun onAdImpression(
                adSpot: InneractiveAdSpot?,
                impressionData: ImpressionData?
            ) {
                logInfo(TAG, "onAdImpression: $adSpot, $impressionData")
                val adValue = impressionData?.asAdValue() ?: return
                demandSource = impressionData.demandSource
                setDsp(demandSource)
                val ad = getAd() ?: return
                emitEvent(AdEvent.PaidRevenue(ad, adValue))
                // tracked impression/shown by [BannerView]
            }

            override fun onAdImpression(adSpot: InneractiveAdSpot?) {
            }

            override fun onAdClicked(adSpot: InneractiveAdSpot?) {
                logInfo(TAG, "onAdClicked: $adSpot")
                getAd()?.let {
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
            widthDp = adParams.bannerFormat.width,
            heightDp = adParams.bannerFormat.height
        ).also {
            this.adViewHolder = it
        }
    }
}

private const val TAG = "DTExchangeBanner"