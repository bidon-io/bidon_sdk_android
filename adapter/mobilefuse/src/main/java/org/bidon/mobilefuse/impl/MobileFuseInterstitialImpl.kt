package org.bidon.mobilefuse.impl

import android.app.Activity
import android.content.Context
import com.mobilefuse.sdk.AdError
import com.mobilefuse.sdk.MobileFuseInterstitialAd
import kotlinx.coroutines.flow.MutableSharedFlow
import org.bidon.mobilefuse.MobileFuseAuctionParams
import org.bidon.mobilefuse.ext.GetMobileFuseTokenUseCase
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdLoadingType
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.AdValue.Companion.USD
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus

class MobileFuseInterstitialImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val auctionId: String,
    private val isTestMode: Boolean
) :
    AdLoadingType.Bidding<MobileFuseAuctionParams>,
    AdSource.Interstitial<MobileFuseAuctionParams>,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId,
        demandAd = demandAd
    ) {

    private var interstitialAd: MobileFuseInterstitialAd? = null
    override val ad: Ad?
        get() = interstitialAd?.let {
            Ad(
                demandAd = demandAd,
                auctionId = auctionId,
                roundId = roundId,
                currencyCode = it.winningBidInfo?.currency ?: USD,
                demandAdObject = this,
                dsp = null,
                adUnitId = null,
                ecpm = it.winningBidInfo?.cpmPrice?.toDouble() ?: 0.0,
                networkName = demandId.demandId,
            )
        }
    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE, replay = 1)
    override val isAdReadyToShow: Boolean get() = interstitialAd?.isLoaded == true

    override suspend fun getToken(context: Context): String? {
        return GetMobileFuseTokenUseCase(context, isTestMode)
    }

    override fun adRequest(adParams: MobileFuseAuctionParams) {
        logInfo(Tag, "Starting with $adParams: $this")
        // placementId should be configured in the mediation platform UI and passed back to this method:
        val interstitialAd = MobileFuseInterstitialAd(adParams.activity, "").also {
            interstitialAd = it
        }
        interstitialAd.setListener(object : MobileFuseInterstitialAd.Listener {
            override fun onAdLoaded() {
                logInfo(Tag, "onAdLoaded")
                adEvent.tryEmit(
                    AdEvent.Bid(
                        AuctionResult.Network.Success(
                            adSource = this@MobileFuseInterstitialImpl,
                            roundStatus = RoundStatus.Successful
                        )
                    )
                )
            }

            override fun onAdNotFilled() {
                val cause = BidonError.NoFill(demandId)
                logError(Tag, "onAdNotFilled", cause)
                adEvent.tryEmit(AdEvent.LoadFailed(cause))
            }

            override fun onAdRendered() {
                logInfo(Tag, "onAdRendered")
                ad?.let {
                    adEvent.tryEmit(AdEvent.Shown(it))
                    adEvent.tryEmit(
                        AdEvent.PaidRevenue(
                            ad = it,
                            adValue = interstitialAd.winningBidInfo.let { bidInfo ->
                                AdValue(
                                    adRevenue = bidInfo?.cpmPrice?.toDouble() ?: 0.0,
                                    currency = bidInfo?.currency ?: USD,
                                    precision = Precision.Precise
                                )
                            }
                        )
                    )
                }
            }

            override fun onAdClicked() {
                logInfo(Tag, "onAdClicked")
                ad?.let { adEvent.tryEmit(AdEvent.Clicked(it)) }
            }

            override fun onAdExpired() {
                logInfo(Tag, "onAdExpired")
                adEvent.tryEmit(AdEvent.LoadFailed(BidonError.Expired(demandId)))
            }

            override fun onAdError(adError: AdError?) {
                logError(Tag, "onAdError $adError", Throwable(adError?.errorMessage))
                when (adError) {
                    AdError.AD_ALREADY_RENDERED -> {
                        adEvent.tryEmit(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
                    }

                    AdError.AD_RUNTIME_ERROR,
                    AdError.AD_LOAD_ERROR,
                    AdError.AD_ALREADY_LOADED -> {
                        // do nothing
                    }

                    else -> {
                        // do nothing
                    }
                }
            }

            override fun onAdClosed() {
                logInfo(Tag, "onAdClosed: $this")
                ad?.let { adEvent.tryEmit(AdEvent.Closed(it)) }
            }
        })
        interstitialAd.loadAdFromBiddingToken(adParams.payload)
    }

    override fun fill() {
        logInfo(Tag, "Starting fill: $this")
        ad?.let { adEvent.tryEmit(AdEvent.Fill(it)) }
    }

    override fun show(activity: Activity) {
        logInfo(Tag, "Starting show: $this")
        if (interstitialAd == null || interstitialAd?.isLoaded == false) {
            adEvent.tryEmit(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        } else {
            interstitialAd?.showAd()
        }
    }

    override fun destroy() {
        logInfo(Tag, "destroy $this")
        interstitialAd = null
    }

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            MobileFuseAuctionParams(
                activity = activity,
                payload = requireNotNull(payload)
            )
        }
    }
}

private const val Tag = "MobileFuseInterstitialImpl"