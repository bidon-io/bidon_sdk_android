package com.appodealstack.fyber

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.appodealstack.bidon.SdkCore
import com.appodealstack.bidon.analytics.BNMediationNetwork
import com.appodealstack.bidon.analytics.MediationNetwork
import com.appodealstack.bidon.auctions.AuctionRequest
import com.appodealstack.bidon.auctions.AuctionResult
import com.appodealstack.bidon.config.domain.AdapterInfo
import com.appodealstack.bidon.demands.*
import com.appodealstack.fyber.banner.BannerInterceptor
import com.appodealstack.fyber.banner.initBannerListener
import com.appodealstack.fyber.interstitial.InterstitialInterceptor
import com.appodealstack.fyber.interstitial.initInterstitialListener
import com.appodealstack.fyber.rewarded.RewardedInterceptor
import com.appodealstack.fyber.rewarded.initRewardedListener
import com.fyber.FairBid
import com.fyber.fairbid.ads.*
import com.fyber.fairbid.ads.banner.BannerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val FairBidDemandId = DemandId("fair_bid")

class FairBidAdapter : Adapter, Initializable<FairBidParameters>,
    AdSource.Interstitial, AdSource.Rewarded, AdSource.Banner, MediationNetwork {
    override val mediationNetwork = BNMediationNetwork.Fyber
    override val demandId: DemandId = FairBidDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = "3.2.1",
        bidonSdkVersion = "1.2.3"
    )

    private lateinit var context: Context
    private val scope: CoroutineScope
        get() = CoroutineScope(Dispatchers.Default)

    private val interstitialInterceptorFlow = MutableSharedFlow<InterstitialInterceptor>(extraBufferCapacity = Int.MAX_VALUE)
    private val interstitialPlacementsDemandAd = mutableMapOf<String, DemandAd>()

    private val rewardedInterceptorFlow = MutableSharedFlow<RewardedInterceptor>(extraBufferCapacity = Int.MAX_VALUE)
    private val rewardedPlacementsDemandAd = mutableMapOf<String, DemandAd>()

    private val bannerInterceptorFlow = MutableSharedFlow<BannerInterceptor>(extraBufferCapacity = Int.MAX_VALUE)
    private val bannerPlacementsDemandAd = mutableMapOf<String, DemandAd>()
    private val bannerPlacementsRevenue = mutableMapOf<String, ImpressionData>()
    private val placements = mutableListOf<String>()

    private var fairBidParameters: FairBidParameters? = null

    init {
        scope.launch {
            interstitialInterceptorFlow.collect { interceptor ->
                proceedInterstitialCallbacks(interceptor)
            }
        }
        scope.launch {
            rewardedInterceptorFlow.collect { interceptor ->
                proceedRewardedCallbacks(interceptor)
            }
        }
        scope.launch {
            bannerInterceptorFlow.collect { interceptor ->
                proceedBannerCallbacks(interceptor)
            }
        }
    }

    override suspend fun init(activity: Activity, configParams: FairBidParameters) {
        this.context = activity.applicationContext
        this.fairBidParameters = configParams
        FairBid.configureForAppId(configParams.appKey)
            .enableLogs()
            .disableAutoRequesting()
            .start(activity)
        bannerInterceptorFlow.initBannerListener()
        interstitialInterceptorFlow.initInterstitialListener()
        rewardedInterceptorFlow.initRewardedListener()
    }

    private fun ImpressionData?.asAd(demandAd: DemandAd, placementId: String): Ad {
        return Ad(
            demandId = demandId,
            demandAd = demandAd,
            price = this?.netPayout ?: 0.0,
            sourceAd = placementId, // Cause FairBid is a Singleton
            monetizationNetwork = this?.networkInstanceId,
            dsp = this?.demandSource,
            auctionRound = Ad.AuctionRound.Mediation,
            currencyCode = this?.currency,
        )
    }

    override fun interstitial(activity: Activity?, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
        return AuctionRequest {
            val placementId = adParams.getString(PlacementKey) ?: fairBidParameters?.interstitialPlacementIds?.first()
            if (placementId.isNullOrBlank()) {
                return@AuctionRequest Result.failure(DemandError.NoPlacement(demandId))
            }
            interstitialPlacementsDemandAd[placementId] = demandAd
            Interstitial.request(placementId)
            val loadingResult = interstitialInterceptorFlow.first {
                (it as? InterstitialInterceptor.Loaded)?.placementId == placementId ||
                        (it as? InterstitialInterceptor.LoadFailed)?.placementId == placementId
            }
            return@AuctionRequest when (loadingResult) {
                is InterstitialInterceptor.Loaded -> {
                    val impressionData = Interstitial.getImpressionData(placementId)
                    Result.success(
                        AuctionResult(
                            ad = impressionData.asAd(demandAd, placementId),
                            adProvider = object : AdProvider {
                                override fun canShow(): Boolean = Interstitial.isAvailable(placementId)
                                override fun destroy() {}
                                override fun showAd(activity: Activity?, adParams: Bundle) {
                                    val options = ShowOptions().apply {
                                        customParameters = adParams.keySet().mapNotNull { key ->
                                            try {
                                                key to adParams.getString(key)
                                            } catch (e: Exception) {
                                                null
                                            }
                                        }.toMap()
                                    }
                                    Interstitial.show(placementId, options, activity)
                                }
                            }
                        )
                    )
                }
                is InterstitialInterceptor.LoadFailed -> {
                    Result.failure(DemandError.NoFill(demandId))
                }
                else -> error("Unexpected state: $loadingResult")
            }
        }
    }

    override fun rewarded(activity: Activity?, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
        return AuctionRequest {
            val placementId = adParams.getString(PlacementKey) ?: fairBidParameters?.rewardedPlacementIds?.first()
            if (placementId.isNullOrBlank()) {
                return@AuctionRequest Result.failure(DemandError.NoPlacement(demandId))
            }
            placements.addIfAbsent(placementId)
            rewardedPlacementsDemandAd[placementId] = demandAd
            Rewarded.request(placementId)
            val loadingResult = rewardedInterceptorFlow.first {
                (it as? RewardedInterceptor.Loaded)?.placementId == placementId ||
                        (it as? RewardedInterceptor.LoadFailed)?.placementId == placementId
            }
            return@AuctionRequest when (loadingResult) {
                is RewardedInterceptor.Loaded -> {
                    val impressionData = Rewarded.getImpressionData(placementId)
                    Result.success(
                        AuctionResult(
                            ad = impressionData.asAd(demandAd, placementId),
                            adProvider = object : AdProvider {
                                override fun canShow(): Boolean = Interstitial.isAvailable(placementId)
                                override fun destroy() {}
                                override fun showAd(activity: Activity?, adParams: Bundle) {
                                    val options = ShowOptions().apply {
                                        customParameters = adParams.keySet().mapNotNull { key ->
                                            try {
                                                key to adParams.getString(key)
                                            } catch (e: Exception) {
                                                null
                                            }
                                        }.toMap()
                                    }
                                    Interstitial.show(placementId, options, activity)
                                }
                            }
                        )
                    )
                }
                is RewardedInterceptor.LoadFailed -> {
                    Result.failure(DemandError.NoFill(demandId))
                }
                else -> error("Unexpected state: $loadingResult")
            }
        }
    }

    override fun banner(context: Context, demandAd: DemandAd, adParams: Bundle, adContainer: ViewGroup?): AuctionRequest {
        return AuctionRequest {
            val placementId = adParams.getString(PlacementKey) ?: fairBidParameters?.bannerPlacementIds?.first()
            if (placementId.isNullOrBlank()) {
                return@AuctionRequest Result.failure(DemandError.NoPlacement(demandId))
            }
            bannerPlacementsRevenue.remove(placementId)
            Banner.show(placementId, BannerOptions().placeInContainer(adContainer), context as Activity)
            val loadingResult = bannerInterceptorFlow.first {
                (it as? BannerInterceptor.Loaded)?.placementId == placementId ||
                        (it as? BannerInterceptor.Error)?.placementId == placementId
            }
            return@AuctionRequest when (loadingResult) {
                is BannerInterceptor.Error -> {
                    Result.failure(loadingResult.cause)
                }
                is BannerInterceptor.Loaded -> {
                    Result.success(
                        AuctionResult(
                            ad = null.asAd(demandAd, placementId),
                            adProvider = object : AdProvider, AdViewProvider {
                                override fun getAdView(): View = requireNotNull(adContainer)
                                override fun canShow(): Boolean = true
                                override fun showAd(activity: Activity?, adParams: Bundle) {}

                                override fun destroy() {
                                    Banner.destroy(placementId)
                                }
                            }
                        )
                    )
                }
                is BannerInterceptor.Clicked,
                is BannerInterceptor.RequestStarted,
                is BannerInterceptor.Shown -> error("Unexpected state: $loadingResult")
            }
        }
    }

    private fun proceedInterstitialCallbacks(interceptor: InterstitialInterceptor) {
        when (interceptor) {
            is InterstitialInterceptor.Clicked -> {
                val (listener, ad) = getCoreListener(interceptor.placementId)
                listener.onAdClicked(ad)
            }
            is InterstitialInterceptor.Hidden -> {
                val (listener, ad) = getCoreListener(interceptor.placementId)
                listener.onAdHidden(ad)
            }
            is InterstitialInterceptor.ShowFailed -> {
                val (listener, ad) = getCoreListener(interceptor.placementId)
                listener.onAdDisplayFailed(DemandError.Unspecified(ad.demandId))
            }
            is InterstitialInterceptor.Shown -> {
                val (listener, ad) = getCoreListener(interceptor.placementId)
                listener.onAdDisplayed(ad)
            }
            is InterstitialInterceptor.LoadFailed,
            is InterstitialInterceptor.Loaded -> {
                // do nothing. Use only in [fun interstitial()]
            }
            is InterstitialInterceptor.RequestStarted -> {
                // do nothing again
            }
        }
    }

    private fun proceedRewardedCallbacks(interceptor: RewardedInterceptor) {
        when (interceptor) {
            is RewardedInterceptor.Clicked -> {
                val (listener, ad) = getCoreListener(interceptor.placementId)
                listener.onAdClicked(ad)
            }
            is RewardedInterceptor.Hidden -> {
                val (listener, ad) = getCoreListener(interceptor.placementId)
                listener.onAdHidden(ad)
            }
            is RewardedInterceptor.ShowFailed -> {
                val (listener, ad) = getCoreListener(interceptor.placementId)
                listener.onAdDisplayFailed(DemandError.Unspecified(ad.demandId))
            }
            is RewardedInterceptor.Shown -> {
                val (listener, ad) = getCoreListener(interceptor.placementId)
                listener.onAdDisplayed(ad)
            }
            is RewardedInterceptor.LoadFailed,
            is RewardedInterceptor.Loaded -> {
                // do nothing. Use only in [fun rewarded()]
            }
            is RewardedInterceptor.RequestStarted -> {
                // do nothing again
            }
            is RewardedInterceptor.Completion -> {
                val (listener, ad) = getCoreListener(interceptor.placementId)
                listener.onUserRewarded(ad, if (interceptor.userRewarded) RewardedAdListener.Reward("", 0) else null)
            }
        }
    }

    private fun proceedBannerCallbacks(interceptor: BannerInterceptor) {
        when (interceptor) {
            is BannerInterceptor.Clicked -> {
                val (listener, ad) = getCoreListener(interceptor.placementId)
                listener.onAdClicked(ad)
            }
            is BannerInterceptor.Error,
            is BannerInterceptor.Loaded -> {
                // do nothing. Use only in [fun banner()]
            }
            is BannerInterceptor.RequestStarted -> {
            }
            is BannerInterceptor.Shown -> {
                bannerPlacementsRevenue[interceptor.placementId] = interceptor.impressionData
                val (listener, ad) = getCoreListener(interceptor.placementId)
                listener.onAdDisplayed(ad)
            }
        }
    }

    private fun getCoreListener(placementId: String): Pair<AdListener, Ad> {
        interstitialPlacementsDemandAd[placementId]?.let { demandAd ->
            val impressionData = Interstitial.getImpressionData(placementId)
            return SdkCore.getListenerForDemand(demandAd) to impressionData.asAd(demandAd, placementId)
        }
        rewardedPlacementsDemandAd[placementId]?.let { demandAd ->
            val impressionData = Rewarded.getImpressionData(placementId)
            return SdkCore.getListenerForDemand(demandAd) to impressionData.asAd(demandAd, placementId)
        }
        bannerPlacementsDemandAd[placementId]?.let { demandAd ->
            val impressionData = bannerPlacementsRevenue[placementId]
            return SdkCore.getListenerForDemand(demandAd) to impressionData.asAd(demandAd, placementId)
        }
        error("Unknown DemandAd for placementId=$placementId")
    }

    private fun MutableList<String>.addIfAbsent(placementId: String) {
        if (this.indexOf(placementId) == -1) {
            this.add(placementId)
        }
    }
}

const val PlacementKey = "placement"