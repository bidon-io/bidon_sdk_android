package com.appodealstack.fyber

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.auctions.AuctionRequest
import com.appodealstack.mads.auctions.AuctionResult
import com.appodealstack.mads.demands.*
import com.fyber.fairbid.ads.ImpressionData
import com.fyber.fairbid.ads.Interstitial
import com.fyber.fairbid.ads.Rewarded
import com.fyber.fairbid.ads.ShowOptions
import com.fyber.fairbid.ads.interstitial.InterstitialListener
import com.fyber.fairbid.ads.rewarded.RewardedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val FairBidDemandId = DemandId("fair_bid")

class FairBidAdapter : Adapter.Mediation<FairBidParameters>,
    AdSource.Interstitial, AdSource.Rewarded {
    override val demandId: DemandId = FairBidDemandId
    private lateinit var context: Context
    private val interstitialInterceptorFlow = MutableSharedFlow<InterstitialInterceptor>(extraBufferCapacity = Int.MAX_VALUE)
    private val interstitialPlacementsDemandAd = mutableMapOf<String, DemandAd>()

    private val rewardedInterceptorFlow = MutableSharedFlow<RewardedInterceptor>(extraBufferCapacity = Int.MAX_VALUE)
    private val rewardedPlacementsDemandAd = mutableMapOf<String, DemandAd>()

    init {
        CoroutineScope(Dispatchers.Default).launch {
            interstitialInterceptorFlow.collect { interceptor ->
                proceedInterstitialCallbacks(interceptor)
            }
        }
        CoroutineScope(Dispatchers.Default).launch {
            rewardedInterceptorFlow.collect { interceptor ->
                proceedRewardedCallbacks(interceptor)
            }
        }
    }

    override suspend fun init(context: Context, configParams: FairBidParameters) {
        this.context = context
        initInterstitialListener()
        initRewardedListener()
    }

    override fun interstitial(activity: Activity?, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
        return AuctionRequest {
            val placementId = requireNotNull(adParams.getString(PlacementKey)) {
                "PlacementId should be provided"
            }
            interstitialPlacementsDemandAd[placementId] = demandAd
            Interstitial.request(placementId)
            val loadingResult = interstitialInterceptorFlow.first {
                (it as? InterstitialInterceptor.Loaded)?.placementId == placementId ||
                        (it as? InterstitialInterceptor.LoadFailed)?.placementId == placementId
            }
            return@AuctionRequest when (loadingResult) {
                is InterstitialInterceptor.Loaded -> {
                    val ad = Ad(
                        demandId = demandId,
                        demandAd = demandAd,
                        price = Interstitial.getImpressionData(placementId)?.netPayout ?: 0.0,
                        sourceAd = placementId // Cause FairBid is a Singleton
                    )
                    Result.success(
                        AuctionResult(
                            ad = ad,
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
                    Result.failure(DemandError.NoFill)
                }
                else -> error("Unexpected state: $loadingResult")
            }
        }
    }

    override fun rewarded(activity: Activity?, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
        return AuctionRequest {
            val placementId = requireNotNull(adParams.getString(PlacementKey)) {
                "PlacementId should be provided"
            }
            rewardedPlacementsDemandAd[placementId] = demandAd
            Rewarded.request(placementId)
            val loadingResult = rewardedInterceptorFlow.first {
                (it as? RewardedInterceptor.Loaded)?.placementId == placementId ||
                        (it as? RewardedInterceptor.LoadFailed)?.placementId == placementId
            }
            return@AuctionRequest when (loadingResult) {
                is RewardedInterceptor.Loaded -> {
                    val ad = Ad(
                        demandId = demandId,
                        demandAd = demandAd,
                        price = Interstitial.getImpressionData(placementId)?.netPayout ?: 0.0,
                        sourceAd = placementId // Cause FairBid is a Singleton
                    )
                    Result.success(
                        AuctionResult(
                            ad = ad,
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
                    Result.failure(DemandError.NoFill)
                }
                else -> error("Unexpected state: $loadingResult")
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
                listener.onAdDisplayFailed(DemandError.Unspecified)
            }
            is InterstitialInterceptor.Shown -> {
                val (listener, ad) = getCoreListener(interceptor.placementId)
                listener.onAdDisplayed(ad)
            }
            is InterstitialInterceptor.LoadFailed,
            is InterstitialInterceptor.Loaded -> {
                // do nothing. Use only for [fun interstitial()]
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
                listener.onAdDisplayFailed(DemandError.Unspecified)
            }
            is RewardedInterceptor.Shown -> {
                val (listener, ad) = getCoreListener(interceptor.placementId)
                listener.onAdDisplayed(ad)
            }
            is RewardedInterceptor.LoadFailed,
            is RewardedInterceptor.Loaded -> {
                // do nothing. Use only for [fun rewarded()]
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

    private fun getCoreListener(placementId: String): Pair<AdListener, Ad> {
        interstitialPlacementsDemandAd[placementId]?.let { demandAd ->
            return SdkCore.getListenerForDemand(demandAd) to Ad(
                demandId = demandId,
                demandAd = demandAd,
                price = Interstitial.getImpressionData(placementId)?.netPayout ?: 0.0,
                sourceAd = placementId // Cause FairBid is a Singleton
            )
        }
        rewardedPlacementsDemandAd[placementId]?.let { demandAd ->
            return SdkCore.getListenerForDemand(demandAd) to Ad(
                demandId = demandId,
                demandAd = demandAd,
                price = Rewarded.getImpressionData(placementId)?.netPayout ?: 0.0,
                sourceAd = placementId // Cause FairBid is a Singleton
            )
        }
        error("Unknown DemandAd for placementId=$placementId")
    }

    private fun initInterstitialListener() {
        Interstitial.setInterstitialListener(object : InterstitialListener {
            override fun onShow(placementId: String, impressionData: ImpressionData) {
                interstitialInterceptorFlow.tryEmit(
                    InterstitialInterceptor.Shown(placementId, impressionData)
                )
            }

            override fun onClick(placementId: String) {
                interstitialInterceptorFlow.tryEmit(
                    InterstitialInterceptor.Clicked(placementId)
                )
            }

            override fun onHide(placementId: String) {
                interstitialInterceptorFlow.tryEmit(
                    InterstitialInterceptor.Hidden(placementId)
                )
            }

            override fun onShowFailure(placementId: String, impressionData: ImpressionData) {
                interstitialInterceptorFlow.tryEmit(
                    InterstitialInterceptor.ShowFailed(placementId)
                )

            }

            override fun onAvailable(placementId: String) {
                interstitialInterceptorFlow.tryEmit(
                    InterstitialInterceptor.Loaded(placementId)
                )

            }

            override fun onUnavailable(placementId: String) {
                interstitialInterceptorFlow.tryEmit(
                    InterstitialInterceptor.LoadFailed(placementId)
                )
            }

            override fun onRequestStart(placementId: String) {}
        })
    }

    private fun initRewardedListener() {
        Rewarded.setRewardedListener(object : RewardedListener {
            override fun onShow(placementId: String, impressionData: ImpressionData) {
                rewardedInterceptorFlow.tryEmit(
                    RewardedInterceptor.Shown(placementId, impressionData)
                )
            }

            override fun onClick(placementId: String) {
                rewardedInterceptorFlow.tryEmit(
                    RewardedInterceptor.Clicked(placementId)
                )
            }

            override fun onHide(placementId: String) {
                rewardedInterceptorFlow.tryEmit(
                    RewardedInterceptor.Hidden(placementId)
                )
            }

            override fun onShowFailure(placementId: String, impressionData: ImpressionData) {
                rewardedInterceptorFlow.tryEmit(
                    RewardedInterceptor.ShowFailed(placementId)
                )

            }

            override fun onAvailable(placementId: String) {
                rewardedInterceptorFlow.tryEmit(
                    RewardedInterceptor.Loaded(placementId)
                )

            }

            override fun onUnavailable(placementId: String) {
                rewardedInterceptorFlow.tryEmit(
                    RewardedInterceptor.LoadFailed(placementId)
                )
            }

            override fun onCompletion(placementId: String, userRewarded: Boolean) {
                rewardedInterceptorFlow.tryEmit(
                    RewardedInterceptor.Completion(placementId, userRewarded)
                )
            }

            override fun onRequestStart(placementId: String) {}
        })
    }
}

sealed interface InterstitialInterceptor {
    class Shown(val placementId: String, val impressionData: ImpressionData) : InterstitialInterceptor
    data class Clicked(val placementId: String) : InterstitialInterceptor
    data class Hidden(val placementId: String) : InterstitialInterceptor
    data class ShowFailed(val placementId: String) : InterstitialInterceptor
    data class Loaded(val placementId: String) : InterstitialInterceptor
    data class LoadFailed(val placementId: String) : InterstitialInterceptor
    data class RequestStarted(val placementId: String) : InterstitialInterceptor
}

sealed interface RewardedInterceptor {
    class Shown(val placementId: String, val impressionData: ImpressionData) : RewardedInterceptor
    data class Clicked(val placementId: String) : RewardedInterceptor
    data class Hidden(val placementId: String) : RewardedInterceptor
    data class ShowFailed(val placementId: String) : RewardedInterceptor
    data class Loaded(val placementId: String) : RewardedInterceptor
    data class LoadFailed(val placementId: String) : RewardedInterceptor
    data class RequestStarted(val placementId: String) : RewardedInterceptor
    data class Completion(val placementId: String, val userRewarded: Boolean) : RewardedInterceptor
}

const val PlacementKey = "placement"