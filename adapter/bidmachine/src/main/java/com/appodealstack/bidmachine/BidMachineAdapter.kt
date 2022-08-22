package com.appodealstack.bidmachine

import android.app.Activity
import android.content.Context
import com.appodealstack.bidmachine.ext.adapterVersion
import com.appodealstack.bidmachine.ext.sdkVersion
import com.appodealstack.bidmachine.impl.BMInterstitialAdImpl
import com.appodealstack.bidmachine.impl.BMRewardedAdImpl
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

class BidMachineAdapter :
    Adapter,
    Initializable<BidMachineParameters>,
    AdProvider.Rewarded<BMFullscreenAuctionParams>,
    AdProvider.Interstitial<BMFullscreenAuctionParams> {
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
            BidMachine.setLoggingEnabled(true)
            BidMachine.initialize(context, sourceId) {
                continuation.resume(Unit)
            }
        }

    override fun parseConfigParam(json: JsonObject): BidMachineParameters = json.parse(BidMachineParameters.serializer())

    override fun interstitial(demandAd: DemandAd, roundId: String): AdSource.Interstitial<BMFullscreenAuctionParams> {
        return BMInterstitialAdImpl(demandId, demandAd, roundId)
    }

    override fun rewarded(demandAd: DemandAd, roundId: String): AdSource.Rewarded<BMFullscreenAuctionParams> {
        return BMRewardedAdImpl(demandId, demandAd, roundId)
    }

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
