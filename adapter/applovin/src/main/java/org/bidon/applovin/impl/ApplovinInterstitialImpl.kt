package org.bidon.applovin.impl

import android.app.Activity
import com.applovin.adview.AppLovinInterstitialAd
import com.applovin.sdk.*
import kotlinx.coroutines.flow.MutableSharedFlow
import org.bidon.applovin.ApplovinFullscreenAdAuctionParams
import org.bidon.applovin.ext.asBidonAdValue
import org.bidon.sdk.adapter.*
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.minByPricefloorOrNull
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus

/**
 * I have no idea how it works. There is no documentation.
 *
 * https://appodeal.slack.com/archives/C02PE4GAFU0/p1661421318406689
 */
internal class ApplovinInterstitialImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val applovinSdk: AppLovinSdk,
    private val auctionId: String
) : AdSource.Interstitial<ApplovinFullscreenAdAuctionParams>,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId,
        demandAd = demandAd,
    ) {

    private var applovinAd: AppLovinAd? = null
    private var lineItem: LineItem? = null

    private val requestListener by lazy {
        object : AppLovinAdLoadListener {
            override fun adReceived(ad: AppLovinAd) {
                logInfo(Tag, "adReceived: $this")
                applovinAd = ad
                adEvent.tryEmit(
                    AdEvent.Bid(
                        AuctionResult(
                            ecpm = lineItem?.pricefloor ?: 0.0,
                            adSource = this@ApplovinInterstitialImpl,
                            roundStatus = RoundStatus.Successful
                        )
                    )
                )
            }

            override fun failedToReceiveAd(errorCode: Int) {
                logInfo(Tag, "failedToReceiveAd: errorCode=$errorCode. $this")
                adEvent.tryEmit(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
            }
        }
    }

    private val listener by lazy {
        object :
            AppLovinAdVideoPlaybackListener,
            AppLovinAdDisplayListener,
            AppLovinAdClickListener {
            override fun videoPlaybackBegan(ad: AppLovinAd) {}
            override fun videoPlaybackEnded(ad: AppLovinAd, percentViewed: Double, fullyWatched: Boolean) {}

            override fun adDisplayed(ad: AppLovinAd) {
                logInfo(Tag, "adDisplayed: $this")
                adEvent.tryEmit(AdEvent.Shown(ad.asAd()))
                adEvent.tryEmit(
                    AdEvent.PaidRevenue(
                        ad = ad.asAd(),
                        adValue = lineItem?.pricefloor.asBidonAdValue()
                    )
                )
            }

            override fun adHidden(ad: AppLovinAd) {
                logInfo(Tag, "adHidden: $this")
                adEvent.tryEmit(AdEvent.Closed(ad.asAd()))
            }

            override fun adClicked(ad: AppLovinAd) {
                logInfo(Tag, "adClicked: $this")
                adEvent.tryEmit(AdEvent.Clicked(ad.asAd()))
            }
        }
    }

    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE, replay = 1)
    override val isAdReadyToShow: Boolean
        get() = applovinAd != null

    override val ad: Ad?
        get() = applovinAd?.asAd()

    override fun destroy() {
        logInfo(Tag, "destroy")
        applovinAd = null
    }

    override fun getAuctionParams(
        activity: Activity,
        pricefloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        onLineItemConsumed: (LineItem) -> Unit,
    ): Result<AdAuctionParams> = runCatching {
        val lineItem = lineItems
            .minByPricefloorOrNull(demandId, pricefloor)
            ?.also(onLineItemConsumed)
        ApplovinFullscreenAdAuctionParams(
            lineItem = lineItem ?: error(BidonError.NoAppropriateAdUnitId),
            timeoutMs = timeout,
        )
    }

    override fun bid(adParams: ApplovinFullscreenAdAuctionParams) {
        logInfo(Tag, "Starting with $adParams: $this")
        lineItem = adParams.lineItem
        val adService: AppLovinAdService = applovinSdk.adService
        val zoneId = adParams.lineItem.adUnitId
        if (zoneId.isNullOrEmpty()) {
            adService.loadNextAd(AppLovinAdSize.INTERSTITIAL, requestListener)
        } else {
            adService.loadNextAdForZoneId(zoneId, requestListener)
        }
    }

    override fun fill() {
        runCatching {
            logInfo(Tag, "Starting fill: $this")
            adEvent.tryEmit(AdEvent.Fill(requireNotNull(applovinAd?.asAd())))
        }
    }

    override fun show(activity: Activity) {
        logInfo(Tag, "Starting show: $this")
        val applovinAd = applovinAd
        if (applovinAd != null) {
            val adDialog = AppLovinInterstitialAd.create(applovinSdk, activity).apply {
                setAdDisplayListener(listener)
                setAdClickListener(listener)
            }
            adDialog.showAndRender(applovinAd)
        } else {
            adEvent.tryEmit(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        }
    }

    private fun AppLovinAd?.asAd(): Ad {
        return Ad(
            demandAd = demandAd,
            ecpm = lineItem?.pricefloor ?: 0.0,
            demandAdObject = this ?: demandAd,
            networkName = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = AdValue.USD,
            auctionId = auctionId,
            adUnitId = lineItem?.adUnitId
        )
    }
}

private const val Tag = "ApplovinInterstitial"
