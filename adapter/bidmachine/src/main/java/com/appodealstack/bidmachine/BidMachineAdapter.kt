package com.appodealstack.bidmachine

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.appodealstack.bidon.SdkCore
import com.appodealstack.bidon.auctions.AuctionRequest
import com.appodealstack.bidon.auctions.AuctionResult
import com.appodealstack.bidon.demands.*
import com.appodealstack.bidon.demands.banners.BannerSize
import com.appodealstack.bidon.demands.banners.BannerSizeKey
import io.bidmachine.BidMachine
import io.bidmachine.PriceFloorParams
import io.bidmachine.banner.BannerListener
import io.bidmachine.banner.BannerRequest
import io.bidmachine.banner.BannerView
import io.bidmachine.interstitial.InterstitialAd
import io.bidmachine.interstitial.InterstitialListener
import io.bidmachine.interstitial.InterstitialRequest
import io.bidmachine.rewarded.RewardedAd
import io.bidmachine.rewarded.RewardedListener
import io.bidmachine.rewarded.RewardedRequest
import io.bidmachine.utils.BMError
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

val BidMachineDemandId = DemandId("bidmachine")

typealias BidMachineBannerSize = io.bidmachine.banner.BannerSize

class BidMachineAdapter : Adapter.PostBid<BidMachineParameters>,
    AdSource.Interstitial, AdSource.Rewarded, AdSource.Banner,
    PlacementSource by PlacementSourceImpl() {
    private lateinit var context: Context

    override val demandId = BidMachineDemandId

    override suspend fun init(activity: Activity, configParams: BidMachineParameters): Unit =
        suspendCancellableCoroutine { continuation ->
            this.context = activity.applicationContext
            val sourceId = configParams.sourceId
            BidMachine.initialize(context, sourceId) {
                continuation.resume(Unit)
            }
        }

    override fun interstitial(activity: Activity?, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
        return AuctionRequest { data ->
            suspendCancellableCoroutine { continuation ->
                val isFinished = AtomicBoolean(false)
                val placement = adParams.getString(PlacementKey)
                val interstitialRequest = InterstitialRequest.Builder().apply {
                    data?.let {
                        setPriceFloorParams(PriceFloorParams().addPriceFloor(it.priceFloor))
                    }
                    placement?.let {
                        setPlacementId(placement)
                    }
                }.build()
                InterstitialAd(context)
                    .setListener(object : InterstitialListener {
                        override fun onAdLoaded(interstitialAd: InterstitialAd) {
                            if (!isFinished.getAndSet(true)) {
                                val bmAuctionResult = interstitialAd.auctionResult
                                val auctionResult = AuctionResult(
                                    ad = bmAuctionResult.asAd(demandAd, interstitialAd),
                                    adProvider = object : AdProvider {
                                        override fun canShow(): Boolean = interstitialAd.canShow()
                                        override fun showAd(activity: Activity?, adParams: Bundle) = interstitialAd.show()
                                        override fun destroy() = interstitialAd.destroy()
                                    }
                                )
                                interstitialAd.setCoreListener(auctionResult)
                                continuation.resume(Result.success(auctionResult))
                            }
                        }

                        override fun onAdLoadFailed(interstitialAd: InterstitialAd, bmError: BMError) {
                            if (!isFinished.getAndSet(true)) {
                                // remove listener
                                interstitialAd.setListener(null)
                                continuation.resume(Result.failure(bmError.asBidonError(demandId)))
                            }
                        }

                        override fun onAdExpired(interstitialAd: InterstitialAd) {
                            if (!isFinished.getAndSet(true)) {
                                // remove listener
                                interstitialAd.setListener(null)
                                continuation.resume(Result.failure(DemandError.Expired(demandId)))
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onAdShown(interstitialAd: InterstitialAd) {
                        }

                        override fun onAdImpression(interstitialAd: InterstitialAd) {}
                        override fun onAdClicked(interstitialAd: InterstitialAd) {}
                        override fun onAdShowFailed(interstitialAd: InterstitialAd, p1: BMError) {}
                        override fun onAdClosed(interstitialAd: InterstitialAd, p1: Boolean) {}
                    }).load(interstitialRequest)
            }
        }
    }

    override fun rewarded(activity: Activity?, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
        return AuctionRequest { data ->
            suspendCancellableCoroutine { continuation ->
                val isFinished = AtomicBoolean(false)
                val placement = adParams.getString(PlacementKey)
                val request = RewardedRequest.Builder().apply {
                    data?.let {
                        setPriceFloorParams(PriceFloorParams().addPriceFloor(it.priceFloor))
                    }
                    placement?.let {
                        setPlacementId(placement)
                    }
                }.build()
                RewardedAd(context)
                    .setListener(object : RewardedListener {
                        override fun onAdLoaded(rewardedAd: RewardedAd) {
                            if (!isFinished.getAndSet(true)) {
                                val auctionResult = AuctionResult(
                                    ad = rewardedAd.auctionResult.asAd(demandAd, rewardedAd),
                                    adProvider = object : AdProvider {
                                        override fun canShow(): Boolean = rewardedAd.canShow()
                                        override fun showAd(activity: Activity?, adParams: Bundle) = rewardedAd.show()
                                        override fun destroy() = rewardedAd.destroy()
                                    }
                                )
                                rewardedAd.setCoreListener(auctionResult)
                                continuation.resume(Result.success(auctionResult))
                            }
                        }

                        override fun onAdLoadFailed(rewardedAd: RewardedAd, bmError: BMError) {
                            if (!isFinished.getAndSet(true)) {
                                // remove listener
                                rewardedAd.setListener(null)
                                continuation.resume(Result.failure(bmError.asBidonError(demandId)))
                            }
                        }

                        override fun onAdExpired(rewardedAd: RewardedAd) {
                            if (!isFinished.getAndSet(true)) {
                                // remove listener
                                rewardedAd.setListener(null)
                                continuation.resume(Result.failure(DemandError.Expired(demandId)))
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onAdShown(p0: RewardedAd) {
                        }

                        override fun onAdImpression(p0: RewardedAd) {}
                        override fun onAdClicked(p0: RewardedAd) {}
                        override fun onAdShowFailed(p0: RewardedAd, p1: BMError) {}
                        override fun onAdClosed(p0: RewardedAd, p1: Boolean) {}
                        override fun onAdRewarded(p0: RewardedAd) {}

                    })
                    .load(request)
            }
        }
    }

    override fun banner(context: Context, demandAd: DemandAd, adParams: Bundle, adContainer: ViewGroup?): AuctionRequest {
        return AuctionRequest { data ->
            suspendCancellableCoroutine { continuation ->
                val isFinished = AtomicBoolean(false)
                val bannerSize = adParams.getInt(BannerSizeKey, BannerSize.Banner.ordinal).let {
                    BannerSize.values()[it]
                }
                val request = BannerRequest.Builder()
                    .setSize(bannerSize.asBidMachineBannerSize())
                    .apply {
                        data?.let {
                            setPriceFloorParams(PriceFloorParams().addPriceFloor(it.priceFloor))
                        }
                        getPlacement(demandAd)?.let {
                            setPlacementId(it)
                        }
                    }.build()
                val bannerView = BannerView(context)
                bannerView.setListener(object : BannerListener {
                    override fun onAdLoaded(view: BannerView) {
                        if (!isFinished.getAndSet(true)) {
                            val auctionResult = AuctionResult(
                                ad = view.auctionResult.asAd(demandAd, bannerView),
                                adProvider = object : AdProvider, AdViewProvider {
                                    override fun canShow(): Boolean = view.canShow()
                                    override fun showAd(activity: Activity?, adParams: Bundle) {}
                                    override fun destroy() = view.destroy()
                                    override fun getAdView(): View = view
                                }
                            )
                            view.setCoreListener(auctionResult)
                            continuation.resume(Result.success(auctionResult))
                        }
                    }

                    override fun onAdLoadFailed(view: BannerView, bmError: BMError) {
                        if (!isFinished.getAndSet(true)) {
                            // remove listener
                            view.setListener(null)
                            continuation.resume(Result.failure(bmError.asBidonError(demandId)))
                        }
                    }

                    override fun onAdExpired(view: BannerView) {
                        if (!isFinished.getAndSet(true)) {
                            // remove listener
                            view.setListener(null)
                            continuation.resume(Result.failure(DemandError.Expired(demandId)))
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onAdShown(p0: BannerView) {
                    }

                    override fun onAdImpression(view: BannerView) {}
                    override fun onAdClicked(p0: BannerView) {}
                })
                bannerView.load(request)
            }
        }
    }

    private fun InterstitialAd.setCoreListener(auctionResult: AuctionResult) {
        val coreListener = SdkCore.getListenerForDemand(auctionResult.ad.demandAd)
        val demandAd = auctionResult.ad.demandAd
        this.setListener(
            object : InterstitialListener {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    coreListener.onAdLoaded(
                        interstitialAd.auctionResult.asAd(demandAd, interstitialAd)
                    )
                }

                override fun onAdLoadFailed(ad: InterstitialAd, bmError: BMError) {
                    coreListener.onAdDisplayFailed(bmError.asBidonError(demandId))
                }

                @Deprecated("Deprecated in Java")
                override fun onAdShown(interstitialAd: InterstitialAd) {
                    coreListener.onAdDisplayed(
                        interstitialAd.auctionResult.asAd(demandAd, interstitialAd)
                    )
                }

                override fun onAdImpression(interstitialAd: InterstitialAd) {
                    val ad = interstitialAd.auctionResult.asAd(demandAd, interstitialAd)
                    SdkCore.getAdRevenueInterceptor()?.onAdRevenueReceived(ad)
                    coreListener.onAdImpression(ad)
                }

                override fun onAdClicked(interstitialAd: InterstitialAd) {
                    coreListener.onAdClicked(
                        interstitialAd.auctionResult.asAd(demandAd, interstitialAd)
                    )
                }

                override fun onAdExpired(ad: InterstitialAd) {
                }

                override fun onAdShowFailed(ad: InterstitialAd, bmError: BMError) {
                    coreListener.onAdDisplayFailed(bmError.asBidonError(demandId))
                }

                override fun onAdClosed(interstitialAd: InterstitialAd, bmError: Boolean) {
                    coreListener.onAdHidden(
                        interstitialAd.auctionResult.asAd(demandAd, interstitialAd)
                    )
                }
            }
        )
    }

    private fun BannerView.setCoreListener(auctionResult: AuctionResult) {
        val coreListener = SdkCore.getListenerForDemand(auctionResult.ad.demandAd)
        val demandAd = auctionResult.ad.demandAd
        this.setListener(object : BannerListener {
            override fun onAdLoaded(bannerView: BannerView) {
                coreListener.onAdLoaded(
                    bannerView.auctionResult.asAd(demandAd, bannerView)
                )
            }

            override fun onAdLoadFailed(bannerView: BannerView, bmError: BMError) {
                coreListener.onAdLoadFailed(bmError.asBidonError(demandId))
            }

            @Deprecated("Deprecated in Java")
            override fun onAdShown(bannerView: BannerView) {
                coreListener.onAdDisplayed(
                    bannerView.auctionResult.asAd(demandAd, bannerView)
                )
            }

            override fun onAdImpression(bannerView: BannerView) {
                val ad = bannerView.auctionResult.asAd(demandAd, bannerView)
                SdkCore.getAdRevenueInterceptor()?.onAdRevenueReceived(ad)
                coreListener.onAdImpression(ad)
            }

            override fun onAdClicked(bannerView: BannerView) {
                coreListener.onAdClicked(
                    bannerView.auctionResult.asAd(demandAd, bannerView)
                )
            }

            override fun onAdExpired(bannerView: BannerView) {}
        })
    }

    private fun RewardedAd.setCoreListener(auctionResult: AuctionResult) {
        val coreListener = SdkCore.getListenerForDemand(auctionResult.ad.demandAd)
        val demandAd = auctionResult.ad.demandAd
        this.setListener(
            object : RewardedListener {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    coreListener.onAdLoaded(
                        rewardedAd.auctionResult.asAd(demandAd, rewardedAd)
                    )
                }

                override fun onAdLoadFailed(ad: RewardedAd, bmError: BMError) {
                    coreListener.onAdDisplayFailed(bmError.asBidonError(demandId))
                }

                @Deprecated("Deprecated in Java")
                override fun onAdShown(rewardedAd: RewardedAd) {
                    coreListener.onAdDisplayed(
                        rewardedAd.auctionResult.asAd(demandAd, rewardedAd)
                    )
                }

                override fun onAdImpression(rewardedAd: RewardedAd) {
                    val ad = rewardedAd.auctionResult.asAd(demandAd, rewardedAd)
                    SdkCore.getAdRevenueInterceptor()?.onAdRevenueReceived(ad)
                    coreListener.onAdImpression(ad)
                }

                override fun onAdClicked(rewardedAd: RewardedAd) {
                    coreListener.onAdClicked(
                        rewardedAd.auctionResult.asAd(demandAd, rewardedAd)
                    )
                }

                override fun onAdExpired(ad: RewardedAd) {
                }

                override fun onAdShowFailed(rewardedAd: RewardedAd, bmError: BMError) {
                    coreListener.onAdDisplayFailed(bmError.asBidonError(demandId))
                }

                override fun onAdClosed(rewardedAd: RewardedAd, bmError: Boolean) {
                    coreListener.onAdHidden(
                        rewardedAd.auctionResult.asAd(demandAd, rewardedAd)
                    )
                }

                override fun onAdRewarded(rewardedAd: RewardedAd) {
                    coreListener.onUserRewarded(
                        ad = rewardedAd.auctionResult.asAd(demandAd, rewardedAd),
                        reward = null
                    )
                }
            }
        )
    }

    private fun BannerSize.asBidMachineBannerSize() = when (this) {
        BannerSize.Banner -> BidMachineBannerSize.Size_320x50
        BannerSize.LeaderBoard -> BidMachineBannerSize.Size_728x90
        BannerSize.MRec -> BidMachineBannerSize.Size_300x250
        else -> BidMachineBannerSize.Size_320x50
    }

    private fun io.bidmachine.models.AuctionResult?.asAd(demandAd: DemandAd, sourceAd: Any): Ad {
        val bmAuctionResult = this
        return Ad(
            demandId = demandId,
            demandAd = demandAd,
            price = bmAuctionResult?.price ?: 0.0,
            sourceAd = sourceAd,
            currencyCode = null,
            auctionRound = Ad.AuctionRound.PostBid,
            dsp = bmAuctionResult?.demandSource,
            monetizationNetwork = demandId.demandId
        )
    }
}

private const val PlacementKey = "placement"