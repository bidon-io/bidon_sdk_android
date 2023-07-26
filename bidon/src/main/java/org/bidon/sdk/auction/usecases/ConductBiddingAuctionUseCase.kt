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
import org.bidon.sdk.auction.models.BidResponse
import org.bidon.sdk.auction.models.BidResponse.BidStatus
import org.bidon.sdk.auction.models.Round
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.stats.models.asRoundStatus
import org.bidon.sdk.utils.ext.SystemTimeNow

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
    ): List<AuctionResult.Bidding>
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
    ): List<AuctionResult.Bidding> {
        var biddingStartTime: Long? = null
        var biddingFinishTime: Long? = null
        val onEach: (List<AuctionResult>) -> Unit = {
            resultsCollector.addAuctionResult(it)
        }
        return runCatching {
            withTimeoutOrNull(round.timeoutMs) {
                val participants = biddingSources.filter {
                    (it as AdSource<*>).demandId.demandId in participantIds
                }
                logInfo(Tag, "participants: $participants")

                /**
                 * Load bids
                 */
                val tokens = participants.getTokens(context)
                logInfo(Tag, "tokens: $tokens")
                biddingStartTime = SystemTimeNow
                markBidStarted(participants)
                val bidResponse = bidRequestUseCase.invoke(
                    adTypeParam = adTypeParam,
                    tokens = tokens,
                    extras = demandAd.getExtras(),
                    bidfloor = bidfloor,
                    auctionId = auctionId,
                    roundId = round.id,
                    auctionConfigurationId = auctionConfigurationId,
                ).getOrNull()
                biddingFinishTime = SystemTimeNow
                markBidFinished(participants, bidResponse)

                /**
                 * Finish bidding
                 */
                when (bidResponse?.status) {
                    BidStatus.Success -> {
                        fillBids(bidResponse.bids, biddingSources, adTypeParam, round)
                    }

                    else -> {
                        listOf(
                            AuctionResult.Bidding.Failure.NoBid(
                                roundStatus = RoundStatus.NoBid,
                                biddingStartTimeTs = biddingStartTime,
                                biddingFinishTimeTs = biddingFinishTime
                            )
                        )
                    }
                }
            }
        }.getOrNull()?.also(onEach) ?: listOf(
            AuctionResult.Bidding.Failure.NoBid(
                roundStatus = RoundStatus.BidTimeoutReached,
                biddingStartTimeTs = biddingStartTime,
                biddingFinishTimeTs = biddingFinishTime
            )
        ).also(onEach)
    }

    private fun markBidFinished(
        participants: List<AdLoadingType.Bidding<AdAuctionParams>>,
        bidResponse: BidResponse?
    ) {
        participants.forEach { adSource ->
            val bid = bidResponse?.bids?.firstOrNull {
                (adSource as AdSource<*>).demandId.demandId == it.demand.id.code
            }
            (adSource as StatisticsCollector).markBidFinished(
                roundStatus = RoundStatus.Successful.takeIf { bid != null } ?: RoundStatus.NoBid,
                ecpm = bid?.price ?: 0.0
            )
        }
    }

    private fun markBidStarted(participants: List<AdLoadingType.Bidding<AdAuctionParams>>) {
        participants.forEach { adSource ->
            (adSource as StatisticsCollector).markBidStarted()
        }
    }

    private suspend fun fillBids(
        bids: List<Bid>?,
        biddingSources: List<AdLoadingType.Bidding<AdAuctionParams>>,
        adTypeParam: AdTypeParam,
        round: Round
    ): List<AuctionResult.Bidding> {
        var filled = false
        return bids?.map { bid ->
            val adSource = biddingSources.first {
                (it as AdSource<*>).demandId.demandId == bid.demand.id.code
            } as AdSource<*>
            if (!filled) {
                adSource.markFillStarted(null, bid.price)
                val auctionResultBidding = loadAd(
                    biddingSources = biddingSources,
                    bid = bid,
                    adTypeParam = adTypeParam,
                    round = round,
                )
                adSource.markBidFinished(
                    roundStatus = auctionResultBidding.roundStatus,
                    ecpm = bid.price
                )
                if (auctionResultBidding is AuctionResult.Bidding.Success) {
                    filled = true
                }
                auctionResultBidding
            } else {
                AuctionResult.Bidding.Failure.Other(
                    roundStatus = RoundStatus.Lose,
                    adSource = biddingSources.first {
                        (it as AdSource<*>).demandId.demandId == bid.demand.id.code
                    } as AdSource<*>,
                )
            }
        } ?: emptyList()
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
            return AuctionResult.Bidding.Failure.Other(
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
                adSource.markBidFinished(
                    roundStatus = RoundStatus.NoFill,
                    ecpm = bid.price
                )
                AuctionResult.Bidding.Failure.Other(
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
            AuctionResult.Bidding.Success(
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
            AuctionResult.Bidding.Failure.Other(
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

private const val Tag = "ConductBiddingAuctionUseCase"