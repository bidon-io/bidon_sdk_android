package org.bidon.unityads.impl

import android.app.Activity
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import kotlinx.coroutines.flow.MutableSharedFlow
import org.bidon.sdk.adapter.*
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceType
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.minByPricefloorOrNull
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.unityads.ext.asBidonError

/**
 * Created by Aleksei Cherniaev on 12/04/2023.
 */
internal class UnityAdsBanner(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val auctionId: String,
) : AdSource.Banner<UnityAdsBannerAuctionParams>,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId,
        demandAd = demandAd
    ) {
    private var bannerAdView: BannerView? = null
    private var param: UnityAdsBannerAuctionParams? = null

    private val bannerListener: BannerView.IListener = object : BannerView.IListener {
        override fun onBannerLoaded(bannerAdView: BannerView?) {
            this@UnityAdsBanner.bannerAdView = bannerAdView
            isAdReadyToShow = true
            bannerAdView?.let {
                adEvent.tryEmit(
                    AdEvent.Bid(
                        AuctionResult(
                            ecpm = requireNotNull(param?.lineItem?.pricefloor),
                            adSource = this@UnityAdsBanner,
                            roundStatus = RoundStatus.Successful
                        )
                    )
                )
            }
        }

        override fun onBannerClick(bannerAdView: BannerView?) {
            logInfo(Tag, "onAdClicked: $this")
            bannerAdView?.let {
                adEvent.tryEmit(AdEvent.Clicked(it.asAd()))
            }
        }

        override fun onBannerFailedToLoad(bannerAdView: BannerView?, errorInfo: BannerErrorInfo?) {
            logError(Tag, "Error while loading ad: $errorInfo. $this", errorInfo.asBidonError())
            isAdReadyToShow = false
            adEvent.tryEmit(
                AdEvent.LoadFailed(errorInfo.asBidonError())
            )
        }

        override fun onBannerLeftApplication(bannerView: BannerView?) {
        }
    }

    override val ad: Ad?
        get() = bannerAdView?.asAd()

    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE, replay = 1)
    override var isAdReadyToShow: Boolean = false

    override fun getAuctionParams(
        activity: Activity,
        pricefloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        bannerFormat: BannerFormat,
        onLineItemConsumed: (LineItem) -> Unit,
        containerWidth: Float,
    ): Result<AdAuctionParams> = runCatching {
        val lineItem = lineItems
            .minByPricefloorOrNull(demandId, pricefloor)
            ?.also(onLineItemConsumed) ?: error(BidonError.NoAppropriateAdUnitId)
        UnityAdsBannerAuctionParams(
            lineItem = lineItem,
            bannerFormat = bannerFormat,
            activity = activity,
        )
    }

    override fun getAdView(): AdViewHolder {
        val bannerAdView = requireNotNull(bannerAdView)
        return AdViewHolder(
            networkAdview = bannerAdView,
            widthDp = bannerAdView.size.width,
            heightDp = bannerAdView.size.height
        )
    }

    override fun bid(adParams: UnityAdsBannerAuctionParams) {
        logInfo(Tag, "Starting with $adParams")
        param = adParams
        val adUnitId = adParams.adUnitId
        if (adUnitId.isNotBlank()) {
            val unityBannerSize = when (adParams.bannerFormat) {
                BannerFormat.LeaderBoard -> UnityBannerSize(728, 90)
                BannerFormat.Banner -> UnityBannerSize(320, 50)
                BannerFormat.Adaptive -> if (DeviceType.isTablet) {
                    UnityBannerSize(728, 90)
                } else {
                    UnityBannerSize(320, 50)
                }

                BannerFormat.MRec -> UnityBannerSize(300, 250)
            }
            val adView = BannerView(adParams.activity, adParams.adUnitId, unityBannerSize)
                .apply { this.listener = bannerListener }
                .also { bannerAdView = it }
            adView.load()
        } else {
            val error = BidonError.NoAppropriateAdUnitId
            logError(
                tag = Tag,
                message = "No appropriate AdUnitId found for price_floor=${adParams.lineItem.pricefloor}",
                error = error
            )
            adEvent.tryEmit(AdEvent.LoadFailed(error))
        }
    }

    override fun fill() {
        logInfo(Tag, "Starting fill: $this")
        /**
         * Admob fills the bid automatically. It's not needed to fill it manually.
         */
        adEvent.tryEmit(AdEvent.Fill(requireNotNull(bannerAdView?.asAd())))
    }

    override fun show(activity: Activity) {}

    override fun destroy() {
        bannerAdView?.listener = null
        bannerAdView?.destroy()
        bannerAdView = null
    }

    private fun BannerView.asAd() = Ad(
        demandAd = demandAd,
        ecpm = param?.lineItem?.pricefloor ?: 0.0,
        demandAdObject = this,
        networkName = demandId.demandId,
        dsp = null,
        roundId = roundId,
        currencyCode = AdValue.USD,
        auctionId = auctionId,
        adUnitId = param?.adUnitId
    )
}

private const val Tag = "UnityAdsBanner"