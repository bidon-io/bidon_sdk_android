package org.bidon.sdk.auction.usecases.impl

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.withTimeoutOrNull
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.ext.ad
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.auction.usecases.RequestAdUnitUseCase
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.stats.models.asRoundStatus

class RequestAdUnitUseCaseImpl : RequestAdUnitUseCase {

    override suspend fun invoke(
        adSource: AdSource<AdAuctionParams>,
        adUnit: AdUnit,
        adTypeParam: AdTypeParam,
        priceFloor: Double,
    ): AuctionResult {
        val result = withTimeoutOrNull(adUnit.timeout) {
            adSource.markFillStarted(adUnit, adUnit.pricefloor)
            logInfo(TAG, "FillStarted: \n$adUnit")

            val adParam = adSource.getAuctionParam(
                AdAuctionParamSource(
                    activity = adTypeParam.activity,
                    pricefloor = priceFloor,
                    optBannerFormat = (adTypeParam as? AdTypeParam.Banner)?.bannerFormat,
                    optContainerWidth = (adTypeParam as? AdTypeParam.Banner)?.containerWidth,
                    adUnit = adUnit,
                )
            ).getOrNull()

            val adEvent = adParam?.let {
                adSource.adEvent
                    .onSubscription {
                        runCatching {
                            adSource.markFillStarted(it.adUnit, it.price)
                            adSource.load(it)
                        }.onFailure { ex ->
                            logError(TAG, "Loading failed($it): $ex", ex)
                            adSource.emitEvent(
                                AdEvent.LoadFailed(BidonError.NoFill(adSource.demandId))
                            )
                        }
                    }
                    .first { event -> event is AdEvent.Fill || event is AdEvent.LoadFailed || event is AdEvent.Expired }
            } ?: AdEvent.LoadFailed(BidonError.FillTimedOut(adSource.demandId))

            val requestStatus = when (adEvent) {
                is AdEvent.Fill -> RoundStatus.Successful
                is AdEvent.Expired -> RoundStatus.NoFill
                is AdEvent.LoadFailed -> adEvent.cause.asRoundStatus()
                else -> error("unexpected: $adEvent")
            }

            val auctionResult = getAuctionResult(
                bidType = adUnit.bidType,
                adSource = adSource,
                requestStatus = requestStatus
            )

            logInfo(TAG, "FillFinished: $adUnit. \nResult: ${auctionResult.roundStatus}")

            adSource.markFillFinished(requestStatus, adSource.ad?.ecpm)

            auctionResult
        }
        return if (result == null) {
            logInfo(
                TAG,
                "FillFinished: $adUnit. \nResult: FillTimeoutReached. Timeout: ${adUnit.timeout} "
            )
            getAuctionResult(
                bidType = adUnit.bidType,
                adSource = adSource,
                requestStatus = RoundStatus.FillTimeoutReached
            )
        } else {
            result
        }
    }

    private fun getAuctionResult(
        bidType: BidType,
        adSource: AdSource<AdAuctionParams>,
        requestStatus: RoundStatus
    ): AuctionResult = when (bidType) {
        BidType.RTB -> AuctionResult.Bidding(adSource, requestStatus)
        BidType.CPM -> AuctionResult.Network(adSource, requestStatus)
    }
}

private const val TAG = "RequestAdUnitUseCase"