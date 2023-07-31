package org.bidon.sdk.config.models.adapters

import android.app.Activity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdLoadingType
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.ext.ad
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus

internal class TestInterstitialImpl(
    private val testParameters: TestAdapterParameters,
) : AdSource.Interstitial<TestInterstitialParameters>,
    AdLoadingType.Network<TestInterstitialParameters>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private lateinit var adParams: TestInterstitialParameters
//
//    override val ad: Ad
//        get() = Ad(
//            demandAd = demandAd,
//            ecpm = adParams.lineItem.pricefloor,
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

    override fun fill(adParams: TestInterstitialParameters) {
        this.adParams = adParams
        when (testParameters.bid) {
            Process.Succeed -> {
                emitEvent(
                    AdEvent.Bid(
                        AuctionResult.Network(
                            adSource = this,
                            roundStatus = RoundStatus.Successful,
                        )
                    )
                )
            }
            Process.Failed -> {
                emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
            }
            Process.Timeout -> {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(60_000L)
                    error("should not be here")
                }
            }
        }
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

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            val lineItem = popLineItem(demandId) ?: error(BidonError.NoAppropriateAdUnitId)
            TestInterstitialParameters(lineItem)
        }
    }
}