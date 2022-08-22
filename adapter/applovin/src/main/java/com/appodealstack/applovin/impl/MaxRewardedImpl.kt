package com.appodealstack.applovin.impl

import android.app.Activity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import com.applovin.mediation.ads.MaxRewardedAd
import com.appodealstack.applovin.ApplovinFullscreenAdAuctionParams
import com.appodealstack.applovin.ApplovinMaxDemandId
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.data.models.LineItem
import com.appodealstack.bidon.core.ext.asFailure
import com.appodealstack.bidon.core.ext.asSuccess
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInternal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

internal class MaxRewardedImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String
) : AdSource.Rewarded<ApplovinFullscreenAdAuctionParams> {

    private var rewardedAd: MaxRewardedAd? = null
    private var maxAd: MaxAd? = null

    private val maxAdListener by lazy {
        object : MaxRewardedAdListener {
            override fun onAdLoaded(ad: MaxAd) {
                maxAd = ad
                state.value = AdState.Bid(
                    AuctionResult(
                        priceFloor = ad.revenue,
                        adSource = this@MaxRewardedImpl
                    )
                )
            }

            override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                logError(Tag, "(code=${error.code}) ${error.message}", error.asBidonError())
                state.value = AdState.LoadFailed(error.asBidonError())
            }

            override fun onAdDisplayed(ad: MaxAd) {
                maxAd = ad
                state.value = AdState.Impression(ad.asAd())
            }

            override fun onAdHidden(ad: MaxAd) {
                maxAd = ad
                state.value = AdState.Closed(ad.asAd())
            }

            override fun onAdClicked(ad: MaxAd) {
                maxAd = ad
                state.value = AdState.Clicked(ad.asAd())
            }

            override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                maxAd = ad
                state.value = AdState.ShowFailed(error.asBidonError())
            }

            override fun onRewardedVideoStarted(ad: MaxAd?) {}
            override fun onRewardedVideoCompleted(ad: MaxAd?) {}

            override fun onUserRewarded(ad: MaxAd, reward: MaxReward?) {
                maxAd = ad
                state.value = AdState.OnReward(
                    ad = ad.asAd(),
                    reward = Reward(reward?.label ?: "", reward?.amount ?: 0)
                )
            }
        }
    }

    override val state = MutableStateFlow<AdState>(AdState.Initialized)

    override val ad: Ad?
        get() = maxAd?.asAd() ?: rewardedAd?.asAd()

    override fun destroy() {
        logInternal(Tag, "destroy")
        rewardedAd?.setListener(null)
        rewardedAd?.destroy()
        rewardedAd = null
        maxAd = null
    }

    override fun getAuctionParams(priceFloor: Double, timeout: Long, lineItems: List<LineItem>): AdAuctionParams {
        return ApplovinFullscreenAdAuctionParams(
            adUnitId = checkNotNull(lineItems.first { it.demandId == demandId.demandId }.adUnitId),
            timeoutMs = timeout
        )
    }

    override suspend fun bid(
        activity: Activity?,
        adParams: ApplovinFullscreenAdAuctionParams
    ): Result<AuctionResult> {
        logInternal(Tag, "Starting with $adParams")
        val maxInterstitialAd = MaxRewardedAd.getInstance(adParams.adUnitId, activity).also {
            it.setListener(maxAdListener)
            rewardedAd = it
        }
        maxInterstitialAd.loadAd()
        val state = state.first {
            it is AdState.Bid || it is AdState.LoadFailed
        }
        return when (state) {
            is AdState.LoadFailed -> state.cause.asFailure()
            is AdState.Bid -> state.result.asSuccess()
            else -> error("unexpected: $state")
        }
    }

    override suspend fun fill(): Result<Ad> = runCatching {
        /**
         * Applovin fills the bid automatically. It's not needed to fill it manually.
         */
        AdState.Fill(
            requireNotNull(rewardedAd?.asAd())
        ).also { state.value = it }.ad
    }

    override fun show(activity: Activity) {
        if (rewardedAd?.isReady == true) {
            rewardedAd?.showAd()
        } else {
            state.value = AdState.ShowFailed(BidonError.FullscreenAdNotReady)
        }
    }

    /**
     * Use it after loaded ECPM is known
     */
    private fun MaxAd?.asAd(): Ad {
        val maxAd = this
        return Ad(
            demandId = ApplovinMaxDemandId,
            demandAd = demandAd,
            price = maxAd?.revenue ?: 0.0,
            sourceAd = maxAd ?: demandAd,
            monetizationNetwork = maxAd?.networkName,
            dsp = maxAd?.dspId,
            roundId = roundId,
            currencyCode = USD
        )
    }

    /**
     * Use it before loaded ECPM is unknown
     */
    private fun MaxRewardedAd?.asAd(): Ad {
        val maxAd = this
        return Ad(
            demandId = ApplovinMaxDemandId,
            demandAd = demandAd,
            price = 0.0,
            sourceAd = maxAd ?: demandAd,
            monetizationNetwork = null,
            dsp = null,
            roundId = roundId,
            currencyCode = USD
        )
    }
}

private const val Tag = "ApplovinMax Rewarded"
private const val USD = "USD"
