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
import org.bidon.sdk.adapter.AdLoadingType
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.vungle.VungleFullscreenAuctionParams
import org.bidon.vungle.ext.asBidonError

/**
 * Created by Aleksei Cherniaev on 03/08/2023.
 */
internal class VungleRewardedImpl :
    AdSource.Rewarded<VungleFullscreenAuctionParams>,
    AdLoadingType.Bidding<VungleFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var adParams: VungleFullscreenAuctionParams? = null

    override fun getToken(context: Context): String? = Vungle.getAvailableBidTokens(context)

    override val isAdReadyToShow: Boolean
        get() = adParams?.let {
            Vungle.canPlayAd(
                it.placementId,
                it.payload
            )
        } ?: false

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            VungleFullscreenAuctionParams(
                placementId = requireNotNull(json?.getString("placement_id")) {
                    "Bid price is required for Bigo Ads"
                },
                price = pricefloor,
                payload = requireNotNull(json?.getString("payload")) {
                    "Payload is required for Bigo Ads"
                }
            )
        }
    }

    override fun adRequest(adParams: VungleFullscreenAuctionParams) {
        this.adParams = adParams
        Vungle.loadAd(
            adParams.placementId, adParams.payload, AdConfig(),
            object : LoadAdCallback {
                override fun onAdLoad(placementId: String?) {
                    emitEvent(
                        AdEvent.Bid(
                            AuctionResult.Bidding(
                                adSource = this@VungleRewardedImpl,
                                roundStatus = RoundStatus.Successful
                            )
                        )
                    )
                }

                override fun onError(placementId: String?, exception: VungleException?) {
                    logError(TAG, "onError placementId=$placementId. $this", exception)
                    emitEvent(AdEvent.LoadFailed(exception.asBidonError()))
                }
            }
        )
    }

    override fun fill() {
        val ad = getAd(this)
        if (adParams != null && ad != null) {
            emitEvent(AdEvent.Fill(ad))
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.BannerAdNotReady))
        }
    }

    override fun show(activity: Activity) {
        val adParams = adParams ?: return
        if (!Vungle.canPlayAd(adParams.placementId, adParams.payload)) {
            emitEvent(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        } else {
            Vungle.playAd(
                adParams.placementId, adParams.payload, AdConfig(),
                object : PlayAdCallback {
                    override fun creativeId(creativeId: String?) {}
                    override fun onAdEnd(placementId: String?, completed: Boolean, isCTAClicked: Boolean) {}
                    override fun onAdEnd(placementId: String?) {
                        logInfo(TAG, "onAdEnd: $this")
                        val ad = getAd(this@VungleRewardedImpl) ?: return
                        emitEvent(AdEvent.Closed(ad))
                    }

                    override fun onAdClick(placementId: String?) {
                        logInfo(TAG, "onAdClick: $this")
                        val ad = getAd(this@VungleRewardedImpl) ?: return
                        emitEvent(AdEvent.Clicked(ad))
                    }

                    override fun onAdRewarded(placementId: String?) {
                        logInfo(TAG, "onAdRewarded: $this")
                        val ad = getAd(this@VungleRewardedImpl) ?: return
                        emitEvent(AdEvent.OnReward(ad, null))
                    }

                    override fun onAdLeftApplication(placementId: String?) {}

                    override fun onError(placementId: String?, exception: VungleException?) {
                        logError(TAG, "onAdError: $this", exception)
                        emitEvent(AdEvent.ShowFailed(exception.asBidonError()))
                    }

                    override fun onAdStart(placementId: String?) {
                        logInfo(TAG, "onAdStart: $this")
                        val ad = getAd(this@VungleRewardedImpl) ?: return
                        emitEvent(AdEvent.Shown(ad))
                    }

                    override fun onAdViewed(placementId: String?) {
                        logInfo(TAG, "onAdViewed: $this")
                        val ad = getAd(this@VungleRewardedImpl) ?: return
                        emitEvent(
                            AdEvent.PaidRevenue(
                                ad = ad,
                                adValue = AdValue(
                                    adRevenue = adParams.price,
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

private const val TAG = "VungleRewardedImpl"