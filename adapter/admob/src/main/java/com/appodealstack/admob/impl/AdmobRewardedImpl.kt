package com.appodealstack.admob.impl

import android.app.Activity
import com.appodealstack.admob.AdmobFullscreenAdAuctionParams
import com.appodealstack.admob.asBidonError
import com.appodealstack.bidon.data.models.auction.LineItem
import com.appodealstack.bidon.data.models.auction.minByPricefloorOrNull
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
import com.appodealstack.bidon.view.Reward
import com.appodealstack.bidon.view.helper.SdkDispatchers
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

// $0.1 ca-app-pub-9630071911882835/9299488830
// $0.5 ca-app-pub-9630071911882835/4234864416
// $1.0 ca-app-pub-9630071911882835/7790966049
// $2.0 ca-app-pub-9630071911882835/1445049547

@Suppress("unused")
internal class AdmobRewardedImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val auctionId: String
) : AdSource.Rewarded<AdmobFullscreenAdAuctionParams>,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId
    ) {

    private val dispatcher: CoroutineDispatcher = SdkDispatchers.Main

    private var lineItem: LineItem? = null
    private var rewardedAd: RewardedAd? = null
    private val requiredRewardedAd: RewardedAd get() = requireNotNull(rewardedAd)

    private val requestListener by lazy {
        object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                logInternal(Tag, "Error while loading ad: $loadAdError. $this", loadAdError.asBidonError())
                markBidFinished(
                    ecpm = null,
                    roundStatus = loadAdError.asBidonError().asRoundStatus(),
                )
                adState.tryEmit(AdState.LoadFailed(loadAdError.asBidonError()))
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                logInternal(Tag, "onAdLoaded: $this")
                this@AdmobRewardedImpl.rewardedAd = rewardedAd
                requiredRewardedAd.onPaidEventListener = paidListener
                requiredRewardedAd.fullScreenContentCallback = rewardedListener
                markBidFinished(
                    ecpm = requireNotNull(lineItem?.priceFloor),
                    roundStatus = RoundStatus.Successful,
                )
                adState.tryEmit(
                    AdState.Bid(
                        AuctionResult(
                            ecpm = requireNotNull(lineItem?.priceFloor),
                            adSource = this@AdmobRewardedImpl,
                        )
                    )
                )
            }
        }
    }
    private val onUserEarnedRewardListener by lazy {
        OnUserEarnedRewardListener { rewardItem ->
            logInternal(Tag, "onUserEarnedReward $rewardItem: $this")
            adState.tryEmit(
                AdState.OnReward(
                    ad = requiredRewardedAd.asAd(),
                    reward = Reward(rewardItem.type, rewardItem.amount)
                )
            )
        }
    }

    /**
     * @see [https://developers.google.com/android/reference/com/google/android/gms/ads/OnPaidEventListener]
     */
    private val paidListener by lazy {
        OnPaidEventListener { adValue ->
            val type = when (adValue.precisionType) {
                0 -> "UNKNOWN"
                1 -> "PRECISE"
                2 -> "ESTIMATED"
                3 -> "PUBLISHER_PROVIDED"
                else -> "unknown type ${adValue.precisionType}"
            }
            val ecpm = adValue.valueMicros / 1_000_000.0
            adState.tryEmit(
                AdState.PaidRevenue(
                    ad = Ad(
                        demandId = demandId,
                        demandAd = demandAd,
                        price = ecpm,
                        sourceAd = requiredRewardedAd,
                        monetizationNetwork = demandId.demandId,
                        dsp = null,
                        roundId = roundId,
                        currencyCode = "USD",
                        auctionId = auctionId,
                    )
                )
            )
        }
    }

    private val rewardedListener by lazy {
        object : FullScreenContentCallback() {
            override fun onAdClicked() {
                logInternal(Tag, "onAdClicked: $this")
                adState.tryEmit(AdState.Clicked(requiredRewardedAd.asAd()))
            }

            override fun onAdDismissedFullScreenContent() {
                logInternal(Tag, "onAdDismissedFullScreenContent: $this")
                adState.tryEmit(AdState.Closed(requiredRewardedAd.asAd()))
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                logError(Tag, "onAdFailedToShowFullScreenContent: $this", error.asBidonError())
                adState.tryEmit(AdState.ShowFailed(error.asBidonError()))
            }

            override fun onAdImpression() {
                logInternal(Tag, "onAdShown: $this")
                adState.tryEmit(AdState.Impression(requiredRewardedAd.asAd()))
            }

            override fun onAdShowedFullScreenContent() {}
        }
    }

    override val ad: Ad?
        get() = rewardedAd?.asAd()

    override val adState = MutableSharedFlow<AdState>(extraBufferCapacity = Int.MAX_VALUE)

    override fun destroy() {
        logInternal(Tag, "destroy $this")
        rewardedAd?.onPaidEventListener = null
        rewardedAd?.fullScreenContentCallback = null
        rewardedAd = null
        lineItem = null
    }

    override fun getAuctionParams(
        activity: Activity,
        priceFloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        onLineItemConsumed: (LineItem) -> Unit,
    ): Result<AdAuctionParams> = runCatching {
        val lineItem = lineItems
            .minByPricefloorOrNull(demandId, priceFloor)
            ?.also(onLineItemConsumed)
        AdmobFullscreenAdAuctionParams(
            lineItem = lineItem ?: error(BidonError.NoAppropriateAdUnitId),
            priceFloor = priceFloor,
            context = activity.applicationContext
        )
    }

    override suspend fun bid(adParams: AdmobFullscreenAdAuctionParams): AuctionResult {
        logInternal(Tag, "Starting with $adParams: $this")
        markBidStarted(adParams.lineItem.adUnitId)
        return withContext(dispatcher) {
            lineItem = adParams.lineItem
            val adRequest = AdRequest.Builder().build()
            val adUnitId = lineItem?.adUnitId
            if (!adUnitId.isNullOrBlank()) {
                RewardedAd.load(adParams.context, adUnitId, adRequest, requestListener)
            } else {
                val error = BidonError.NoAppropriateAdUnitId
                logError(
                    tag = Tag,
                    message = "No appropriate AdUnitId found. PriceFloor=${adParams.priceFloor}, " +
                        "but LineItem with max priceFloor=${lineItem?.priceFloor}",
                    error = error
                )
                adState.tryEmit(AdState.LoadFailed(error))
            }
            val state = adState.first {
                it is AdState.Bid || it is AdState.LoadFailed
            }
            when (state) {
                is AdState.LoadFailed -> {
                    AuctionResult(
                        ecpm = 0.0,
                        adSource = this@AdmobRewardedImpl
                    )
                }
                is AdState.Bid -> state.result
                else -> error("unexpected: $state")
            }
        }
    }

    override suspend fun fill(): Result<Ad> = runCatching {
        logInternal(Tag, "Starting fill: $this")
        markFillStarted()
        /**
         * Admob fills the bid automatically. It's not needed to fill it manually.
         */
        AdState.Fill(
            requiredRewardedAd.asAd()
        ).also {
            markFillFinished(RoundStatus.Successful)
            adState.tryEmit(it)
        }.ad
    }

    override fun show(activity: Activity) {
        logInternal(Tag, "Starting show: $this")
        if (rewardedAd == null) {
            adState.tryEmit(AdState.ShowFailed(BidonError.FullscreenAdNotReady))
        } else {
            rewardedAd?.show(activity, onUserEarnedRewardListener)
        }
    }

    private fun RewardedAd.asAd(): Ad {
        return Ad(
            demandId = demandId,
            demandAd = demandAd,
            price = lineItem?.priceFloor ?: 0.0,
            sourceAd = this,
            monetizationNetwork = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = "USD",
            auctionId = auctionId,
        )
    }
}

private const val Tag = "Admob Rewarded"
