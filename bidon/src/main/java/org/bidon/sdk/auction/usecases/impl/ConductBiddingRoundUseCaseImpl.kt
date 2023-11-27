package org.bidon.sdk.auction.usecases.impl

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.ResultsCollector
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.auction.models.BidResponse
import org.bidon.sdk.auction.models.BiddingResponse
import org.bidon.sdk.auction.models.RoundRequest
import org.bidon.sdk.auction.usecases.BidRequestUseCase
import org.bidon.sdk.auction.usecases.ConductBiddingRoundUseCase
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.utils.SdkDispatchers

@Suppress("UNCHECKED_CAST")
internal class ConductBiddingRoundUseCaseImpl(
    private val bidRequestUseCase: BidRequestUseCase
) : ConductBiddingRoundUseCase {

    override suspend fun invoke(
        context: Context,
        biddingSources: List<Mode.Bidding>,
        participantIds: List<String>,
        adTypeParam: AdTypeParam,
        demandAd: DemandAd,
        bidfloor: Double,
        auctionId: String,
        round: RoundRequest,
        auctionConfigurationUid: String?,
        adUnits: List<AdUnit>,
        resultsCollector: ResultsCollector,
    ) {
        runCatching {
            withTimeoutOrNull(round.timeoutMs) {
                val participants = biddingSources.filter {
                    (it as AdSource<*>).demandId.demandId in participantIds
                }
                logInfo(TAG, "participants: $participants")

                /**
                 * Load bids
                 */
                /**
                 * Load bids
                 */
                val tokens = participants.getTokens(
                    context = context,
                    adTypeParam = adTypeParam,
                    adUnits = adUnits
                )
                logInfo(TAG, "${tokens.size} token(s):")
                tokens.forEachIndexed { index, (demandId, token) ->
                    logInfo(TAG, "#$index ${demandId.demandId} {$token}")
                }
                resultsCollector.serverBiddingStarted()
                if (tokens.isEmpty()) {
                    logError(TAG, "No tokens found", BidonError.NoBid)
                    resultsCollector.serverBiddingFinished(null)
                    return@withTimeoutOrNull
                }
                bidRequestUseCase.invoke(
                    adTypeParam = adTypeParam,
                    tokens = tokens,
                    extras = demandAd.getExtras(),
                    bidfloor = bidfloor,
                    auctionId = auctionId,
                    roundId = round.id,
                    auctionConfigurationUid = auctionConfigurationUid
                ).mapCatching { bidResponse ->
                    val bids = bidResponse.bids?.takeIf {
                        it.isNotEmpty() && bidResponse.status == BiddingResponse.BidStatus.Success
                    }
                    requireNotNull(bids) {
                        "No bids found: $bidResponse"
                    }
                }.onSuccess { bids ->
                    /**
                     * Finish bidding
                     */
                    resultsCollector.serverBiddingFinished(bids)
                    fillBids(
                        resultsCollector = resultsCollector,
                        bids = bids,
                        biddingSources = participants,
                        adTypeParam = adTypeParam,
                        round = round,
                        roundPricefloor = bidfloor
                    )
                }.onFailure {
                    resultsCollector.serverBiddingFinished(null)
                    logError(TAG, "Error while server bidding", it)
                }
            } ?: run {
                resultsCollector.biddingTimeoutReached()
            }
        }.onFailure {
            logError(TAG, "Error while server bidding", it)
        }
    }

    private suspend fun fillBids(
        resultsCollector: ResultsCollector,
        bids: List<BidResponse>,
        biddingSources: List<Mode.Bidding>,
        adTypeParam: AdTypeParam,
        round: RoundRequest,
        roundPricefloor: Double
    ) {
        var filled = false
        bids.forEach { bid ->
            val adSource = biddingSources.first {
                (it as AdSource<*>).demandId.demandId == bid.adUnit.demandId
            } as AdSource<*>
            if (!filled) {
                adSource.markFillStarted(
                    adUnit = bid.adUnit,
                    pricefloor = bid.price
                )
                val fillResult = loadAd(
                    biddingSources = biddingSources,
                    bid = bid,
                    adTypeParam = adTypeParam,
                    round = round,
                    roundPricefloor = roundPricefloor
                ).also {
                    logInfo(TAG, "fillResult: ${it.roundStatus}, ${(it as? AuctionResult.Bidding)?.adSource}")
                    if (it.roundStatus == RoundStatus.Successful) {
                        logInfo(TAG, "fillResult: ${it.roundStatus}")
                        filled = true
                    }
                }
                adSource.markFillFinished(
                    roundStatus = fillResult.roundStatus,
                    ecpm = bid.price
                )
                resultsCollector.add(fillResult)
            } else {
                val lose = AuctionResult.BiddingLose(
                    adapterName = adSource.demandId.demandId,
                    ecpm = bid.price
                )
                logInfo(TAG, "$lose")
                resultsCollector.add(lose)
            }
        }
    }

    private suspend fun loadAd(
        biddingSources: List<Mode.Bidding>,
        bid: BidResponse,
        adTypeParam: AdTypeParam,
        round: RoundRequest,
        roundPricefloor: Double
    ): AuctionResult.Bidding {
        val adSource = biddingSources.first {
            (it as AdSource<*>).demandId.demandId == bid.adUnit.demandId
        }
        val adParam = (adSource as AdSource<AdAuctionParams>).getAuctionParam(
            AdAuctionParamSource(
                activity = adTypeParam.activity,
                pricefloor = roundPricefloor,
                timeout = round.timeoutMs,
                optBannerFormat = (adTypeParam as? AdTypeParam.Banner)?.bannerFormat,
                optContainerWidth = (adTypeParam as? AdTypeParam.Banner)?.containerWidth,
                bidResponse = bid
            )
        ).getOrNull() ?: return AuctionResult.Bidding(
            roundStatus = RoundStatus.NoAppropriateAdUnitId,
            adSource = adSource,
        )
        adSource.addImpressionId(bid.impressionId)

        /**
         * Start loading ad
         */
        val bidAdEvent = adSource.adEvent
            .onSubscription {
                runCatching {
                    adSource.markFillStarted(adParam.adUnit, adParam.price)
                    adSource.load(adParam)
                }.onFailure {
                    logError(TAG, "Loading failed($adParam): $it", it)
                    adSource.emitEvent(
                        event = AdEvent.LoadFailed(
                            cause = BidonError.NoFill(adSource.demandId)
                        )
                    )
                }
            }.first {
                // Wait for ad-request result
                it is AdEvent.Fill || it is AdEvent.LoadFailed || it is AdEvent.Expired
            }
        return when (bidAdEvent) {
            is AdEvent.LoadFailed,
            is AdEvent.Expired -> {
                AuctionResult.Bidding(
                    roundStatus = RoundStatus.NoFill,
                    adSource = adSource,
                )
            }

            is AdEvent.Fill -> {
                AuctionResult.Bidding(
                    adSource = adSource,
                    roundStatus = RoundStatus.Successful
                )
            }

            else -> {
                error("unexpected")
            }
        }
    }

    private suspend fun List<Mode.Bidding>.getTokens(
        context: Context,
        adTypeParam: AdTypeParam,
        adUnits: List<AdUnit>
    ): List<Pair<DemandId, String>> = withContext(SdkDispatchers.Default) {
        this@getTokens.mapNotNull { adSource ->
            runCatching {
                require(adSource is AdSource<*>)
                adUnits
                    .filter { it.bidType == BidType.RTB }
                    .filter { it.demandId == adSource.demandId.demandId }
                    .takeIf {
                        /**
                         * Bidding AdUnit should exist
                         */
                        val adUnitFound = it.isNotEmpty()
                        if (!adUnitFound) {
                            logError(TAG, "No bidding AdUnit found for ${adSource.demandId}", BidonError.NoAppropriateAdUnitId)
                        }
                        adUnitFound
                    }?.let {
                        adSource.getToken(context, adTypeParam, it)?.let { token ->
                            adSource.demandId to token
                        }
                    }
            }.getOrNull()
        }
    }
}

private const val TAG = "ConductBiddingRoundUseCase"
