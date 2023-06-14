package org.bidon.sdk.stats.impl

import kotlinx.coroutines.withContext
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.databinders.DataBinderType
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.models.Bidding
import org.bidon.sdk.stats.models.Demand
import org.bidon.sdk.stats.models.Round
import org.bidon.sdk.stats.models.RoundStat
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.stats.models.StatsRequestBody
import org.bidon.sdk.stats.usecases.StatsRequestUseCase
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.json.JsonParsers
import org.bidon.sdk.utils.networking.BaseResponse
import org.bidon.sdk.utils.networking.JsonHttpRequest
import org.bidon.sdk.utils.networking.requests.CreateRequestBodyUseCase

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class StatsRequestUseCaseImpl(
    private val createRequestBody: CreateRequestBodyUseCase,
) : StatsRequestUseCase {
    private val binders: List<DataBinderType> = listOf(
        DataBinderType.Device,
        DataBinderType.App,
        DataBinderType.Token,
        DataBinderType.Session,
        DataBinderType.User,
        DataBinderType.Segment,
        DataBinderType.Reg,
        DataBinderType.Test,
    )

    override suspend operator fun invoke(
        auctionId: String,
        auctionConfigurationId: Int,
        auctionStartTs: Long,
        auctionFinishTs: Long,
        results: List<RoundStat>,
        demandAd: DemandAd,
    ): Result<BaseResponse> = runCatching {
        return withContext(SdkDispatchers.IO) {
            val body = results.asStatsRequestBody(
                auctionId = auctionId,
                auctionConfigurationId = auctionConfigurationId,
                auctionStartTs = auctionStartTs,
                auctionFinishTs = auctionFinishTs
            )
            val requestBody = createRequestBody(
                binders = binders,
                dataKeyName = "stats",
                data = body,
                extras = BidonSdk.getExtras() + demandAd.getExtras()
            )
            logInfo(Tag, "$requestBody")
            get<JsonHttpRequest>().invoke(
                path = "$StatsRequestPath/${demandAd.adType.code}",
                body = requestBody,
            ).mapCatching { jsonResponse ->
                val baseResponse = JsonParsers.parseOrNull<BaseResponse>(jsonResponse)
                requireNotNull(baseResponse)
            }.onFailure {
                logError(Tag, "Error while sending stats", it)
            }.onSuccess {
                logInfo(Tag, "Stats was sent successfully")
            }
        }
    }

    private fun List<RoundStat>.asStatsRequestBody(
        auctionId: String,
        auctionConfigurationId: Int,
        auctionStartTs: Long,
        auctionFinishTs: Long,
    ): StatsRequestBody {
        val winner = this
            .flatMap { it.demands + it.bidding }
            .filterNotNull()
            .firstOrNull { it.roundStatus == RoundStatus.Win }
            .asSuccessResultOrFail(
                auctionStartTs = auctionStartTs,
                auctionFinishTs = auctionFinishTs
            )
        return StatsRequestBody(
            auctionId = auctionId,
            auctionConfigurationId = auctionConfigurationId,
            rounds = this.map { stat ->
                Round(
                    id = stat.roundId,
                    winnerEcpm = stat.winnerEcpm,
                    winnerDemandId = stat.winnerDemandId?.demandId,
                    pricefloor = stat.pricefloor,
                    demands = stat.demands.map { demandStat ->
                        Demand(
                            demandId = demandStat.demandId.demandId,
                            adUnitId = demandStat.adUnitId,
                            roundStatusCode = demandStat.roundStatus.code,
                            ecpm = demandStat.ecpm,
                            bidStartTs = demandStat.bidStartTs,
                            bidFinishTs = demandStat.bidFinishTs,
                            fillStartTs = demandStat.fillStartTs,
                            fillFinishTs = demandStat.fillFinishTs,
                        )
                    },
                    bidding = stat.bidding?.let {
                        Bidding(
                            demandId = it.demandId?.demandId,
                            bidFinishTs = it.bidFinishTs,
                            fillFinishTs = it.fillFinishTs,
                            bidStartTs = it.bidStartTs,
                            ecpm = it.ecpm,
                            fillStartTs = it.fillStartTs,
                            roundStatusCode = it.roundStatus.code
                        )
                    }
                )
            },
            result = winner
        )
    }
}

private const val StatsRequestPath = "stats"
private const val Tag = "StatsRequestUseCase"
