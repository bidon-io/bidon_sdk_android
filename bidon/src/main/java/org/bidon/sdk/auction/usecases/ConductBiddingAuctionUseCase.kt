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
    ): AuctionResult.Bidding
}

@Suppress("UNCHECKED_CAST")
internal class ConductBiddingAuctionUseCaseImpl(
    private val bidRequestUseCase: BidRequestUseCase
) : ConductBiddingAuctionUseCase {

    @Suppress("UNCHECKED_CAST")
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
    ): AuctionResult.Bidding {
        var biddingStartTime: Long? = null
        var biddingFinishTime: Long? = null
        val onEach: (AuctionResult) -> Unit = {
            resultsCollector.addAuctionResult(it)
        }
        return runCatching {
            withTimeoutOrNull(round.timeoutMs) {
                val participants = biddingSources.filter {
                    (it as AdSource<*>).demandId.demandId in participantIds
                }
                logInfo(Tag, "participants: $participants")
                // Bidding started
                val tokens = participants.getTokens(context)
                logInfo(Tag, "tokens: $tokens")
                biddingStartTime = SystemTimeNow
                participants.forEach { adSource ->
                    (adSource as StatisticsCollector).markBidStarted()
                }
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

                // Bidding completed
                val winner = bidResponse?.bid
                logInfo(Tag, "winner: $winner")
                participants.forEach { adSource ->
                    val isSucceed = winner?.demandId == (adSource as AdSource<AdAuctionParams>).demandId.demandId
                    (adSource as StatisticsCollector).markBidFinished(
                        roundStatus = RoundStatus.Successful.takeIf { isSucceed } ?: RoundStatus.NoBid,
                        ecpm = winner?.price
                    )
                }

                // Fill winner
                val results: AuctionResult.Bidding = if (bidResponse?.status == BidStatus.Success && winner != null) {
                    loadWinner(
                        biddingSources = biddingSources,
                        winner = winner,
                        adTypeParam = adTypeParam,
                        round = round,
                    )
                } else {
                    AuctionResult.Bidding.Failure.NoBid(
                        roundStatus = RoundStatus.NoBid,
                        biddingStartTimeTs = biddingStartTime,
                        biddingFinishTimeTs = biddingFinishTime
                    )
                }
                results
            } ?: AuctionResult.Bidding.Failure.NoBid(
                roundStatus = RoundStatus.BidTimeoutReached,
                biddingStartTimeTs = biddingStartTime,
                biddingFinishTimeTs = biddingFinishTime
            )
        }.getOrNull()?.also(onEach) ?: AuctionResult.Bidding.Failure.NoBid(
            roundStatus = RoundStatus.UnspecifiedException,
            biddingStartTimeTs = biddingStartTime,
            biddingFinishTimeTs = biddingFinishTime
        ).also(onEach)
    }

    private suspend fun loadWinner(
        biddingSources: List<AdLoadingType.Bidding<AdAuctionParams>>,
        winner: Bid,
        adTypeParam: AdTypeParam,
        round: Round,
    ): AuctionResult.Bidding {
        val winnerAdSource = biddingSources.first {
            (it as AdSource<*>).demandId.demandId == winner.demandId
        }
        val adParam = (winnerAdSource as AdSource<AdAuctionParams>).obtainAuctionParam(
            AdAuctionParamSource(
                activity = adTypeParam.activity,
                pricefloor = winner.price,
                timeout = round.timeoutMs,
                payload = winner.payload,
                optBannerFormat = (adTypeParam as? AdTypeParam.Banner)?.bannerFormat,
                optContainerWidth = (adTypeParam as? AdTypeParam.Banner)?.containerWidth,
            )
        ).onFailure {
            return AuctionResult.Bidding.Failure.NoFill(
                roundStatus = RoundStatus.NoAppropriateAdUnitId,
                adSource = winnerAdSource
            )
        }.getOrThrow()
        /**
         * Start loading ad
         */
        // Load AdRequest
        winnerAdSource.adRequest(adParam)
        // Wait for ad-request result
        val bidAdEvent = winnerAdSource.adEvent.first {
            it is AdEvent.Bid || it is AdEvent.LoadFailed || it is AdEvent.Expired
        }
        return when (bidAdEvent) {
            is AdEvent.Bid -> {
                winnerAdSource.fillWinner(bidfloor = winner.price)
            }

            is AdEvent.LoadFailed,
            is AdEvent.Expired -> {
                winnerAdSource.markBidFinished(
                    roundStatus = RoundStatus.NoFill,
                    ecpm = winner.price
                )
                AuctionResult.Bidding.Failure.NoFill(
                    roundStatus = RoundStatus.NoFill,
                    adSource = winnerAdSource
                )
            }

            else -> {
                error("unexpected")
            }
        }
    }

    private suspend fun AdLoadingType.Bidding<AdAuctionParams>.fillWinner(
        bidfloor: Double,
    ): AuctionResult.Bidding {
        val winnerAdSource = this
        winnerAdSource as AdSource<AdAuctionParams>

        // Start Fill Ad
        winnerAdSource.markFillStarted(adUnitId = null)
        winnerAdSource.fill()
        // Wait for fill result
        val fillAdEvent = winnerAdSource.adEvent.first {
            it is AdEvent.Fill || it is AdEvent.LoadFailed || it is AdEvent.Expired
        }
        return if (fillAdEvent is AdEvent.Fill) {
            winnerAdSource.markFillFinished(
                roundStatus = RoundStatus.Successful,
                ecpm = bidfloor
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
                ecpm = bidfloor
            )
            AuctionResult.Bidding.Failure.NoFill(
                roundStatus = roundStatus,
                adSource = winnerAdSource
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