package org.bidon.sdk.stats.impl

import kotlinx.coroutines.withContext
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.databinders.DataBinderType
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.RoundStat
import org.bidon.sdk.stats.models.Demand
import org.bidon.sdk.stats.models.Round
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
        DataBinderType.Geo,
        DataBinderType.Session,
        DataBinderType.User,
        DataBinderType.Segment,
    )

    override suspend operator fun invoke(
        auctionId: String,
        auctionConfigurationId: Int,
        results: List<RoundStat>,
        adType: AdType,
    ): Result<BaseResponse> = runCatching {
        return withContext(SdkDispatchers.IO) {
            val body = results.asStatsRequestBody(auctionId, auctionConfigurationId)
            val requestBody = createRequestBody(
                binders = binders,
                dataKeyName = "stats",
                data = body,
            )
            logInfo(Tag, "$requestBody")
            get<JsonHttpRequest>().invoke(
                path = "$StatsRequestPath/${adType.code}",
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
    ): StatsRequestBody {
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
                    }
                )
            }
        )
    }
}

private const val StatsRequestPath = "stats"
private const val Tag = "StatsRequestUseCase"
