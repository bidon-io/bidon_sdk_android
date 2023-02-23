package org.bidon.applovin.impl

import android.app.Activity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import com.applovin.mediation.ads.MaxRewardedAd
import org.bidon.applovin.ApplovinDemandId
import org.bidon.applovin.MaxFullscreenAdAuctionParams
import org.bidon.applovin.ext.asBidonAdValue
import org.bidon.sdk.adapter.*
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.rewarded.Reward
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.stats.models.asRoundStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

internal class MaxRewardedImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val auctionId: String
) : AdSource.Rewarded<MaxFullscreenAdAuctionParams>,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId
    ) {

    private var rewardedAd: MaxRewardedAd? = null
    private var maxAd: MaxAd? = null

    private val maxAdListener by lazy {
        object : MaxRewardedAdListener {
            override fun onAdLoaded(ad: MaxAd) {
                maxAd = ad
                markBidFinished(
                    ecpm = requireNotNull(ad.revenue),
                    roundStatus = RoundStatus.Successful,
                )
                adEvent.tryEmit(
                    AdEvent.Bid(
                        AuctionResult(
                            ecpm = ad.revenue,
                            adSource = this@MaxRewardedImpl,
                        )
                    )
                )
            }

            override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                logError(Tag, "(code=${error.code}) ${error.message}", error.asBidonError())
                markBidFinished(
                    ecpm = null,
                    roundStatus = error.asBidonError().asRoundStatus(),
                )
                adEvent.tryEmit(AdEvent.LoadFailed(error.asBidonError()))
            }

            override fun onAdDisplayed(ad: MaxAd) {
                maxAd = ad
                adEvent.tryEmit(AdEvent.Shown(ad.asAd()))
                adEvent.tryEmit(
                    AdEvent.PaidRevenue(
                        ad = ad.asAd(),
                        adValue = ad.asBidonAdValue()
                    )
                )
            }

            override fun onAdHidden(ad: MaxAd) {
                maxAd = ad
                adEvent.tryEmit(AdEvent.Closed(ad.asAd()))
            }

            override fun onAdClicked(ad: MaxAd) {
                maxAd = ad
                adEvent.tryEmit(AdEvent.Clicked(ad.asAd()))
            }

            override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                maxAd = ad
                adEvent.tryEmit(AdEvent.ShowFailed(error.asBidonError()))
            }

            @Deprecated("Deprecated in Java")
            override fun onRewardedVideoStarted(ad: MaxAd?) {}
            @Deprecated("Deprecated in Java")
            override fun onRewardedVideoCompleted(ad: MaxAd?) {}

            override fun onUserRewarded(ad: MaxAd, reward: MaxReward?) {
                maxAd = ad
                adEvent.tryEmit(
                    AdEvent.OnReward(
                        ad = ad.asAd(),
                        reward = Reward(reward?.label ?: "", reward?.amount ?: 0)
                    )
                )
            }
        }
    }

    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE)

    override val isAdReadyToShow: Boolean
        get() = rewardedAd?.isReady == true

    override val ad: Ad?
        get() = maxAd?.asAd() ?: rewardedAd?.asAd()

    override fun destroy() {
        logInfo(Tag, "destroy")
        rewardedAd?.setListener(null)
        rewardedAd?.destroy()
        rewardedAd = null
        maxAd = null
    }

    override fun getAuctionParams(
        activity: Activity,
        pricefloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        onLineItemConsumed: (LineItem) -> Unit,
    ): Result<AdAuctionParams> = runCatching {
        val lineItem = lineItems.minByOrNull { it.pricefloor }
            ?.also(onLineItemConsumed)
        MaxFullscreenAdAuctionParams(
            lineItem = requireNotNull(lineItem),
            timeoutMs = timeout,
            activity = activity
        )
    }

    override suspend fun bid(adParams: MaxFullscreenAdAuctionParams): AuctionResult {
        logInfo(Tag, "Starting with $adParams")
        markBidStarted(adParams.lineItem.adUnitId)
        val maxInterstitialAd = MaxRewardedAd.getInstance(adParams.lineItem.adUnitId, adParams.activity).also {
            it.setListener(maxAdListener)
            rewardedAd = it
        }
        maxInterstitialAd.loadAd()
        val state = adEvent.first {
            it is AdEvent.Bid || it is AdEvent.LoadFailed
        }
        return when (state) {
            is AdEvent.LoadFailed -> {
                AuctionResult(
                    ecpm = 0.0,
                    adSource = this
                )
            }
            is AdEvent.Bid -> state.result
            else -> error("unexpected: $state")
        }
    }

    override suspend fun fill(): Result<Ad> = runCatching {
        /**
         * Applovin fills the bid automatically. It's not needed to fill it manually.
         */
        AdEvent.Fill(
            requireNotNull(rewardedAd?.asAd())
        ).also { adEvent.tryEmit(it) }.ad
    }

    override fun show(activity: Activity) {
        if (rewardedAd?.isReady == true) {
            rewardedAd?.showAd()
        } else {
            adEvent.tryEmit(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        }
    }

    /**
     * Use it after loaded ECPM is known
     */
    private fun MaxAd?.asAd(): Ad {
        val maxAd = this
        return Ad(
            demandAd = demandAd,
            eCPM = maxAd?.revenue ?: 0.0,
            sourceAd = maxAd ?: demandAd,
            networkName = maxAd?.networkName,
            dsp = maxAd?.dspId,
            roundId = roundId,
            currencyCode = AdValue.DefaultCurrency,
            auctionId = auctionId,
            adUnitId = this?.adUnitId
        )
    }

    /**
     * Use it before loaded ECPM is unknown
     */
    private fun MaxRewardedAd?.asAd(): Ad {
        val maxAd = this
        return Ad(
            demandAd = demandAd,
            eCPM = 0.0,
            sourceAd = maxAd ?: demandAd,
            networkName = ApplovinDemandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = AdValue.DefaultCurrency,
            auctionId = auctionId,
            adUnitId = this?.adUnitId
        )
    }
}

private const val Tag = "Max Rewarded"