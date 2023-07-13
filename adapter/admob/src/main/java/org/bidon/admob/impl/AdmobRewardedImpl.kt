package org.bidon.admob.impl

import android.app.Activity
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableSharedFlow
import org.bidon.admob.AdmobFullscreenAdAuctionParams
import org.bidon.admob.asBidonError
import org.bidon.admob.ext.asBidonAdValue
import org.bidon.admob.ext.asBundle
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.*
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.rewarded.Reward
import org.bidon.sdk.auction.models.minByPricefloorOrNull
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

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
    AdLoadingType.Network<AdmobFullscreenAdAuctionParams>,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId,
        demandAd = demandAd,
    ) {

    private var param: AdmobFullscreenAdAuctionParams? = null
    private var rewardedAd: RewardedAd? = null
    private val requiredRewardedAd: RewardedAd get() = requireNotNull(rewardedAd)

    private val onUserEarnedRewardListener by lazy {
        OnUserEarnedRewardListener { rewardItem ->
            logInfo(Tag, "onUserEarnedReward $rewardItem: $this")
            adEvent.tryEmit(
                AdEvent.OnReward(
                    ad = requiredRewardedAd.asAd(),
                    reward = Reward(rewardItem.type, rewardItem.amount)
                )
            )
            sendRewardImpression()
        }
    }

    /**
     * @see [https://developers.google.com/android/reference/com/google/android/gms/ads/OnPaidEventListener]
     */
    private val paidListener by lazy {
        OnPaidEventListener { adValue ->
            adEvent.tryEmit(
                AdEvent.PaidRevenue(
                    ad = Ad(
                        demandAd = demandAd,
                        ecpm = param?.lineItem?.pricefloor ?: 0.0,
                        demandAdObject = requiredRewardedAd,
                        networkName = demandId.demandId,
                        dsp = null,
                        roundId = roundId,
                        currencyCode = "USD",
                        auctionId = auctionId,
                        adUnitId = param?.lineItem?.adUnitId
                    ),
                    adValue = adValue.asBidonAdValue()
                )
            )
        }
    }

    private val rewardedListener by lazy {
        object : FullScreenContentCallback() {
            override fun onAdClicked() {
                logInfo(Tag, "onAdClicked: $this")
                adEvent.tryEmit(AdEvent.Clicked(requiredRewardedAd.asAd()))
            }

            override fun onAdDismissedFullScreenContent() {
                logInfo(Tag, "onAdDismissedFullScreenContent: $this")
                adEvent.tryEmit(AdEvent.Closed(requiredRewardedAd.asAd()))
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                logError(Tag, "onAdFailedToShowFullScreenContent: $this", error.asBidonError())
                adEvent.tryEmit(AdEvent.ShowFailed(error.asBidonError()))
            }

            override fun onAdImpression() {
                logInfo(Tag, "onAdShown: $this")
                adEvent.tryEmit(AdEvent.Shown(requiredRewardedAd.asAd()))
            }

            override fun onAdShowedFullScreenContent() {}
        }
    }

    override val ad: Ad?
        get() = rewardedAd?.asAd()

    override val adEvent =
        MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE, replay = 1)
    override val isAdReadyToShow: Boolean
        get() = rewardedAd != null

    override fun destroy() {
        logInfo(Tag, "destroy $this")
        rewardedAd?.onPaidEventListener = null
        rewardedAd?.fullScreenContentCallback = null
        rewardedAd = null
        param = null
    }

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            val lineItem = lineItems
                .minByPricefloorOrNull(demandId, pricefloor)
                ?.also(onLineItemConsumed)
            AdmobFullscreenAdAuctionParams(
                lineItem = lineItem ?: error(BidonError.NoAppropriateAdUnitId),
                pricefloor = pricefloor,
                context = activity.applicationContext
            )
        }
    }

    override fun fill(adParams: AdmobFullscreenAdAuctionParams) {
        logInfo(Tag, "Starting with $adParams: $this")
        param = adParams
        val adRequest = AdRequest.Builder()
            .addNetworkExtrasBundle(AdMobAdapter::class.java, BidonSdk.regulation.asBundle())
            .build()
        val adUnitId = param?.lineItem?.adUnitId
        if (!adUnitId.isNullOrBlank()) {
            val requestListener = object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    logError(
                        Tag,
                        "Error while loading ad. LoadAdError=$loadAdError.\n$this",
                        loadAdError.asBidonError()
                    )
                    adEvent.tryEmit(AdEvent.LoadFailed(loadAdError.asBidonError()))
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    logInfo(Tag, "onAdLoaded. RewardedAd=$rewardedAd, $this")
                    this@AdmobRewardedImpl.rewardedAd = rewardedAd
                    requiredRewardedAd.onPaidEventListener = paidListener
                    requiredRewardedAd.fullScreenContentCallback = rewardedListener
                    adEvent.tryEmit(AdEvent.Fill(requiredRewardedAd.asAd()))
                }
            }
            RewardedAd.load(adParams.context, adUnitId, adRequest, requestListener)
        } else {
            val error = BidonError.NoAppropriateAdUnitId
            logError(
                tag = Tag,
                message = "No appropriate AdUnitId found. PriceFloor=${adParams.pricefloor}, " +
                    "but LineItem with max pricefloor=${param?.lineItem?.pricefloor}",
                error = error
            )
            adEvent.tryEmit(AdEvent.LoadFailed(error))
        }
    }

    override fun show(activity: Activity) {
        logInfo(Tag, "Starting show: $this")
        if (rewardedAd == null) {
            adEvent.tryEmit(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        } else {
            rewardedAd?.show(activity, onUserEarnedRewardListener)
        }
    }

    private fun RewardedAd.asAd(): Ad {
        return Ad(
            demandAd = demandAd,
            ecpm = param?.lineItem?.pricefloor ?: 0.0,
            demandAdObject = this,
            networkName = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = "USD",
            auctionId = auctionId,
            adUnitId = param?.lineItem?.adUnitId
        )
    }
}

private const val Tag = "Admob Rewarded"
