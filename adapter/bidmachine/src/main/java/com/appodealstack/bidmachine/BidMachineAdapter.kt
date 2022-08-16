package com.appodealstack.bidmachine

import android.app.Activity
import android.content.Context
import com.appodealstack.bidmachine.ext.adapterVersion
import com.appodealstack.bidmachine.ext.sdkVersion
import com.appodealstack.bidmachine.impl.BMInterstitialAdImpl
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.config.data.models.AdapterInfo
import com.appodealstack.bidon.core.parse
import io.bidmachine.BidMachine
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.resume

val BidMachineDemandId = DemandId("bidmachine")

internal typealias BidMachineBannerSize = io.bidmachine.banner.BannerSize
internal typealias BMAuctionResult = io.bidmachine.models.AuctionResult

class BidMachineAdapter : Adapter, Initializable<BidMachineParameters>,
    AdProvider.Interstitial<BMFullscreenParams> {
    private lateinit var context: Context

    override val demandId = BidMachineDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(activity: Activity, configParams: BidMachineParameters): Unit =
        suspendCancellableCoroutine { continuation ->
            this.context = activity.applicationContext
            val sourceId = configParams.sellerId
            BidMachine.initialize(context, sourceId) {
                continuation.resume(Unit)
            }
        }


    override fun parseConfigParam(json: JsonObject): BidMachineParameters = json.parse(BidMachineParameters.serializer())

    override fun interstitial(demandAd: DemandAd, roundId: String): AdSource.Interstitial<BMFullscreenParams> {
        return BMInterstitialAdImpl(demandId, demandAd, roundId)
    }

//    override fun rewarded(activity: Activity?, demandAd: DemandAd, adParams: BMFullscreenParams): OldAuctionRequest {
//        return OldAuctionRequest {
//            suspendCancellableCoroutine { continuation ->
//                val isFinished = AtomicBoolean(false)
//                val request = RewardedRequest.Builder().apply {
//                    setPriceFloorParams(PriceFloorParams().addPriceFloor(adParams.priceFloor))
//                    demandAd.placement?.let {
//                        setPlacementId(it)
//                    }
//                }.build()
//                RewardedAd(context)
//                    .setListener(object : RewardedListener {
//                        override fun onAdLoaded(rewardedAd: RewardedAd) {
//                            if (!isFinished.getAndSet(true)) {
//                                val auctionResult = OldAuctionResult(
//                                    ad = rewardedAd.auctionResult.asAd(demandAd, rewardedAd),
//                                    adProvider = object : OldAdProvider {
//                                        override fun canShow(): Boolean = rewardedAd.canShow()
//                                        override fun showAd(activity: Activity?, adParams: Bundle) = rewardedAd.show()
//                                        override fun destroy() = rewardedAd.destroy()
//                                    }
//                                )
//                                rewardedAd.setCoreListener(auctionResult)
//                                continuation.resume(Result.success(auctionResult))
//                            }
//                        }
//
//                        override fun onAdLoadFailed(rewardedAd: RewardedAd, bmError: BMError) {
//                            if (!isFinished.getAndSet(true)) {
//                                // remove listener
//                                rewardedAd.setListener(null)
//                                continuation.resume(Result.failure(bmError.asBidonError(demandId)))
//                            }
//                        }
//
//                        override fun onAdExpired(rewardedAd: RewardedAd) {
//                            if (!isFinished.getAndSet(true)) {
//                                // remove listener
//                                rewardedAd.setListener(null)
//                                continuation.resume(Result.failure(DemandError.Expired(demandId)))
//                            }
//                        }
//
//                        @Deprecated("Deprecated in Java")
//                        override fun onAdShown(p0: RewardedAd) {
//                        }
//
//                        override fun onAdImpression(p0: RewardedAd) {}
//                        override fun onAdClicked(p0: RewardedAd) {}
//                        override fun onAdShowFailed(p0: RewardedAd, p1: BMError) {}
//                        override fun onAdClosed(p0: RewardedAd, p1: Boolean) {}
//                        override fun onAdRewarded(p0: RewardedAd) {}
//
//                    })
//                    .load(request)
//            }
//        }
//    }
//
//    override fun banner(context: Context, demandAd: DemandAd, adParams: BMBannerParams): OldAuctionRequest {
//        return OldAuctionRequest {
//            suspendCancellableCoroutine { continuation ->
//                val isFinished = AtomicBoolean(false)
//                val request = BannerRequest.Builder()
//                    .setSize(adParams.bannerSize.asBidMachineBannerSize())
//                    .apply {
//                        setPriceFloorParams(PriceFloorParams().addPriceFloor(adParams.priceFloor))
//                        demandAd.placement?.let {
//                            setPlacementId(it)
//                        }
//                    }.build()
//                val bannerView = BannerView(context)
//                bannerView.setListener(object : BannerListener {
//                    override fun onAdLoaded(view: BannerView) {
//                        if (!isFinished.getAndSet(true)) {
//                            val auctionResult = OldAuctionResult(
//                                ad = view.auctionResult.asAd(demandAd, bannerView),
//                                adProvider = object : OldAdProvider, AdViewProvider {
//                                    override fun canShow(): Boolean = view.canShow()
//                                    override fun showAd(activity: Activity?, adParams: Bundle) {}
//                                    override fun destroy() = view.destroy()
//                                    override fun getAdView(): View = view
//                                }
//                            )
//                            view.setCoreListener(auctionResult)
//                            continuation.resume(Result.success(auctionResult))
//                        }
//                    }
//
//                    override fun onAdLoadFailed(view: BannerView, bmError: BMError) {
//                        if (!isFinished.getAndSet(true)) {
//                            // remove listener
//                            view.setListener(null)
//                            continuation.resume(Result.failure(bmError.asBidonError(demandId)))
//                        }
//                    }
//
//                    override fun onAdExpired(view: BannerView) {
//                        if (!isFinished.getAndSet(true)) {
//                            // remove listener
//                            view.setListener(null)
//                            continuation.resume(Result.failure(DemandError.Expired(demandId)))
//                        }
//                    }
//
//                    @Deprecated("Deprecated in Java")
//                    override fun onAdShown(p0: BannerView) {
//                    }
//
//                    override fun onAdImpression(view: BannerView) {}
//                    override fun onAdClicked(p0: BannerView) {}
//                })
//                bannerView.load(request)
//            }
//        }
//    }
//
//    override fun rewardedParams(priceFloor: Double, timeout: Long, lineItems: List<LineItem>): AdSource.AdParams {
//        return BMFullscreenParams(priceFloor = priceFloor, timeout = timeout)
//    }

//    override fun bannerParams(
//        priceFloor: Double,
//        lineItems: List<LineItem>,
//        bannerSize: BannerSize,
//        adContainer: ViewGroup?
//    ): AdSource.AdParams {
//        return BMBannerParams(priceFloor = priceFloor, bannerSize = bannerSize)
//    }
//
//    private fun InterstitialAd.setCoreListener(auctionResult: OldAuctionResult) {
//        val coreListener = SdkCore.getListenerForDemand(auctionResult.ad.demandAd)
//        val demandAd = auctionResult.ad.demandAd
//        this.setListener(
//            object : InterstitialListener {
//                override fun onAdLoaded(interstitialAd: InterstitialAd) {
//                    coreListener.onAdLoaded(
//                        interstitialAd.auctionResult.asAd(demandAd, interstitialAd)
//                    )
//                }
//
//                override fun onAdLoadFailed(ad: InterstitialAd, bmError: BMError) {
//                    coreListener.onAdShowFailed(bmError.asBidonError(demandId))
//                }
//
//                @Deprecated("Deprecated in Java")
//                override fun onAdShown(interstitialAd: InterstitialAd) {
//                    coreListener.onAdShown(
//                        interstitialAd.auctionResult.asAd(demandAd, interstitialAd)
//                    )
//                }
//
//                override fun onAdImpression(interstitialAd: InterstitialAd) {
//                    val ad = interstitialAd.auctionResult.asAd(demandAd, interstitialAd)
//                    SdkCore.getAdRevenueInterceptor()?.onAdRevenueReceived(ad)
//                    coreListener.onAdImpression(ad)
//                }
//
//                override fun onAdClicked(interstitialAd: InterstitialAd) {
//                    coreListener.onAdClicked(
//                        interstitialAd.auctionResult.asAd(demandAd, interstitialAd)
//                    )
//                }
//
//                override fun onAdExpired(ad: InterstitialAd) {
//                }
//
//                override fun onAdShowFailed(ad: InterstitialAd, bmError: BMError) {
//                    coreListener.onAdShowFailed(bmError.asBidonError(demandId))
//                }
//
//                override fun onAdClosed(interstitialAd: InterstitialAd, bmError: Boolean) {
//                    coreListener.onAdClosed(
//                        interstitialAd.auctionResult.asAd(demandAd, interstitialAd)
//                    )
//                }
//            }
//        )
//    }
//
//    private fun BannerView.setCoreListener(auctionResult: OldAuctionResult) {
//        val coreListener = SdkCore.getListenerForDemand(auctionResult.ad.demandAd)
//        val demandAd = auctionResult.ad.demandAd
//        this.setListener(object : BannerListener {
//            override fun onAdLoaded(bannerView: BannerView) {
//                coreListener.onAdLoaded(
//                    bannerView.auctionResult.asAd(demandAd, bannerView)
//                )
//            }
//
//            override fun onAdLoadFailed(bannerView: BannerView, bmError: BMError) {
//                coreListener.onAdLoadFailed(bmError.asBidonError(demandId))
//            }
//
//            @Deprecated("Deprecated in Java")
//            override fun onAdShown(bannerView: BannerView) {
//                coreListener.onAdShown(
//                    bannerView.auctionResult.asAd(demandAd, bannerView)
//                )
//            }
//
//            override fun onAdImpression(bannerView: BannerView) {
//                val ad = bannerView.auctionResult.asAd(demandAd, bannerView)
//                SdkCore.getAdRevenueInterceptor()?.onAdRevenueReceived(ad)
//                coreListener.onAdImpression(ad)
//            }
//
//            override fun onAdClicked(bannerView: BannerView) {
//                coreListener.onAdClicked(
//                    bannerView.auctionResult.asAd(demandAd, bannerView)
//                )
//            }
//
//            override fun onAdExpired(bannerView: BannerView) {}
//        })
//    }
//
//    private fun RewardedAd.setCoreListener(auctionResult: OldAuctionResult) {
//        val coreListener = SdkCore.getListenerForDemand(auctionResult.ad.demandAd)
//        val demandAd = auctionResult.ad.demandAd
//        this.setListener(
//            object : RewardedListener {
//                override fun onAdLoaded(rewardedAd: RewardedAd) {
//                    coreListener.onAdLoaded(
//                        rewardedAd.auctionResult.asAd(demandAd, rewardedAd)
//                    )
//                }
//
//                override fun onAdLoadFailed(ad: RewardedAd, bmError: BMError) {
//                    coreListener.onAdShowFailed(bmError.asBidonError(demandId))
//                }
//
//                @Deprecated("Deprecated in Java")
//                override fun onAdShown(rewardedAd: RewardedAd) {
//                    coreListener.onAdShown(
//                        rewardedAd.auctionResult.asAd(demandAd, rewardedAd)
//                    )
//                }
//
//                override fun onAdImpression(rewardedAd: RewardedAd) {
//                    val ad = rewardedAd.auctionResult.asAd(demandAd, rewardedAd)
//                    SdkCore.getAdRevenueInterceptor()?.onAdRevenueReceived(ad)
//                    coreListener.onAdImpression(ad)
//                }
//
//                override fun onAdClicked(rewardedAd: RewardedAd) {
//                    coreListener.onAdClicked(
//                        rewardedAd.auctionResult.asAd(demandAd, rewardedAd)
//                    )
//                }
//
//                override fun onAdExpired(ad: RewardedAd) {
//                }
//
//                override fun onAdShowFailed(rewardedAd: RewardedAd, bmError: BMError) {
//                    coreListener.onAdShowFailed(bmError.asBidonError(demandId))
//                }
//
//                override fun onAdClosed(rewardedAd: RewardedAd, bmError: Boolean) {
//                    coreListener.onAdClosed(
//                        rewardedAd.auctionResult.asAd(demandAd, rewardedAd)
//                    )
//                }
//
//                override fun onAdRewarded(rewardedAd: RewardedAd) {
//                    coreListener.onUserRewarded(
//                        ad = rewardedAd.auctionResult.asAd(demandAd, rewardedAd),
//                        reward = null
//                    )
//                }
//            }
//        )
//    }

//    private fun BannerSize.asBidMachineBannerSize() = when (this) {
//        BannerSize.Banner -> BidMachineBannerSize.Size_320x50
//        BannerSize.LeaderBoard -> BidMachineBannerSize.Size_728x90
//        BannerSize.MRec -> BidMachineBannerSize.Size_300x250
//        else -> BidMachineBannerSize.Size_320x50
//    }
//
//    private fun BMAuctionResult?.asAd(demandAd: DemandAd, sourceAd: Any): Ad {
//        val bmAuctionResult = this
//        return Ad(
//            demandId = demandId,
//            demandAd = demandAd,
//            price = bmAuctionResult?.price ?: 0.0,
//            sourceAd = sourceAd,
//            currencyCode = null,
//            roundId = "Ad.AuctionRound.PostBid",
//            dsp = bmAuctionResult?.demandSource,
//            monetizationNetwork = demandId.demandId
//        )
//    }

}

private const val PlacementKey = "placement"