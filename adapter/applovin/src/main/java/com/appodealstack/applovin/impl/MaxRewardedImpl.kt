package com.appodealstack.applovin.impl

import android.app.Activity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import com.applovin.mediation.ads.MaxRewardedAd
import com.appodealstack.applovin.ApplovinDemandId
import com.appodealstack.applovin.MaxFullscreenAdAuctionParams
import com.appodealstack.bidon.data.models.auction.LineItem
import com.appodealstack.bidon.data.models.stats.RoundStatus
import com.appodealstack.bidon.data.models.stats.asRoundStatus
import com.appodealstack.bidon.domain.adapter.AdAuctionParams
import com.appodealstack.bidon.domain.adapter.AdSource
import com.appodealstack.bidon.domain.adapter.AdState
import com.appodealstack.bidon.domain.auction.AuctionResult
import com.appodealstack.bidon.domain.common.*
import com.appodealstack.bidon.domain.stats.StatisticsCollector
import com.appodealstack.bidon.domain.stats.impl.StatisticsCollectorImpl
import com.appodealstack.bidon.domain.stats.impl.logError
import com.appodealstack.bidon.domain.stats.impl.logInternal
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
                    roundStatus = RoundStatus.SuccessfulBid,
                )
                adState.tryEmit(
                    AdState.Bid(
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
                adState.tryEmit(AdState.LoadFailed(error.asBidonError()))
            }

            override fun onAdDisplayed(ad: MaxAd) {
                maxAd = ad
                adState.tryEmit(AdState.Impression(ad.asAd()))
            }

            override fun onAdHidden(ad: MaxAd) {
                maxAd = ad
                adState.tryEmit(AdState.Closed(ad.asAd()))
            }

            override fun onAdClicked(ad: MaxAd) {
                maxAd = ad
                adState.tryEmit(AdState.Clicked(ad.asAd()))
            }

            override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                maxAd = ad
                adState.tryEmit(AdState.ShowFailed(error.asBidonError()))
            }

            override fun onRewardedVideoStarted(ad: MaxAd?) {}
            override fun onRewardedVideoCompleted(ad: MaxAd?) {}

            override fun onUserRewarded(ad: MaxAd, reward: MaxReward?) {
                maxAd = ad
                adState.tryEmit(
                    AdState.OnReward(
                        ad = ad.asAd(),
                        reward = Reward(reward?.label ?: "", reward?.amount ?: 0)
                    )
                )
            }
        }
    }

    override val adState = MutableSharedFlow<AdState>(extraBufferCapacity = Int.MAX_VALUE)

    override val ad: Ad?
        get() = maxAd?.asAd() ?: rewardedAd?.asAd()

    override fun destroy() {
        logInternal(Tag, "destroy")
        rewardedAd?.setListener(null)
        rewardedAd?.destroy()
        rewardedAd = null
        maxAd = null
    }

    override fun getAuctionParams(
        activity: Activity,
        priceFloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        onLineItemConsumed: (LineItem) -> Unit,
    ): Result<AdAuctionParams> = runCatching {
        val lineItem = lineItems.minByOrNull { it.priceFloor }
            ?.also(onLineItemConsumed)
        MaxFullscreenAdAuctionParams(
            lineItem = requireNotNull(lineItem),
            timeoutMs = timeout,
            activity = activity
        )
    }

    override suspend fun bid(adParams: MaxFullscreenAdAuctionParams): AuctionResult {
        logInternal(Tag, "Starting with $adParams")
        markBidStarted(adParams.lineItem.adUnitId)
        val maxInterstitialAd = MaxRewardedAd.getInstance(adParams.lineItem.adUnitId, adParams.activity).also {
            it.setListener(maxAdListener)
            rewardedAd = it
        }
        maxInterstitialAd.loadAd()
        val state = adState.first {
            it is AdState.Bid || it is AdState.LoadFailed
        }
        return when (state) {
            is AdState.LoadFailed -> {
                AuctionResult(
                    ecpm = 0.0,
                    adSource = this
                )
            }
            is AdState.Bid -> state.result
            else -> error("unexpected: $state")
        }
    }

    override suspend fun fill(): Result<Ad> = runCatching {
        /**
         * Applovin fills the bid automatically. It's not needed to fill it manually.
         */
        AdState.Fill(
            requireNotNull(rewardedAd?.asAd())
        ).also { adState.tryEmit(it) }.ad
    }

    override fun show(activity: Activity) {
        if (rewardedAd?.isReady == true) {
            rewardedAd?.showAd()
        } else {
            adState.tryEmit(AdState.ShowFailed(BidonError.FullscreenAdNotReady))
        }
    }

    /**
     * Use it after loaded ECPM is known
     */
    private fun MaxAd?.asAd(): Ad {
        val maxAd = this
        return Ad(
            demandId = ApplovinDemandId,
            demandAd = demandAd,
            price = maxAd?.revenue ?: 0.0,
            sourceAd = maxAd ?: demandAd,
            monetizationNetwork = maxAd?.networkName,
            dsp = maxAd?.dspId,
            roundId = roundId,
            currencyCode = USD,
            auctionId = auctionId,
        )
    }

    /**
     * Use it before loaded ECPM is unknown
     */
    private fun MaxRewardedAd?.asAd(): Ad {
        val maxAd = this
        return Ad(
            demandId = ApplovinDemandId,
            demandAd = demandAd,
            price = 0.0,
            sourceAd = maxAd ?: demandAd,
            monetizationNetwork = null,
            dsp = null,
            roundId = roundId,
            currencyCode = USD,
            auctionId = auctionId,
        )
    }
}

private const val Tag = "Max Rewarded"
private const val USD = "USD"
