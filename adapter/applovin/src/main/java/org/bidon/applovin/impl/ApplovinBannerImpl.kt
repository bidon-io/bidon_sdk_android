package org.bidon.applovin.impl

import com.applovin.adview.AppLovinAdView
import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinAdClickListener
import com.applovin.sdk.AppLovinAdDisplayListener
import com.applovin.sdk.AppLovinAdLoadListener
import com.applovin.sdk.AppLovinAdSize
import com.applovin.sdk.AppLovinSdk
import org.bidon.applovin.ApplovinBannerAuctionParams
import org.bidon.applovin.ext.asBidonAdValue
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo.isTablet
import org.bidon.sdk.auction.ext.height
import org.bidon.sdk.auction.ext.width
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.BidType

/**
 * I have no idea how it works. There is no documentation.
 *
 * https://appodeal.slack.com/archives/C02PE4GAFU0/p1661421318406689
 */
internal class ApplovinBannerImpl(
    private val applovinSdk: AppLovinSdk,
) : AdSource.Banner<ApplovinBannerAuctionParams>,
    Mode.Network,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var adView: AppLovinAdView? = null
    private var lineItem: AdUnit? = null
    private var bannerFormat: BannerFormat? = null

    private val listener by lazy {
        object : AppLovinAdDisplayListener, AppLovinAdClickListener {
            override fun adDisplayed(ad: AppLovinAd) {
                logInfo(TAG, "adDisplayed: $ad")
                getAd()?.let {
                    emitEvent(AdEvent.PaidRevenue(it, lineItem?.pricefloor.asBidonAdValue()))
                }
                // tracked impression/shown by [BannerView]
            }

            override fun adHidden(ad: AppLovinAd) {
                logInfo(TAG, "adHidden: $ad")
                emitEvent(AdEvent.ShowFailed(BidonError.NoFill(demandId)))
            }

            override fun adClicked(ad: AppLovinAd) {
                logInfo(TAG, "adClicked: $ad")
                getAd()?.let {
                    emitEvent(AdEvent.Clicked(it))
                }
            }
        }
    }

    override var isAdReadyToShow: Boolean = false

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        adView?.setAdLoadListener(null)
        adView = null
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            ApplovinBannerAuctionParams(
                activity = activity,
                adUnit = popAdUnit(demandId, BidType.CPM) ?: error(BidonError.NoAppropriateAdUnitId),
                bannerFormat = bannerFormat
            )
        }
    }

    override fun load(adParams: ApplovinBannerAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        lineItem = adParams.adUnit
        bannerFormat = adParams.bannerFormat
        val adSize = adParams.bannerFormat.asApplovinAdSize() ?: error(
            BidonError.AdFormatIsNotSupported(
                demandId.demandId,
                adParams.bannerFormat
            )
        )
        val requestListener = object : AppLovinAdLoadListener {
            override fun adReceived(ad: AppLovinAd) {
                logInfo(TAG, "adReceived: $this")
                isAdReadyToShow = true
                getAd()?.let {
                    emitEvent(AdEvent.Fill(it))
                }
            }

            override fun failedToReceiveAd(errorCode: Int) {
                logInfo(TAG, "failedToReceiveAd: errorCode=$errorCode. $this")
                emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
            }
        }
        adParams.activity.runOnUiThread {
            val bannerView =
                AppLovinAdView(applovinSdk, adSize, adParams.zoneId, adParams.activity.applicationContext).also {
                    it.setAdClickListener(listener)
                    it.setAdDisplayListener(listener)
                    adView = it
                }
            bannerView.setAdLoadListener(requestListener)
            bannerView.loadNextAd()
        }
    }

    override fun getAdView(): AdViewHolder? {
        val adView = adView ?: return null
        val bannerFormat = bannerFormat ?: return null
        return AdViewHolder(
            networkAdview = adView,
            widthDp = adView.size.width.takeIf { it > 0 } ?: bannerFormat.width,
            heightDp = adView.size.height.takeIf { it > 0 } ?: bannerFormat.height
        )
    }

    private fun BannerFormat.asApplovinAdSize() = when (this) {
        BannerFormat.Banner -> AppLovinAdSize.BANNER
        BannerFormat.LeaderBoard -> AppLovinAdSize.LEADER
        BannerFormat.Adaptive -> if (isTablet) {
            AppLovinAdSize.LEADER
        } else {
            AppLovinAdSize.BANNER
        }

        BannerFormat.MRec -> AppLovinAdSize.MREC
    }
}

private const val TAG = "ApplovinBanner"
