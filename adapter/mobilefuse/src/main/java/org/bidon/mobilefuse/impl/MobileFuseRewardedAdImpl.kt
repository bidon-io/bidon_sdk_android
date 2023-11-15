package org.bidon.mobilefuse.impl

import android.app.Activity
import android.content.Context
import com.mobilefuse.sdk.AdError
import com.mobilefuse.sdk.MobileFuseRewardedAd
import kotlinx.coroutines.flow.MutableSharedFlow
import org.bidon.mobilefuse.ext.GetMobileFuseTokenUseCase
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
import java.util.concurrent.atomic.AtomicBoolean

class MobileFuseRewardedAdImpl(private val isTestMode: Boolean) :
    AdSource.Rewarded<MobileFuseFullscreenAuctionParams>,
    Mode.Bidding,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var rewardedAd: MobileFuseRewardedAd? = null

    /**
     * This flag is used to prevent [AdError]-callback from being exposed twice.
     */
    private var isLoaded = AtomicBoolean(false)

    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE, replay = 1)
    override val isAdReadyToShow: Boolean get() = rewardedAd?.isLoaded == true

    override suspend fun getToken(context: Context, adTypeParam: AdTypeParam): String? {
        return GetMobileFuseTokenUseCase(context, isTestMode)
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return ObtainAuctionParamUseCase().getFullscreenParam(auctionParamsScope)
    }

    override fun load(adParams: MobileFuseFullscreenAuctionParams) {
        logInfo(Tag, "Starting with $adParams: $this")
        // placementId should be configured in the mediation platform UI and passed back to this method:
        val rewardedAd = MobileFuseRewardedAd(adParams.activity, adParams.placementId).also {
            rewardedAd = it
        }
        rewardedAd.setListener(object : MobileFuseRewardedAd.Listener {
            override fun onAdLoaded() {
                if (!isLoaded.getAndSet(true)) {
                    logInfo(Tag, "onAdLoaded")
                    getAd()?.let { adEvent.tryEmit(AdEvent.Fill(it)) }
                }
            }

            override fun onAdNotFilled() {
                val cause = BidonError.NoFill(demandId)
                logInfo(Tag, "onAdNotFilled $this")
                adEvent.tryEmit(AdEvent.LoadFailed(cause))
            }

            override fun onAdRendered() {
                logInfo(Tag, "onAdRendered")
                getAd()?.let {
                    adEvent.tryEmit(AdEvent.Shown(it))
                    adEvent.tryEmit(
                        AdEvent.PaidRevenue(
                            ad = it,
                            adValue = rewardedAd.winningBidInfo.let { bidInfo ->
                                AdValue(
                                    adRevenue = bidInfo?.cpmPrice?.toDouble() ?: 0.0,
                                    currency = bidInfo?.currency ?: AdValue.USD,
                                    precision = Precision.Precise
                                )
                            }
                        )
                    )
                }
            }

            override fun onAdClicked() {
                logInfo(Tag, "onAdClicked")
                getAd()?.let { adEvent.tryEmit(AdEvent.Clicked(it)) }
            }

            override fun onAdExpired() {
                logInfo(Tag, "onAdExpired")
                adEvent.tryEmit(AdEvent.LoadFailed(BidonError.Expired(demandId)))
            }

            override fun onAdError(adError: AdError?) {
                logError(Tag, "onAdError $adError", Throwable(adError?.errorMessage))
                when (adError) {
                    AdError.AD_ALREADY_RENDERED -> {
                        adEvent.tryEmit(AdEvent.ShowFailed(BidonError.AdNotReady))
                    }

                    AdError.AD_ALREADY_LOADED,
                    AdError.AD_RUNTIME_ERROR -> {
                        // do nothing
                    }

                    AdError.AD_LOAD_ERROR -> {
                        if (!isLoaded.getAndSet(true)) {
                            getAd()?.let { adEvent.tryEmit(AdEvent.LoadFailed(BidonError.NoFill(demandId))) }
                        }
                    }

                    else -> {
                        // do nothing
                    }
                }
            }

            override fun onAdClosed() {
                logInfo(Tag, "onAdClosed: $this")
                getAd()?.let { adEvent.tryEmit(AdEvent.Closed(it)) }
                this@MobileFuseRewardedAdImpl.rewardedAd = null
            }

            override fun onUserEarnedReward() {
                logInfo(Tag, "onUserEarnedReward: $this")
                getAd()?.let {
                    adEvent.tryEmit(AdEvent.OnReward(ad = it, reward = null))
                }
            }
        })
        rewardedAd.loadAdFromBiddingToken(adParams.signalData)
    }

    override fun show(activity: Activity) {
        logInfo(Tag, "Starting show: $this")
        if (rewardedAd?.isLoaded == true) {
            rewardedAd?.showAd()
        } else {
            adEvent.tryEmit(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }

    override fun destroy() {
        logInfo(Tag, "destroy $this")
        rewardedAd = null
    }
}

private const val Tag = "MobileFuseRewardedAdImpl"