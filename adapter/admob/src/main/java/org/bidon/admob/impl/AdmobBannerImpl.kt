package org.bidon.admob.impl

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import com.google.android.gms.ads.*
import kotlinx.coroutines.flow.MutableSharedFlow
import org.bidon.admob.AdmobBannerAuctionParams
import org.bidon.admob.asBidonError
import org.bidon.admob.ext.asBidonAdValue
import org.bidon.sdk.adapter.*
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.getHeightDp
import org.bidon.sdk.ads.banner.helper.getWidthDp
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

/**
 * [Test ad units](https://developers.google.com/admob/android/test-ads)
 */
internal class AdmobBannerImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val auctionId: String
) : AdSource.Banner<AdmobBannerAuctionParams>,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId,
        demandAd = demandAd
    ) {

    override val ad: Ad?
        get() = adView?.asAd()

    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE, replay = 1)

    override var isAdReadyToShow: Boolean = false

    private var adSize: AdSize? = null
    private var param: AdmobBannerAuctionParams? = null
    private var adView: AdView? = null
    private val requiredAdView: AdView get() = requireNotNull(adView)

    private val requestListener by lazy {
        object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                logError(Tag, "Error while loading ad: $loadAdError. $this", loadAdError.asBidonError())
                adEvent.tryEmit(
                    AdEvent.LoadFailed(loadAdError.asBidonError())
                )
            }

            override fun onAdLoaded() {
                logInfo(Tag, "onAdLoaded: $this")
                adView?.run {
                    isAdReadyToShow = true
                    adEvent.tryEmit(
                        AdEvent.Bid(
                            AuctionResult(
                                ecpm = requireNotNull(param?.lineItem?.pricefloor),
                                adSource = this@AdmobBannerImpl,
                                roundStatus = RoundStatus.Successful
                            )
                        )
                    )
                }
            }

            override fun onAdClicked() {
                logInfo(Tag, "onAdClicked: $this")
                adEvent.tryEmit(AdEvent.Clicked(requiredAdView.asAd()))
            }

            override fun onAdClosed() {
                logInfo(Tag, "onAdClosed: $this")
                adEvent.tryEmit(AdEvent.Closed(requiredAdView.asAd()))
            }

            override fun onAdImpression() {
                logInfo(Tag, "onAdImpression: $this")
                // tracked impression/shown by [BannerView]
            }

            override fun onAdOpened() {}
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
                        demandAdObject = requiredAdView,
                        networkName = demandId.demandId,
                        dsp = null,
                        roundId = roundId,
                        currencyCode = AdValue.USD,
                        auctionId = auctionId,
                        adUnitId = param?.lineItem?.adUnitId
                    ),
                    adValue = adValue.asBidonAdValue()
                )
            )
        }
    }

    override fun destroy() {
        logInfo(Tag, "destroy $this")
        adView?.onPaidEventListener = null
        adView = null
        param = null
    }

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
            ?.also(onLineItemConsumed)
        AdmobBannerAuctionParams(
            lineItem = lineItem ?: error(BidonError.NoAppropriateAdUnitId),
            bannerFormat = bannerFormat,
            pricefloor = pricefloor,
            context = activity.applicationContext,
            containerWidth = containerWidth
        )
    }

    @SuppressLint("MissingPermission")
    override fun bid(adParams: AdmobBannerAuctionParams) {
        logInfo(Tag, "Starting with $adParams")
        param = adParams
        val adUnitId = param?.lineItem?.adUnitId
        if (!adUnitId.isNullOrBlank()) {
            val adView = AdView(adParams.context)
                .apply {
                    val admobBannerSize = adParams.bannerFormat.asAdmobAdSize(
                        context = adParams.context,
                        containerWidth = adParams.containerWidth
                    )
                    this@AdmobBannerImpl.adSize = admobBannerSize
                    this.setAdSize(admobBannerSize)
                    this.adUnitId = adUnitId
                    this.adListener = requestListener
                    this.onPaidEventListener = paidListener
                }
                .also {
                    adView = it
                }
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
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
        runCatching {
            logInfo(Tag, "Starting fill: $this")
            /**
             * Admob fills the bid automatically. It's not needed to fill it manually.
             */
            adEvent.tryEmit(AdEvent.Fill(ad = requireNotNull(adView?.asAd())))
        }
    }

    override fun show(activity: Activity) {}

    override fun getAdView(): AdViewHolder = AdViewHolder(
        networkAdview = requiredAdView,
        widthDp = adSize?.width ?: param?.bannerFormat.getWidthDp(),
        heightDp = adSize?.height ?: param?.bannerFormat.getHeightDp()
    )

    private fun AdView.asAd(): Ad {
        return Ad(
            demandAd = demandAd,
            ecpm = param?.lineItem?.pricefloor ?: 0.0,
            demandAdObject = this,
            networkName = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = AdValue.USD,
            auctionId = auctionId,
            adUnitId = adUnitId
        )
    }

    private fun BannerFormat.asAdmobAdSize(context: Context, containerWidth: Float) = when (this) {
        BannerFormat.Banner -> AdSize.BANNER
        BannerFormat.LeaderBoard -> AdSize.LEADERBOARD
        BannerFormat.MRec -> AdSize.MEDIUM_RECTANGLE
        BannerFormat.Adaptive -> context.adaptiveAdSize(containerWidth)
    }

    @Suppress("DEPRECATION")
    private fun Context.adaptiveAdSize(width: Float): AdSize {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val density = outMetrics.density
        var adWidthPixels = width
        if (adWidthPixels == 0f) {
            adWidthPixels = outMetrics.widthPixels.toFloat()
        }
        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }
}

private const val Tag = "Admob Banner"
