package org.bidon.vungle.impl

import android.app.Activity
import android.content.Context
import com.vungle.warren.AdConfig
import com.vungle.warren.LoadAdCallback
import com.vungle.warren.PlayAdCallback
import com.vungle.warren.Vungle
import com.vungle.warren.error.VungleException
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.vungle.VungleFullscreenAuctionParams
import org.bidon.vungle.ext.asBidonError

/**
 * Created by Aleksei Cherniaev on 03/08/2023.
 */
internal class VungleInterstitialImpl :
    AdSource.Interstitial<VungleFullscreenAuctionParams>,
    Mode.Bidding,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var adParams: VungleFullscreenAuctionParams? = null

    override suspend fun getToken(context: Context, adTypeParam: AdTypeParam, adUnits: List<AdUnit>): String? = Vungle.getAvailableBidTokens(context)

    override val isAdReadyToShow: Boolean
        get() = adParams?.let {
            Vungle.canPlayAd(
                it.placementId,
                it.payload
            )
        } ?: false

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            VungleFullscreenAuctionParams(
                bidResponse = requiredBidResponse
            )
        }
    }

    override fun load(adParams: VungleFullscreenAuctionParams) {
        this.adParams = adParams
        Vungle.loadAd(
            adParams.placementId, adParams.payload, AdConfig(),
            object : LoadAdCallback {
                override fun onAdLoad(placementId: String?) {
                    val ad = getAd()
                    if (ad != null) {
                        emitEvent(AdEvent.Fill(ad))
                    } else {
                        emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
                    }
                }

                override fun onError(placementId: String?, exception: VungleException?) {
                    logInfo(TAG, "onError placementId=$placementId. $this")
                    emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
                }
            }
        )
    }

    override fun show(activity: Activity) {
        val adParams = adParams ?: return
        if (!Vungle.canPlayAd(adParams.placementId, adParams.payload)) {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        } else {
            Vungle.playAd(
                adParams.placementId, adParams.payload, AdConfig(),
                object : PlayAdCallback {
                    override fun creativeId(creativeId: String?) {}
                    @Deprecated("Deprecated in Java")
                    override fun onAdEnd(placementId: String?, completed: Boolean, isCTAClicked: Boolean) {}
                    override fun onAdEnd(placementId: String?) {
                        logInfo(TAG, "onAdEnd: $this")
                        val ad = getAd() ?: return
                        emitEvent(AdEvent.Closed(ad))
                    }

                    override fun onAdClick(placementId: String?) {
                        logInfo(TAG, "onAdClick: $this")
                        val ad = getAd() ?: return
                        emitEvent(AdEvent.Clicked(ad))
                    }

                    override fun onAdRewarded(placementId: String?) {}

                    override fun onAdLeftApplication(placementId: String?) {}

                    override fun onError(placementId: String?, exception: VungleException?) {
                        logError(TAG, "onAdError: $this", exception)
                        emitEvent(AdEvent.ShowFailed(exception.asBidonError()))
                    }

                    override fun onAdStart(placementId: String?) {
                        logInfo(TAG, "onAdStart: $this")
                        val ad = getAd() ?: return
                        emitEvent(AdEvent.Shown(ad))
                    }

                    override fun onAdViewed(placementId: String?) {
                        logInfo(TAG, "onAdViewed: $this")
                        val ad = getAd() ?: return
                        emitEvent(
                            AdEvent.PaidRevenue(
                                ad = ad,
                                adValue = AdValue(
                                    adRevenue = adParams.price / 1000.0,
                                    precision = Precision.Precise,
                                    currency = AdValue.USD,
                                )
                            )
                        )
                    }
                }
            )
        }
    }

    override fun destroy() {
        adParams = null
    }
}

private const val TAG = "VungleInterstitialImpl"