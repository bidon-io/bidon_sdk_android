package org.bidon.sdk.adapters

import android.app.Activity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import org.bidon.sdk.adapter.*
import org.bidon.sdk.ads.*
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auctions.impl.PlacementId
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.utils.ext.asFailure
import org.bidon.sdk.utils.ext.asSuccess

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
            demandAd = demandAd,
            eCPM = 1.5,
            roundId = roundId,
            networkName = "monetizationNetwork-Appodeal",
            dsp = "DSP-bidmachine",
            sourceAd = this,
            currencyCode = "USD",
            auctionId = "auctionId-12312",
            adUnitId = "adUnitId_123"
        )

    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE)

    override val isAdReadyToShow: Boolean
        get() = true

    override suspend fun bid(adParams: TestAdapterInterstitialParameters): AuctionResult {
        this.adParams = adParams
        return when (adParams.bid) {
            Process.Succeed -> {
                AuctionResult(
                    ecpm = 1.3,
                    adSource = this
                )
            }
            Process.Failed -> {
                AuctionResult(
                    ecpm = 0.0,
                    adSource = this
                )
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
        pricefloor: Double,
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