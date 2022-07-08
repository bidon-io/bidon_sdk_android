package com.appodealstack.bidmachine

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.auctions.AuctionRequest
import com.appodealstack.mads.auctions.AuctionResult
import com.appodealstack.mads.demands.*
import com.appodealstack.mads.demands.banners.BannerSize
import com.appodealstack.mads.demands.banners.BannerSizeKey
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
import kotlin.coroutines.suspendCoroutine

val BidMachineDemandId = DemandId("bidmachine")

typealias BidMachineBannerSize = io.bidmachine.banner.BannerSize

class BidMachineAdapter : Adapter.PostBid,
    AdSource.Interstitial, AdSource.Rewarded, AdSource.Banner,
    PlacementSource by PlacementSourceImpl() {
    private lateinit var context: Context

    override val demandId = BidMachineDemandId

    override suspend fun init(context: Context, configParams: Bundle): Unit = suspendCoroutine { continuation ->
        this.context = context
        val sourceId = configParams.getString(SourceIdKey) ?: "1" // TODO remove 1
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
                                val auctionResult = AuctionResult(
                                    ad = Ad(
                                        demandId = BidMachineDemandId,
                                        demandAd = demandAd,
                                        price = interstitialAd.auctionResult?.price ?: 0.0,
                                        sourceAd = interstitialAd
                                    ),
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
                                continuation.resume(Result.failure(bmError.asBidonError()))
                            }
                        }

                        override fun onAdShown(interstitialAd: InterstitialAd) {
                            error("unexpected state. remove on release a28.")
                        }

                        override fun onAdImpression(interstitialAd: InterstitialAd) {
                            error("unexpected state. remove on release a28.")
                        }

                        override fun onAdClicked(interstitialAd: InterstitialAd) {
                            error("unexpected state. remove on release a28.")
                        }

                        override fun onAdExpired(interstitialAd: InterstitialAd) {
                            if (!isFinished.getAndSet(true)) {
                                // remove listener
                                interstitialAd.setListener(null)
                                continuation.resume(Result.failure(DemandError.Expired))
                            }
                        }

                        override fun onAdShowFailed(interstitialAd: InterstitialAd, p1: BMError) {
                            error("unexpected state. remove on release a28.")
                        }

                        override fun onAdClosed(interstitialAd: InterstitialAd, p1: Boolean) {
                            error("unexpected state. remove on release a28.")
                        }
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
                                    ad = Ad(
                                        demandId = BidMachineDemandId,
                                        demandAd = demandAd,
                                        price = rewardedAd.auctionResult?.price ?: 0.0,
                                        sourceAd = rewardedAd
                                    ),
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
                                continuation.resume(Result.failure(bmError.asBidonError()))
                            }
                        }

                        override fun onAdShown(p0: RewardedAd) {
                            error("unexpected state. remove on release a28.")
                        }

                        override fun onAdImpression(p0: RewardedAd) {
                            error("unexpected state. remove on release a28.")
                        }

                        override fun onAdClicked(p0: RewardedAd) {
                            error("unexpected state. remove on release a28.")
                        }

                        override fun onAdExpired(rewardedAd: RewardedAd) {
                            if (!isFinished.getAndSet(true)) {
                                // remove listener
                                rewardedAd.setListener(null)
                                continuation.resume(Result.failure(DemandError.Expired))
                            }
                        }

                        override fun onAdShowFailed(p0: RewardedAd, p1: BMError) {
                            error("unexpected state. remove on release a28.")
                        }

                        override fun onAdClosed(p0: RewardedAd, p1: Boolean) {
                            error("unexpected state. remove on release a28.")
                        }

                        override fun onAdRewarded(p0: RewardedAd) {
                            error("unexpected state. remove on release a28.")
                        }

                    })
                    .load(request)
            }
        }
    }

    override fun banner(context: Context, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
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
                                ad = Ad(
                                    demandId = BidMachineDemandId,
                                    demandAd = demandAd,
                                    price = view.auctionResult?.price ?: 0.0,
                                    sourceAd = bannerView
                                ),
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
                            continuation.resume(Result.failure(bmError.asBidonError()))
                        }
                    }

                    override fun onAdExpired(view: BannerView) {
                        if (!isFinished.getAndSet(true)) {
                            // remove listener
                            view.setListener(null)
                            continuation.resume(Result.failure(DemandError.Expired))
                        }
                    }

                    override fun onAdShown(p0: BannerView) {
                        error("unexpected state. remove on release a28.")
                    }

                    override fun onAdImpression(p0: BannerView) {
                        error("unexpected state. remove on release a28.")
                    }

                    override fun onAdClicked(p0: BannerView) {
                        error("unexpected state. remove on release a28.")
                    }

                })
                bannerView.load(request)
            }
        }
    }

    private fun InterstitialAd.setCoreListener(auctionResult: AuctionResult) {
        val coreListener = SdkCore.getListenerForDemand(auctionResult.ad.demandAd)
        this.setListener(
            object : InterstitialListener {
                override fun onAdLoaded(ad: InterstitialAd) {
                    coreListener.onAdLoaded(auctionResult.ad)
                }

                override fun onAdLoadFailed(ad: InterstitialAd, bmError: BMError) {
                    coreListener.onAdDisplayFailed(bmError.asBidonError())
                }

                override fun onAdShown(ad: InterstitialAd) {
                    coreListener.onAdDisplayed(auctionResult.ad)
                }

                override fun onAdImpression(ad: InterstitialAd) {
                    coreListener.onAdLoaded(auctionResult.ad)
                }

                override fun onAdClicked(ad: InterstitialAd) {
                    coreListener.onAdClicked(auctionResult.ad)
                }

                override fun onAdExpired(ad: InterstitialAd) {
                }

                override fun onAdShowFailed(ad: InterstitialAd, bmError: BMError) {
                    coreListener.onAdDisplayFailed(bmError.asBidonError())
                }

                override fun onAdClosed(ad: InterstitialAd, bmError: Boolean) {
                    coreListener.onAdHidden(auctionResult.ad)
                }
            }
        )
    }

    private fun BannerView.setCoreListener(auctionResult: AuctionResult) {
        val coreListener = SdkCore.getListenerForDemand(auctionResult.ad.demandAd)
        this.setListener(object : BannerListener {
            override fun onAdLoaded(bannerView: BannerView) {
                coreListener.onAdLoaded(auctionResult.ad)
            }

            override fun onAdLoadFailed(bannerView: BannerView, bmError: BMError) {
                coreListener.onAdLoadFailed(bmError.asBidonError())
            }

            override fun onAdShown(bannerView: BannerView) {
                coreListener.onAdDisplayed(auctionResult.ad)
            }

            override fun onAdImpression(bannerView: BannerView) {}

            override fun onAdClicked(bannerView: BannerView) {
                coreListener.onAdClicked(auctionResult.ad)
            }

            override fun onAdExpired(bannerView: BannerView) {}
        })
    }

    private fun RewardedAd.setCoreListener(auctionResult: AuctionResult) {
        val coreListener = SdkCore.getListenerForDemand(auctionResult.ad.demandAd)
        this.setListener(
            object : RewardedListener {
                override fun onAdLoaded(ad: RewardedAd) {
                    coreListener.onAdLoaded(auctionResult.ad)
                }

                override fun onAdLoadFailed(ad: RewardedAd, bmError: BMError) {
                    coreListener.onAdDisplayFailed(bmError.asBidonError())
                }

                override fun onAdShown(ad: RewardedAd) {
                    coreListener.onAdDisplayed(auctionResult.ad)
                }

                override fun onAdImpression(ad: RewardedAd) {
                    coreListener.onAdLoaded(auctionResult.ad)
                }

                override fun onAdClicked(ad: RewardedAd) {
                    coreListener.onAdClicked(auctionResult.ad)
                }

                override fun onAdExpired(ad: RewardedAd) {
                }

                override fun onAdShowFailed(ad: RewardedAd, bmError: BMError) {
                    coreListener.onAdDisplayFailed(bmError.asBidonError())
                }

                override fun onAdClosed(ad: RewardedAd, bmError: Boolean) {
                    coreListener.onAdHidden(auctionResult.ad)
                }

                override fun onAdRewarded(rewardedAd: RewardedAd) {
                    coreListener.onUserRewarded(auctionResult.ad, null)
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
}

private const val SourceIdKey = "SourceId"
private const val PlacementKey = "placement"