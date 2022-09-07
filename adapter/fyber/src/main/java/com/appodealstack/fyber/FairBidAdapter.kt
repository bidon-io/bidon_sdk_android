package com.appodealstack.fyber

import android.app.Activity
import android.content.Context
import com.appodealstack.bidon.data.json.parse
import com.appodealstack.bidon.data.models.config.AdapterInfo
import com.appodealstack.bidon.domain.adapter.Adapter
import com.appodealstack.bidon.domain.adapter.Initializable
import com.appodealstack.bidon.domain.common.Ad
import com.appodealstack.bidon.domain.common.DemandAd
import com.appodealstack.bidon.domain.common.DemandId
import com.appodealstack.bidon.view.helper.SdkDispatchers
import com.appodealstack.fyber.banner.BannerInterceptor
import com.appodealstack.fyber.banner.initBannerListener
import com.appodealstack.fyber.ext.adapterVersion
import com.appodealstack.fyber.ext.sdkVersion
import com.appodealstack.fyber.interstitial.InterstitialInterceptor
import com.appodealstack.fyber.interstitial.initInterstitialListener
import com.appodealstack.fyber.rewarded.RewardedInterceptor
import com.appodealstack.fyber.rewarded.initRewardedListener
import com.fyber.FairBid
import com.fyber.fairbid.ads.ImpressionData
import com.fyber.fairbid.ads.Interstitial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.json.JsonObject

val FairBidDemandId = DemandId("fair_bid")

class FairBidAdapter :
    Adapter,
    Initializable<FairBidParameters> {
    override val demandId: DemandId = FairBidDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    private lateinit var context: Context
    private val scope: CoroutineScope
        get() = CoroutineScope(SdkDispatchers.Default)

    private val interstitialInterceptorFlow = MutableSharedFlow<InterstitialInterceptor>(extraBufferCapacity = Int.MAX_VALUE)
    private val interstitialPlacementsDemandAd = mutableMapOf<String, DemandAd>()

    private val rewardedInterceptorFlow = MutableSharedFlow<RewardedInterceptor>(extraBufferCapacity = Int.MAX_VALUE)
    private val rewardedPlacementsDemandAd = mutableMapOf<String, DemandAd>()

    private val bannerInterceptorFlow = MutableSharedFlow<BannerInterceptor>(extraBufferCapacity = Int.MAX_VALUE)
    private val bannerPlacementsDemandAd = mutableMapOf<String, DemandAd>()
    private val bannerPlacementsRevenue = mutableMapOf<String, ImpressionData>()
    private val placements = mutableListOf<String>()

    override suspend fun init(activity: Activity, configParams: FairBidParameters) {
        this.context = activity.applicationContext
        Interstitial()
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
            roundId = "Ad.AuctionRound.Mediation",
            currencyCode = this?.currency,
            auctionId = ""
        )
    }

