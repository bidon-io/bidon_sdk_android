package org.bidon.dtexchange.impl

import android.app.Activity
import com.fyber.inneractive.sdk.external.ImpressionData
import com.fyber.inneractive.sdk.external.InneractiveAdRequest
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager
import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.fyber.inneractive.sdk.external.InneractiveFullScreenAdRewardedListener
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
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

/**
 * Created by Aleksei Cherniaev on 28/02/2023.
 */
internal class DTExchangeRewarded :
    AdSource.Rewarded<DTExchangeAdAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var inneractiveAdSpot: InneractiveAdSpot? = null
    private var adUnit: AdUnit? = null
    private var demandSource: String? = null

    override val isAdReadyToShow: Boolean
        get() = inneractiveAdSpot?.isReady == true

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            val adUnit = adUnit
            DTExchangeAdAuctionParams(adUnit)
        }
    }

    override fun load(adParams: DTExchangeAdAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        val spotId = adParams.spotId ?: run {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(demandId = demandId, "spotId")
                )
            )
            return
        }
        adUnit = adParams.adUnit
        val spot = InneractiveAdSpotManager.get().createSpot()
        val controller = InneractiveFullscreenUnitController()
        val videoController = InneractiveFullscreenVideoContentController()
        controller.addContentController(videoController)
        controller.eventsListener = object : InneractiveFullscreenAdEventsListenerWithImpressionData {
            override fun onAdImpression(
                adSpot: InneractiveAdSpot?,
                impressionData: ImpressionData?
            ) {
                val adValue = impressionData?.asAdValue() ?: return
                demandSource = impressionData.demandSource
                setDsp(demandSource)
                val ad = getAd() ?: return
                emitEvent(AdEvent.PaidRevenue(ad, adValue))
                emitEvent(AdEvent.Shown(ad))
            }

            override fun onAdImpression(adSpot: InneractiveAdSpot?) {}
            override fun onAdWillCloseInternalBrowser(adSpot: InneractiveAdSpot?) {}
            override fun onAdWillOpenExternalApp(adSpot: InneractiveAdSpot?) {}

            override fun onAdClicked(adSpot: InneractiveAdSpot?) {
                getAd()?.let {
                    emitEvent(AdEvent.Clicked(ad = it))
                }
            }

            override fun onAdEnteredErrorState(
                adSpot: InneractiveAdSpot?,
                adDisplayError: InneractiveUnitController.AdDisplayError?
            ) {
                emitEvent(AdEvent.ShowFailed(adDisplayError.asBidonError()))
            }

            override fun onAdDismissed(adSpot: InneractiveAdSpot?) {
                getAd()?.let {
                    emitEvent(AdEvent.Closed(ad = it))
                }
                this@DTExchangeRewarded.inneractiveAdSpot = null
            }
        }
        controller.rewardedListener = InneractiveFullScreenAdRewardedListener {
            getAd()?.let { ad ->
                emitEvent(
                    AdEvent.OnReward(
                        ad = ad,
                        reward = null
                    )
                )
            }
        }
        spot.addUnitController(controller)

        val adRequest = InneractiveAdRequest(spotId)
        spot.setRequestListener(
            object : InneractiveAdSpot.RequestListener {
                override fun onInneractiveSuccessfulAdRequest(inneractiveAdSpot: InneractiveAdSpot?) {
                    logInfo(TAG, "SuccessfulAdRequest: $inneractiveAdSpot")
                    this@DTExchangeRewarded.inneractiveAdSpot = inneractiveAdSpot
                    setDsp(demandSource ?: inneractiveAdSpot?.mediationNameString)
                    getAd()?.let { ad ->
                        emitEvent(AdEvent.Fill(ad))
                    }
                }

                override fun onInneractiveFailedAdRequest(
                    inneractiveAdSpot: InneractiveAdSpot?,
                    inneractiveErrorCode: InneractiveErrorCode?
                ) {
                    val error = inneractiveErrorCode.asBidonError()
                    logError(TAG, "Error while bidding: $inneractiveErrorCode", error)
                    emitEvent(AdEvent.LoadFailed(error))
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

private const val TAG = "DTExchangeRewarded"