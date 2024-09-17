package org.bidon.fyber

import android.app.Activity
import android.content.Context
import com.fyber.FairBid
import com.fyber.fairbid.ads.ImpressionData
import com.fyber.fairbid.ads.Interstitial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import org.bidon.fyber.banner.BannerInterceptor
import org.bidon.fyber.banner.initBannerListener
import org.bidon.fyber.ext.adapterVersion
import org.bidon.fyber.ext.sdkVersion
import org.bidon.fyber.interstitial.InterstitialInterceptor
import org.bidon.fyber.interstitial.initInterstitialListener
import org.bidon.fyber.rewarded.RewardedInterceptor
import org.bidon.fyber.rewarded.initRewardedListener
import org.bidon.sdk.adapter.*
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.utils.SdkDispatchers
import org.json.JSONObject

internal val FairBidDemandId = DemandId("fair_bid")

@Suppress("unused")
internal class FairBidAdapter :
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
            demandAd = demandAd,
            sourceAd = placementId, // Cause FairBid is a Singleton
            networkName = this?.networkInstanceId,
            dsp = this?.demandSource,
            roundId = "Ad.AuctionRound.Mediation",
            currencyCode = this?.currency,
            auctionId = "",
            adUnitId = null,
            ecpm = this?.netPayout ?: 0.0,
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
//    override fun interstitialParams(pricefloor: Double, timeout: Long, lineItems: List<LineItem>): AdSource.AdParams {
//        error("No additional params for FairBid interstitial")
//    }
//
//    override fun rewardedParams(pricefloor: Double, timeout: Long, lineItems: List<LineItem>): AdSource.AdParams {
//        error("No additional params for FairBid rewarded")
//    }
//
//    override fun bannerParams(
//        pricefloor: Double,
//        lineItems: List<LineItem>,
//        bannerSize: BannerSize,
//        adContainer: ViewGroup?
//    ): AdSource.AdParams = FairBidBannerParams(requireNotNull(adContainer))

    private fun MutableList<String>.addIfAbsent(placementId: String) {
        if (this.indexOf(placementId) == -1) {
            this.add(placementId)
        }
    }

    override fun parseConfigParam(json: String): FairBidParameters {
        val jsonObject = JSONObject(json)
        return FairBidParameters(
            appKey = jsonObject.getString("app_key"),
        )
    }
}

const val PlacementKey = "placement"
