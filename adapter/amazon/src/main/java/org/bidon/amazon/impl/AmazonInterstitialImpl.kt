package org.bidon.amazon.impl

import android.app.Activity
import android.content.Context
import android.view.View
import com.amazon.device.ads.DTBAdInterstitial
import com.amazon.device.ads.DTBAdInterstitialListener
import com.amazon.device.ads.SDKUtilities
import org.bidon.amazon.SlotType
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.json.JSONArray
import org.json.JSONObject

internal class AmazonInterstitialImpl(
    private val slots: Map<SlotType, List<String>>
) : AdSource.Interstitial<FullscreenAuctionParams>,
    Mode.Bidding,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private val obtainToken: ObtainTokenUseCase get() = ObtainTokenUseCase()
    private var amazonInfos = mutableListOf<AmazonInfo>()
    private var interstitial: DTBAdInterstitial? = null

    override val isAdReadyToShow: Boolean
        get() = interstitial != null

    override suspend fun getToken(context: Context, adTypeParam: AdTypeParam): String? {
        val amazonInfo = obtainToken(slots, adTypeParam).takeIf { it.isNotEmpty() }?.also {
            this.amazonInfos.addAll(it)
        } ?: return null
        return JSONArray().apply {
            amazonInfo.map {
                it.adSizes.slotUUID to SDKUtilities.getPricePoint(it.dtbAdResponse)
            }.forEach { (slotUuid, pricePoint) ->
                this.put(
                    JSONObject().apply {
                        this.put("slot_uuid", slotUuid)
                        this.put("price_point", pricePoint)
                    }
                )
            }
        }.toString()
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            FullscreenAuctionParams(
                activity = activity,
                slotUuid = requireNotNull(json?.optString("slot_uuid")) {
                    "SlotUid is required for Amazon banner ad"
                },
                price = pricefloor
            )
        }
    }

    override fun load(adParams: FullscreenAuctionParams) {
        if (amazonInfos.isEmpty()) {
            logError(TAG, "No Amazon slot found", BidonError.NoAppropriateAdUnitId)
            emitEvent(AdEvent.LoadFailed(BidonError.NoAppropriateAdUnitId))
            return
        }
        val dtbAdResponse = amazonInfos.firstOrNull { adParams.slotUuid == it.adSizes.slotUUID }?.dtbAdResponse
        if (dtbAdResponse == null) {
            logError(TAG, "DTBAdResponse is null", BidonError.NoBid)
            emitEvent(AdEvent.LoadFailed(BidonError.NoBid))
            return
        }
        val interstitialAd = DTBAdInterstitial(
            adParams.activity,
            object : DTBAdInterstitialListener {
                override fun onAdLoaded(p0: View?) {
                    logInfo(TAG, "onAdLoaded")
                    emitEvent(AdEvent.Fill(getAd() ?: return))
                }

                override fun onAdFailed(view: View?) {
                    logError(TAG, "onAdFailed", BidonError.NoFill(demandId))
                    emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
                }

                override fun onAdClicked(view: View?) {
                    logInfo(TAG, "onAdClicked")
                    emitEvent(AdEvent.Clicked(getAd() ?: return))
                }

                override fun onAdLeftApplication(view: View?) {}
                override fun onAdOpen(view: View?) {}

                override fun onAdClosed(view: View?) {
                    logInfo(TAG, "onAdClosed")
                    emitEvent(AdEvent.Closed(getAd() ?: return))
                    interstitial = null
                }

                override fun onImpressionFired(view: View?) {
                    logInfo(TAG, "onImpressionFired")
                    getAd()?.let {
                        emitEvent(AdEvent.Shown(it))
                        emitEvent(
                            AdEvent.PaidRevenue(
                                ad = it,
                                adValue = AdValue(
                                    adRevenue = adParams.price,
                                    currency = AdValue.USD,
                                    Precision.Precise
                                )
                            )
                        )
                    }
                }

                override fun onVideoCompleted(view: View?) {
                    super.onVideoCompleted(view)
                    logInfo(TAG, "onVideoCompleted")
                }
            }
        ).also {
            interstitial = it
        }
        val bidInfo = SDKUtilities.getBidInfo(dtbAdResponse)
        interstitialAd.fetchAd(bidInfo)
    }

    override fun show(activity: Activity) {
        val interstitial = interstitial
        if (interstitial == null) {
            logError(TAG, "Interstitial is null", BidonError.AdNotReady)
            emitEvent(AdEvent.LoadFailed(BidonError.AdNotReady))
            return
        }
        interstitial.show()
    }

    override fun destroy() {
        interstitial = null
        amazonInfos.clear()
    }
}

private const val TAG = "AmazonInterstitialImpl"