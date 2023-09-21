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
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceType.isTablet
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

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
    private var applovinAd: AppLovinAd? = null
    private var param: ApplovinBannerAuctionParams? = null

    private val listener by lazy {
        object : AppLovinAdDisplayListener, AppLovinAdClickListener {
            override fun adDisplayed(ad: AppLovinAd) {
                logInfo(TAG, "adDisplayed: $ad")
                emitEvent(
                    AdEvent.PaidRevenue(
                        ad = ad.asAd(),
                        adValue = param?.lineItem?.pricefloor.asBidonAdValue()
                    )
                )
                // tracked impression/shown by [BannerView]
            }

            override fun adHidden(ad: AppLovinAd) {
                logInfo(TAG, "adHidden: $ad")
                emitEvent(AdEvent.ShowFailed(BidonError.NoFill(demandId)))
            }

            override fun adClicked(ad: AppLovinAd) {
                logInfo(TAG, "adClicked: $ad")
                emitEvent(AdEvent.Clicked(ad.asAd()))
            }
        }
    }

    override val isAdReadyToShow: Boolean
        get() = applovinAd != null

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        adView?.setAdLoadListener(null)
        adView = null
        applovinAd = null
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            ApplovinBannerAuctionParams(
                activity = activity,
                lineItem = popLineItem(demandId) ?: error(BidonError.NoAppropriateAdUnitId),
                bannerFormat = bannerFormat
            )
        }
    }

    override fun load(adParams: ApplovinBannerAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        param = adParams
        val adSize = adParams.bannerFormat.asApplovinAdSize() ?: error(
            BidonError.AdFormatIsNotSupported(
                demandId.demandId,
                adParams.bannerFormat
            )
        )
        val requestListener = object : AppLovinAdLoadListener {
            override fun adReceived(ad: AppLovinAd) {
                logInfo(TAG, "adReceived: $this")
                applovinAd = ad
                emitEvent(AdEvent.Fill(ad.asAd()))
            }

            override fun failedToReceiveAd(errorCode: Int) {
                logInfo(TAG, "failedToReceiveAd: errorCode=$errorCode. $this")
                emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
            }
        }
        adParams.activity.runOnUiThread {
            val bannerView =
                AppLovinAdView(applovinSdk, adSize, adParams.lineItem.adUnitId, adParams.activity.applicationContext).also {
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
        return AdViewHolder(
            networkAdview = adView,
            widthDp = adView.size.width.takeIf { it > 0 } ?: when (param?.bannerFormat) {
                BannerFormat.Banner -> 320
                BannerFormat.LeaderBoard -> 728
                BannerFormat.MRec -> 300
                BannerFormat.Adaptive -> if (isTablet) 728 else 320
                null -> error("unexpected")
            },
            heightDp = adView.size.height
        )
    }

    private fun AppLovinAd?.asAd(): Ad {
        return Ad(
            demandAd = demandAd,
            ecpm = param?.lineItem?.pricefloor ?: 0.0,
            demandAdObject = this ?: demandAd,
            networkName = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = AdValue.USD,
            auctionId = auctionId,
            adUnitId = param?.lineItem?.adUnitId,
            bidType = bidType,
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
