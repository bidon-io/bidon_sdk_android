package org.bidon.sdk.config.models.adapters

import android.app.Activity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.ext.ad
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

internal class TestInterstitialImpl(
    private val testParameters: TestAdapterParameters,
) : AdSource.Interstitial<TestInterstitialParameters>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private lateinit var adParams: TestInterstitialParameters
//
//    override val ad: Ad
//        get() = Ad(
//            demandAd = demandAd,
//            price = adParams.lineItem.pricefloor,
//            roundId = roundId,
//            networkName = "monetizationNetwork-asd",
//            dsp = "DSP-bidmachine",
//            demandAdObject = this,
//            currencyCode = "USD",
//            auctionId = auctionId,
//            adUnitId = adParams.adUnitId
//        )

    override val isAdReadyToShow: Boolean
        get() = testParameters.fill == Process.Succeed

    override fun load(adParams: TestInterstitialParameters) {
        this.adParams = adParams
        when (testParameters.fill) {
            Process.Succeed -> emitEvent(AdEvent.Fill(ad!!))
            Process.Failed -> emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
            Process.Timeout -> {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(60_000L)
                    error("should not be here")
                }
            }
        }
    }

    override fun show(activity: Activity) {}

    override fun destroy() {}

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            TestInterstitialParameters(auctionParamsScope.adUnit)
        }
    }
}