package org.bidon.sdk.auction.usecases.impl

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.ResultsCollector
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.RoundRequest
import org.bidon.sdk.auction.usecases.ConductNetworkRoundUseCase
import org.bidon.sdk.auction.usecases.models.NetworksResult
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.stats.models.asRoundStatus

@Suppress("UNCHECKED_CAST")
internal class ConductNetworkRoundUseCaseImpl : ConductNetworkRoundUseCase {
    override fun invoke(
        context: Context,
        networkSources: List<Mode.Network>,
        participantIds: List<String>,
        adTypeParam: AdTypeParam,
        demandAd: DemandAd,
        lineItems: List<LineItem>,
        round: RoundRequest,
        pricefloor: Double,
        scope: CoroutineScope,
        resultsCollector: ResultsCollector,
    ): NetworksResult {
        val mutableLineItems = lineItems.toMutableList()
        runCatching {
            val participants = networkSources.filter {
                (it as AdSource<*>).demandId.demandId in participantIds
            }
            logInfo(TAG, "participants: $participants")
            val deferredList = participants.map { adSource ->
                scope.async {
                    adSource as AdSource<AdAuctionParams>
                    val availableLineItemsForDemand = mutableLineItems.filter { it.demandId == adSource.demandId.demandId }
                    logInfo(
                        tag = TAG,
                        message = "Round '${round.id}'. Adapter ${adSource.demandId.demandId} starts fill. " +
                            "PriceFloor=$pricefloor. LineItems: $availableLineItemsForDemand."
                    )
                    val adEvent = loadAd(
                        adSource = adSource,
                        adTypeParam = adTypeParam,
                        pricefloor = pricefloor,
                        round = round,
                        availableLineItemsForDemand = availableLineItemsForDemand,
                        onLineItemConsumed = { lineItem ->
                            mutableLineItems.remove(lineItem)
                        }
                    )
                    AuctionResult.Network(
                        adSource = adSource,
                        roundStatus = when (adEvent) {
                            is AdEvent.Fill -> RoundStatus.Successful
                            is AdEvent.Expired -> RoundStatus.NoFill
                            is AdEvent.LoadFailed -> adEvent.cause.asRoundStatus()
                            else -> error("unexpected: $adEvent")
                        }
                    ).also {
                        resultsCollector.add(it)
                    }
                }
            }
            return NetworksResult(
                results = deferredList,
                remainingLineItems = mutableLineItems.toList()
            )
        }.getOrNull() ?: run {
            return NetworksResult(
                results = emptyList(),
                remainingLineItems = lineItems
            )
        }
    }

    private suspend fun loadAd(
        adSource: Mode.Network,
        adTypeParam: AdTypeParam,
        pricefloor: Double,
        round: RoundRequest,
        availableLineItemsForDemand: List<LineItem>,
        onLineItemConsumed: (LineItem) -> Unit
    ): AdEvent {
        adSource as AdSource<AdAuctionParams>
        return withTimeoutOrNull(round.timeoutMs) {
            val adParam = adSource.getAuctionParam(
                AdAuctionParamSource(
                    activity = adTypeParam.activity,
                    timeout = round.timeoutMs,
                    optBannerFormat = (adTypeParam as? AdTypeParam.Banner)?.bannerFormat,
                    optContainerWidth = (adTypeParam as? AdTypeParam.Banner)?.containerWidth,
                    pricefloor = pricefloor,
                    lineItems = availableLineItemsForDemand,
                    onLineItemConsumed = onLineItemConsumed
                )
            ).getOrNull() ?: run {
                return@withTimeoutOrNull AdEvent.LoadFailed(BidonError.NoAppropriateAdUnitId)
            }

            // FILL
            adSource.markFillStarted(adParam.lineItem, adParam.price)
            adSource.load(adParam)
            val fillAdEvent = adSource.adEvent.first {
                // wait for results
                it is AdEvent.Fill || it is AdEvent.LoadFailed || it is AdEvent.Expired
            }
            when (fillAdEvent) {
                is AdEvent.Fill -> {
                    adSource.markFillFinished(
                        roundStatus = RoundStatus.Successful,
                        ecpm = fillAdEvent.ad.ecpm
                    )
                }

                is AdEvent.LoadFailed -> {
                    adSource.markFillFinished(
                        roundStatus = fillAdEvent.cause.asRoundStatus(),
                        ecpm = adParam.price
                    )
                }

                is AdEvent.Expired -> {
                    adSource.markFillFinished(
                        roundStatus = RoundStatus.NoFill,
                        ecpm = fillAdEvent.ad.ecpm
                    )
                }

                else -> error("unexpected")
            }
            fillAdEvent
        } ?: AdEvent.LoadFailed(BidonError.FillTimedOut(adSource.demandId))
    }
}

private const val TAG = "ConductNetworkNetworkUseCase"