//    override fun interstitial(activity: Activity?, demandAd: DemandAd, adParams: AdSource.AdParams): OldAuctionRequest {
//        return OldAuctionRequest {
//            val placementId = demandAd.placement
//            if (placementId.isNullOrBlank()) {
//                return@OldAuctionRequest Result.failure(DemandError.NoPlacement(demandId))
//            }
//            interstitialPlacementsDemandAd[placementId] = demandAd
//            Interstitial.request(placementId)
//            val loadingResult = interstitialInterceptorFlow.first {
//                (it as? InterstitialInterceptor.Loaded)?.placementId == placementId ||
//                        (it as? InterstitialInterceptor.LoadFailed)?.placementId == placementId
//            }
//            return@OldAuctionRequest when (loadingResult) {
//                is InterstitialInterceptor.Loaded -> {
//                    val impressionData = Interstitial.getImpressionData(placementId)
//                    Result.success(
//                        OldAuctionResult(
//                            ad = impressionData.asAd(demandAd, placementId),
//                            adProvider = object : OldAdProvider {
//                                override fun canShow(): Boolean = Interstitial.isAvailable(placementId)
//                                override fun destroy() {}
//                                override fun showAd(activity: Activity?, adParams: Bundle) {
//                                    val options = ShowOptions().apply {
//                                        customParameters = adParams.keySet().mapNotNull { key ->
//                                            try {
//                                                key to adParams.getString(key)
//                                            } catch (e: Exception) {
//                                                null
//                                            }
//                                        }.toMap()
//                                    }
//                                    Interstitial.show(placementId, options, activity)
//                                }
//                            }
//                        )
//                    )
//                }
//                is InterstitialInterceptor.LoadFailed -> {
//                    Result.failure(DemandError.NoFill(demandId))
//                }
//                else -> error("Unexpected state: $loadingResult")
//            }
//        }
//    }
//
//    override fun rewarded(activity: Activity?, demandAd: DemandAd, adParams: AdSource.AdParams): OldAuctionRequest {
//        return OldAuctionRequest {
//            val placementId = demandAd.placement
//            if (placementId.isNullOrBlank()) {
//                return@OldAuctionRequest Result.failure(DemandError.NoPlacement(demandId))
//            }
//            placements.addIfAbsent(placementId)
//            rewardedPlacementsDemandAd[placementId] = demandAd
//            Rewarded.request(placementId)
//            val loadingResult = rewardedInterceptorFlow.first {
//                (it as? RewardedInterceptor.Loaded)?.placementId == placementId ||
//                        (it as? RewardedInterceptor.LoadFailed)?.placementId == placementId
//            }
//            return@OldAuctionRequest when (loadingResult) {
//                is RewardedInterceptor.Loaded -> {
//                    val impressionData = Rewarded.getImpressionData(placementId)
//                    Result.success(
//                        OldAuctionResult(
//                            ad = impressionData.asAd(demandAd, placementId),
//                            adProvider = object : OldAdProvider {
//                                override fun canShow(): Boolean = Interstitial.isAvailable(placementId)
//                                override fun destroy() {}
//                                override fun showAd(activity: Activity?, adParams: Bundle) {
//                                    val options = ShowOptions().apply {
//                                        customParameters = adParams.keySet().mapNotNull { key ->
//                                            try {
//                                                key to adParams.getString(key)
//                                            } catch (e: Exception) {
//                                                null
//                                            }
//                                        }.toMap()
//                                    }
//                                    Interstitial.show(placementId, options, activity)
//                                }
//                            }
//                        )
//                    )
//                }
//                is RewardedInterceptor.LoadFailed -> {
//                    Result.failure(DemandError.NoFill(demandId))
//                }
//                else -> error("Unexpected state: $loadingResult")
//            }
//        }
//    }
//
//    override fun banner(context: Context, demandAd: DemandAd, adParams: FairBidBannerParams): OldAuctionRequest {
//        return OldAuctionRequest {
//            val placementId = demandAd.placement
//            if (placementId.isNullOrBlank()) {
//                return@OldAuctionRequest Result.failure(DemandError.NoPlacement(demandId))
//            }
//            bannerPlacementsRevenue.remove(placementId)
//            Banner.show(placementId, BannerOptions().placeInContainer(adParams.adContainer), context as Activity)
//            val loadingResult = bannerInterceptorFlow.first {
//                (it as? BannerInterceptor.Loaded)?.placementId == placementId ||
//                        (it as? BannerInterceptor.Error)?.placementId == placementId
//            }
//            return@OldAuctionRequest when (loadingResult) {
//                is BannerInterceptor.Error -> {
//                    Result.failure(loadingResult.cause)
//                }
//                is BannerInterceptor.Loaded -> {
//                    Result.success(
//                        OldAuctionResult(
//                            ad = null.asAd(demandAd, placementId),
//                            adProvider = object : OldAdProvider, AdViewProvider {
//                                override fun getAdView(): View = adParams.adContainer
//                                override fun canShow(): Boolean = true
//                                override fun showAd(activity: Activity?, adParams: Bundle) {}
//
//                                override fun destroy() {
//                                    Banner.destroy(placementId)
//                                }
//                            }
//                        )
//                    )
//                }
//                is BannerInterceptor.Clicked,
//                is BannerInterceptor.RequestStarted,
//                is BannerInterceptor.Shown -> error("Unexpected state: $loadingResult")
//            }
//        }
//    }
//
//    override fun interstitialParams(priceFloor: Double, timeout: Long, lineItems: List<LineItem>): AdSource.AdParams {
//        error("No additional params for FairBid interstitial")
//    }
//
//    override fun rewardedParams(priceFloor: Double, timeout: Long, lineItems: List<LineItem>): AdSource.AdParams {
//        error("No additional params for FairBid rewarded")
//    }
//
//    override fun bannerParams(
//        priceFloor: Double,
//        lineItems: List<LineItem>,
//        bannerSize: BannerSize,
//        adContainer: ViewGroup?
//    ): AdSource.AdParams = FairBidBannerParams(requireNotNull(adContainer))

    private fun MutableList<String>.addIfAbsent(placementId: String) {
        if (this.indexOf(placementId) == -1) {
            this.add(placementId)
        }
    }

    override fun parseConfigParam(json: JsonObject): FairBidParameters =
        requireNotNull(json[demandId.demandId]).parse(FairBidParameters.serializer())
}

const val PlacementKey = "placement"
