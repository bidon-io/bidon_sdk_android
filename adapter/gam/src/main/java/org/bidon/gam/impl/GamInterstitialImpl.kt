package org.bidon.gam.impl

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback
import org.bidon.gam.GamFullscreenAdAuctionParams
import org.bidon.gam.GamInitParameters
import org.bidon.gam.ext.asBidonAdValue
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
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.BidType

internal class GamInterstitialImpl(
    configParams: GamInitParameters?,
    private val getAdRequest: GetAdRequestUseCase = GetAdRequestUseCase(configParams),
    private val getFullScreenContentCallback: GetFullScreenContentCallbackUseCase = GetFullScreenContentCallbackUseCase(),
    private val obtainToken: GetTokenUseCase = GetTokenUseCase(configParams),
    private val obtainAdAuctionParams: GetAdAuctionParamsUseCase = GetAdAuctionParamsUseCase(),
) : AdSource.Interstitial<GamFullscreenAdAuctionParams>,
    Mode.Bidding,
    Mode.Network,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var interstitialAd: AdManagerInterstitialAd? = null
    private var bidType: BidType = BidType.CPM
    private var price: Double? = null

    override val isAdReadyToShow: Boolean
        get() = interstitialAd != null

    override suspend fun getToken(context: Context, adTypeParam: AdTypeParam, adUnits: List<AdUnit>): String? {
        bidType = BidType.RTB
        logInfo(TAG, "getToken: $demandAd")
        return obtainToken(context, demandAd.adType)
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return obtainAdAuctionParams(auctionParamsScope, demandAd.adType, bidType)
    }

    override fun load(adParams: GamFullscreenAdAuctionParams) {
        logInfo(TAG, "Starting with $adParams")
        val adRequest = getAdRequest(adParams)
        price = adParams.price
        val requestListener = object : AdManagerInterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                logInfo(TAG, "onAdFailedToLoad: $loadAdError. $this")
                emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
            }

            override fun onAdLoaded(interstitialAd: AdManagerInterstitialAd) {
                logInfo(TAG, "onAdLoaded: $this")
                this@GamInterstitialImpl.interstitialAd = interstitialAd
                adParams.activity.runOnUiThread {
                    interstitialAd.onPaidEventListener = OnPaidEventListener { adValue ->
                        getAd()?.let {
                            emitEvent(AdEvent.PaidRevenue(it, adValue.asBidonAdValue()))
                        }
                    }
                    interstitialAd.fullScreenContentCallback = getFullScreenContentCallback.createCallback(
                        adEventFlow = this@GamInterstitialImpl,
                        getAd = {
                            getAd()
                        },
                        onClosed = {
                            this@GamInterstitialImpl.interstitialAd = null
                        }
                    )
                    getAd()?.let { emitEvent(AdEvent.Fill(it)) }
                }
            }
        }
        val adUnitId = when (adParams) {
            is GamFullscreenAdAuctionParams.Bidding -> adParams.adUnitId
            is GamFullscreenAdAuctionParams.Network -> adParams.adUnitId
        }
        AdManagerInterstitialAd.load(adParams.activity, adUnitId, adRequest, requestListener)
    }

    override fun show(activity: Activity) {
        logInfo(TAG, "Starting show: $this")
        if (interstitialAd == null) {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        } else {
            interstitialAd?.show(activity)
        }
    }

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        interstitialAd?.onPaidEventListener = null
        interstitialAd?.fullScreenContentCallback = null
        interstitialAd = null
    }
}

private const val TAG = "GamInterstitial"
