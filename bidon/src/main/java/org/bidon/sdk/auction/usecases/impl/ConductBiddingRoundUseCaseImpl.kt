package org.bidon.sdk.auction.usecases.impl

import android.content.Context
import kotlinx.coroutines.flow.first
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
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.auction.models.BidResponse
import org.bidon.sdk.auction.models.BiddingResponse
import org.bidon.sdk.auction.models.RoundRequest
import org.bidon.sdk.auction.usecases.BidRequestUseCase
import org.bidon.sdk.auction.usecases.ConductBiddingRoundUseCase
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.models.RoundStatus

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
        auctionConfigurationId: Int?,
        auctionConfigurationUid: String?,
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
                val tokens = participants.getTokens(context, adTypeParam)
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
                val bidResponse = bidRequestUseCase.invoke(
                    adTypeParam = adTypeParam,
                    tokens = tokens,
                    extras = demandAd.getExtras(),
                    bidfloor = bidfloor,
                    auctionId = auctionId,
                    roundId = round.id,
                    auctionConfigurationId = auctionConfigurationId,
                    auctionConfigurationUid = auctionConfigurationUid
                ).onFailure {
                    logError(TAG, "Error while server bidding", it)
                }.getOrNull()
                val bids = bidResponse?.bids?.takeIf {
                    it.isNotEmpty() && bidResponse.status == BiddingResponse.BidStatus.Success
                }
                resultsCollector.serverBiddingFinished(bids)

                /**
                 * Finish bidding
                 */
                bids?.let {
                    fillBids(
                        resultsCollector = resultsCollector,
                        bids = it,
                        biddingSources = participants,
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
        bids: List<BidResponse>,
        biddingSources: List<Mode.Bidding>,
        adTypeParam: AdTypeParam,
        round: RoundRequest
    ) {
        var filled = false
        bids.forEach { bid ->
            val adSource = biddingSources.first {
                (it as AdSource<*>).demandId.demandId == bid.demandId
            } as AdSource<*>
            if (!filled) {
                adSource.markFillStarted(
                    lineItem = null,
                    pricefloor = bid.price
                )
                val fillResult = loadAd(
                    biddingSources = biddingSources,
                    bid = bid,
                    adTypeParam = adTypeParam,
                    round = round,
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
                val lose = AuctionResult.Bidding(
                    roundStatus = RoundStatus.Lose,
                    adSource = adSource,
                )
                resultsCollector.add(lose)
            }
        }
    }

    private suspend fun loadAd(
        biddingSources: List<Mode.Bidding>,
        bid: BidResponse,
        adTypeParam: AdTypeParam,
        round: RoundRequest,
    ): AuctionResult.Bidding {
        val adSource = biddingSources.first {
            (it as AdSource<*>).demandId.demandId == bid.demandId
        }
        val adParam = (adSource as AdSource<AdAuctionParams>).getAuctionParam(
            AdAuctionParamSource(
                activity = adTypeParam.activity,
                pricefloor = bid.price,
                timeout = round.timeoutMs,
                optBannerFormat = (adTypeParam as? AdTypeParam.Banner)?.bannerFormat,
                optContainerWidth = (adTypeParam as? AdTypeParam.Banner)?.containerWidth,
                json = bid.json
            )
        ).getOrNull() ?: return AuctionResult.Bidding(
            roundStatus = RoundStatus.NoAppropriateAdUnitId,
            adSource = adSource,
        )

        /**
         * Start loading ad
         */
        // Load AdRequest
        adSource.load(adParam)
        logInfo(TAG, "adSource.load($adParam)")
        // Wait for ad-request result
        val bidAdEvent = adSource.adEvent.first {
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
        adTypeParam: AdTypeParam
    ): List<Pair<DemandId, String>> = this.mapNotNull { adSource ->
        adSource.getToken(context, adTypeParam)?.let { token ->
            (adSource as AdSource<*>).demandId to token
        }
    }
}

private const val TAG = "ConductBiddingRoundUseCase"
