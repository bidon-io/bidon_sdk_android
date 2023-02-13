package com.appodealstack.bidon.stats.impl

import com.appodealstack.bidon.ads.AdType
import com.appodealstack.bidon.databinders.DataBinderType
import com.appodealstack.bidon.logs.logging.impl.logError
import com.appodealstack.bidon.logs.logging.impl.logInfo
import com.appodealstack.bidon.stats.RoundStat
import com.appodealstack.bidon.stats.models.Demand
import com.appodealstack.bidon.stats.models.Round
import com.appodealstack.bidon.stats.models.StatsRequestBody
import com.appodealstack.bidon.stats.usecases.StatsRequestUseCase
import com.appodealstack.bidon.utils.di.get
import com.appodealstack.bidon.utils.json.JsonParsers
import com.appodealstack.bidon.utils.networking.BaseResponse
import com.appodealstack.bidon.utils.networking.JsonHttpRequest
import com.appodealstack.bidon.utils.networking.requests.CreateRequestBodyUseCase

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
        val body = results.asStatsRequestBody(auctionId, auctionConfigurationId)
        val requestBody = createRequestBody(
            binders = binders,
            dataKeyName = "stats",
            data = body,
            dataSerializer = JsonParsers.getSerializer(),
        )
        logInfo("", "$requestBody")
        return get<JsonHttpRequest>().invoke(
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
                    pricefloor = stat.priceFloor,
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
