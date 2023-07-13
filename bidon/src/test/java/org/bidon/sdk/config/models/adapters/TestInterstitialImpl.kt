package org.bidon.sdk.config.models.adapters

import android.app.Activity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.bidon.sdk.adapter.*
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.minByPricefloorOrNull
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus

internal class TestInterstitialImpl(
    override val demandId: DemandId,
    private val auctionId: String,
    private val roundId: String,
    private val testParameters: TestAdapterParameters,
    private val demandAd: DemandAd = DemandAd(AdType.Interstitial),
) : AdSource.Interstitial<TestInterstitialParameters>,
    StatisticsCollector by StatisticsCollectorImpl(auctionId, roundId, demandId, demandAd) {

    private lateinit var adParams: TestInterstitialParameters

    override val ad: Ad
        get() = Ad(
            demandAd = demandAd,
            ecpm = adParams.lineItem.pricefloor,
            roundId = roundId,
            networkName = "monetizationNetwork-Other",
            dsp = "DSP-bidmachine",
            demandAdObject = this,
            currencyCode = "USD",
            auctionId = auctionId,
            adUnitId = adParams.adUnitId
        )

    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE, replay = 1)

    override val isAdReadyToShow: Boolean
        get() = testParameters.fill == Process.Succeed

    override fun bid(adParams: TestInterstitialParameters) {
        this.adParams = adParams
        when (testParameters.bid) {
            Process.Succeed -> {
                adEvent.tryEmit(
                    AdEvent.Bid(
                        AuctionResult(
                            ecpm = adParams.lineItem.pricefloor,
                            adSource = this,
                            roundStatus = RoundStatus.Successful
                        )
                    )
                )
            }
            Process.Failed -> {
                adEvent.tryEmit(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
            }
            Process.Timeout -> {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(60_000L)
                    error("should not be here")
                }
            }
        }
    }

    override fun fill() {
        when (testParameters.fill) {
            Process.Succeed -> adEvent.tryEmit(AdEvent.Fill(ad))
            Process.Failed -> adEvent.tryEmit(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
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

    override fun getAuctionParams(
        activity: Activity,
        pricefloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        onLineItemConsumed: (LineItem) -> Unit
    ): Result<AdAuctionParams> = runCatching {
        val lineItem = lineItems
            .minByPricefloorOrNull(demandId, pricefloor)
            ?.also(onLineItemConsumed) ?: error(BidonError.NoAppropriateAdUnitId)
        TestInterstitialParameters(lineItem)
    }
}