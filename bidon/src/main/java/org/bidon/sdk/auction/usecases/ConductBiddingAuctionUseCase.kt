package org.bidon.sdk.auction.usecases

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdLoadingType
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.ResultsCollector
import org.bidon.sdk.auction.models.Bid
import org.bidon.sdk.auction.models.BidResponse.BidStatus
import org.bidon.sdk.auction.models.Round
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.stats.models.asRoundStatus

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */
internal interface ConductBiddingAuctionUseCase {
    /**
     * @param participantIds Bidding Demand Ids
     */
    suspend fun invoke(
        context: Context,
        biddingSources: List<AdLoadingType.Bidding<AdAuctionParams>>,
        participantIds: List<String>,
        adTypeParam: AdTypeParam,
        demandAd: DemandAd,
        bidfloor: Double,
        auctionId: String,
        round: Round,
        auctionConfigurationId: Int?,
        resultsCollector: ResultsCollector,
    )
}

@Suppress("UNCHECKED_CAST")
internal class ConductBiddingAuctionUseCaseImpl(
    private val bidRequestUseCase: BidRequestUseCase
) : ConductBiddingAuctionUseCase {

    override suspend fun invoke(
        context: Context,
        biddingSources: List<AdLoadingType.Bidding<AdAuctionParams>>,
        participantIds: List<String>,
        adTypeParam: AdTypeParam,
        demandAd: DemandAd,
        bidfloor: Double,
        auctionId: String,
        round: Round,
        auctionConfigurationId: Int?,
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
                val tokens = participants.getTokens(context)
                logInfo(TAG, "tokens: $tokens")
                resultsCollector.serverBiddingStarted()
                val bidResponse = bidRequestUseCase.invoke(
                    adTypeParam = adTypeParam,
                    tokens = tokens,
                    extras = demandAd.getExtras(),
                    bidfloor = bidfloor,
                    auctionId = auctionId,
                    roundId = round.id,
                    auctionConfigurationId = auctionConfigurationId,
                ).onFailure {
                    logError(TAG, "Error while server bidding", it)
                }.getOrNull()
                val bids = bidResponse?.bids?.takeIf { it.isNotEmpty() && bidResponse.status == BidStatus.Success }
                resultsCollector.serverBiddingFinished(bids)

                /**
                 * Finish bidding
                 */
                bids?.let {
                    fillBids(
                        resultsCollector = resultsCollector,
                        bids = it,
                        biddingSources = biddingSources,
                        adTypeParam = adTypeParam,
                        round = round
                    )
                }
                Unit
            } ?: run {
                resultsCollector.biddingTimeoutReached()
            }
        }.onFailure {
            logError(TAG, "Error while server bidding", it)
        }
    }

    private suspend fun fillBids(
        resultsCollector: ResultsCollector,
        bids: List<Bid>,
        biddingSources: List<AdLoadingType.Bidding<AdAuctionParams>>,
        adTypeParam: AdTypeParam,
        round: Round
    ) {
        var filled = false
        bids.forEach { bid ->
            val adSource = biddingSources.first {
                (it as AdSource<*>).demandId.demandId == bid.demand.id.code
            } as AdSource<*>
            if (!filled) {
                adSource.markFillStarted(null, bid.price)
                val fillResult = loadAd(
                    biddingSources = biddingSources,
                    bid = bid,
                    adTypeParam = adTypeParam,
                    round = round,
                ).also {
                    if (it.roundStatus == RoundStatus.Successful) {
                        filled = true
                    }
                }
                resultsCollector.add(fillResult)
            } else {
                val lose = AuctionResult.Bidding(
                    roundStatus = RoundStatus.Lose,
                    adSource = adSource,
                )
                resultsCollector.add(lose)
            }
        }
    }

    private suspend fun loadAd(
        biddingSources: List<AdLoadingType.Bidding<AdAuctionParams>>,
        bid: Bid,
        adTypeParam: AdTypeParam,
        round: Round,
    ): AuctionResult.Bidding {
        val adSource = biddingSources.first {
            (it as AdSource<*>).demandId.demandId == bid.demand.id.code
        }
        val adParam = (adSource as AdSource<AdAuctionParams>).obtainAuctionParam(
            AdAuctionParamSource(
                activity = adTypeParam.activity,
                pricefloor = bid.price,
                timeout = round.timeoutMs,
                payload = bid.demand.payload,
                optBannerFormat = (adTypeParam as? AdTypeParam.Banner)?.bannerFormat,
                optContainerWidth = (adTypeParam as? AdTypeParam.Banner)?.containerWidth,
            )
        ).onFailure {
            return AuctionResult.Bidding(
                roundStatus = RoundStatus.NoAppropriateAdUnitId,
                adSource = adSource,
            )
        }.getOrThrow()
        /**
         * Start loading ad
         */
        // Load AdRequest
        adSource.adRequest(adParam)
        // Wait for ad-request result
        val bidAdEvent = adSource.adEvent.first {
            it is AdEvent.Bid || it is AdEvent.LoadFailed || it is AdEvent.Expired
        }
        return when (bidAdEvent) {
            is AdEvent.Bid -> {
                adSource.fillWinner(bidPrice = bid.price)
            }

            is AdEvent.LoadFailed,
            is AdEvent.Expired -> {
                AuctionResult.Bidding(
                    roundStatus = RoundStatus.NoFill,
                    adSource = adSource,
                )
            }

            else -> {
                error("unexpected")
            }
        }
    }

    private suspend fun AdLoadingType.Bidding<AdAuctionParams>.fillWinner(
        bidPrice: Double,
    ): AuctionResult.Bidding {
        val winnerAdSource = this
        winnerAdSource as AdSource<AdAuctionParams>

        // Start Fill Ad
        winnerAdSource.markFillStarted(adUnitId = null, pricefloor = bidPrice)
        winnerAdSource.fill()
        // Wait for fill result
        val fillAdEvent = winnerAdSource.adEvent.first {
            it is AdEvent.Fill || it is AdEvent.LoadFailed || it is AdEvent.Expired
        }
        return if (fillAdEvent is AdEvent.Fill) {
            winnerAdSource.markFillFinished(
                roundStatus = RoundStatus.Successful,
                ecpm = bidPrice
            )
            AuctionResult.Bidding(
                adSource = winnerAdSource,
                roundStatus = RoundStatus.Successful
            )
        } else {
            val roundStatus = when (fillAdEvent) {
                is AdEvent.Expired -> RoundStatus.NoFill
                is AdEvent.LoadFailed -> fillAdEvent.cause.asRoundStatus()
                else -> error("unexpected")
            }
            winnerAdSource.markFillFinished(
                roundStatus = roundStatus,
                ecpm = bidPrice
            )
            AuctionResult.Bidding(
                roundStatus = roundStatus,
                adSource = winnerAdSource,
            )
        }
    }

    private fun List<AdLoadingType.Bidding<AdAuctionParams>>.getTokens(
        context: Context
    ) = this.mapNotNull { adSource ->
        adSource.getToken(context)?.let { token ->
            (adSource as AdSource<*>).demandId to token
        }
    }
}

private const val TAG = "ConductBiddingAuctionUseCase"