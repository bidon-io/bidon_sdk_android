package com.appodealstack.bidon.adapters

import android.app.Activity
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.data.models.LineItem
import com.appodealstack.bidon.auctions.domain.impl.PlacementId
import com.appodealstack.bidon.core.ext.asFailure
import com.appodealstack.bidon.core.ext.asSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow

internal data class TestAdapterInterstitialParameters(
    val bid: Process = Process.Succeed,
    val fill: Process = Process.Succeed,
    val auctionParam: Process = Process.Succeed,
) : AdAuctionParams

internal enum class Process {
    Succeed,
    Failed,
    Timeout
}

internal class TestAdapterInterstitialImpl(
    override val demandId: DemandId,
    private val roundId: String,
    private val interstitialParameters: TestAdapterInterstitialParameters,
) : AdSource.Interstitial<TestAdapterInterstitialParameters> {

    private lateinit var adParams: TestAdapterInterstitialParameters
    private val demandAd = DemandAd(adType = AdType.Interstitial, placement = PlacementId)

    override val ad: Ad
        get() = Ad(
            demandId = demandId,
            demandAd = demandAd,
            price = 1.5,
            roundId = roundId,
            monetizationNetwork = "monetizationNetwork-Appodeal",
            dsp = "DSP-bidmachine",
            sourceAd = this,
            currencyCode = "USD",
        )

    override val adState = MutableSharedFlow<AdState>(extraBufferCapacity = Int.MAX_VALUE)

    override suspend fun bid(adParams: TestAdapterInterstitialParameters): Result<AuctionResult> {
        this.adParams = adParams
        return when (adParams.bid) {
            Process.Succeed -> {
                AuctionResult(
                    ecpm = 1.3,
                    adSource = this
                ).asSuccess()
            }
            Process.Failed -> {
                BidonError.NoAppropriateAdUnitId.asFailure()
            }
            Process.Timeout -> {
                delay(60_000L)
                error("should not be here")
            }
        }
    }

    override suspend fun fill(): Result<Ad> {
        return when (adParams.bid) {
            Process.Succeed -> ad.asSuccess()
            Process.Failed -> BidonError.NoFill(demandId = demandId).asFailure()
            Process.Timeout -> {
                delay(60_000L)
                error("should not be here")
            }
        }
    }

    override fun show(activity: Activity) {}

    override fun destroy() {}

    override fun getAuctionParams(
        activity: Activity,
        priceFloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        onLineItemConsumed: (LineItem) -> Unit
    ): Result<AdAuctionParams> {
        return when (adParams.bid) {
            Process.Succeed -> interstitialParameters.asSuccess()
            Process.Failed -> BidonError.NoAppropriateAdUnitId.asFailure()
            Process.Timeout -> error("should not be here")
        }
    }
}