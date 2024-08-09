package org.bidon.ironsource.impl

import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.demandOnly.ISDemandOnlyBannerLayout
import com.ironsource.mediationsdk.demandOnly.ISDemandOnlyBannerListener
import com.ironsource.mediationsdk.logger.IronSourceError
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import java.util.Collections

internal class IronSourceBannerImpl :
    AdSource.Banner<IronSourceBannerAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var instanceId: String? = null
    private var bannerLayout: ISDemandOnlyBannerLayout? = null

    override val isAdReadyToShow: Boolean
        get() = bannerLayout != null

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            IronSourceBannerAuctionParams(
                activity = activity,
                bannerFormat = bannerFormat,
                adUnit = adUnit,
            )
        }
    }

    override fun load(adParams: IronSourceBannerAuctionParams) {
        val instanceId = adParams.instanceId.also { this.instanceId = it }
            ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "instanceId")))

        if (loadedAdViewInstanceIds.contains(instanceId)) {
            return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "instanceId already loaded")))
        }

        val bannerLayout =
            IronSource.createBannerForDemandOnly(adParams.activity, adParams.bannerSize)
                .also { bannerLayout = it }
        bannerLayout.bannerDemandOnlyListener = object : ISDemandOnlyBannerListener {
            override fun onBannerAdLoaded(instanceId: String?) {
                logInfo(TAG, "onBannerAdLoaded: $instanceId, $this")
                val ad = getAd() ?: return
                emitEvent(AdEvent.Fill(ad))
            }

            override fun onBannerAdLoadFailed(instanceId: String?, error: IronSourceError?) {
                logInfo(TAG, "onBannerAdLoadFailed: $instanceId, $this")
                emitEvent(AdEvent.LoadFailed(error.asBidonError()))
            }

            override fun onBannerAdShown(instanceId: String?) {
                logInfo(TAG, "onBannerAdShown: $instanceId, $this")
                loadedAdViewInstanceIds.add(instanceId ?: return)
            }

            override fun onBannerAdClicked(instanceId: String?) {
                logInfo(TAG, "onBannerAdClicked: $instanceId, $this")
                val ad = getAd() ?: return
                emitEvent(AdEvent.Clicked(ad))
            }

            override fun onBannerAdLeftApplication(instanceId: String?) {
                logInfo(TAG, "onBannerAdLeftApplication: $instanceId, $this")
            }
        }
        IronSource.loadISDemandOnlyBanner(adParams.activity, bannerLayout, instanceId)
    }

    override fun getAdView(): AdViewHolder? {
        return bannerLayout?.let { adView ->
            AdViewHolder(
                networkAdview = adView,
                widthDp = adView.size.width,
                heightDp = adView.size.height
            )
        }
    }

    override fun destroy() {
        instanceId?.let {
            IronSource.destroyISDemandOnlyBanner(instanceId)
            loadedAdViewInstanceIds.remove(instanceId)
        }
        instanceId = null
        bannerLayout = null
    }
}

// IronSourceError doesn't support multi-load with same instanceId for Banners/MRECs
private val loadedAdViewInstanceIds: MutableList<String> by lazy {
    Collections.synchronizedList(mutableListOf<String>())
}

private const val TAG = "IronSourceBannerImpl"