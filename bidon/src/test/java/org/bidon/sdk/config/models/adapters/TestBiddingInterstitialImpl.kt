package org.bidon.sdk.config.models.adapters

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.adapter.ext.ad
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.BidType

internal class TestBiddingInterstitialImpl(
    override val demandId: DemandId,
    private val testParameters: TestAdapterParameters,
) : AdSource.Interstitial<TestInterstitialParameters>,
    Mode.Bidding,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private lateinit var adParams: TestInterstitialParameters

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

    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE, replay = 1)

    override val isAdReadyToShow: Boolean
        get() = testParameters.fill == Process.Succeed

    override suspend fun getToken(context: Context, adTypeParam: AdTypeParam, adUnits: List<AdUnit>): String {
        return "token123"
    }

    override fun load(adParams: TestInterstitialParameters) {
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
            val lineItem = popAdUnit(demandId, BidType.CPM) ?: error(BidonError.NoAppropriateAdUnitId)
            TestInterstitialParameters(lineItem)
        }
    }
}